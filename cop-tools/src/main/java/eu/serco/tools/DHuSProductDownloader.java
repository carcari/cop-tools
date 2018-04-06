package eu.serco.tools;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import eu.serco.tools.data.DownloadProduct;
import eu.serco.tools.data.dhus.opensearch.Entry;
import eu.serco.tools.data.dhus.opensearch.EntryDate;
import eu.serco.tools.data.dhus.opensearch.Results;
import eu.serco.tools.swift.StorageAccount;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javaswift.joss.instructions.UploadInstructions;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class DHuSProductDownloader {

    final static Logger logger = LogManager.getLogger(DHuSProductDownloader.class);

    @Autowired
    private RestTemplateBuilder templateBuilder;

    @Autowired
    JdbcTemplate jdbcTemplate;


    @Autowired
    private StorageAccount storageAccount;

    @Value("${dhus.product.downloader.username:test}")
    private String username;

    @Value("${dhus.product.downloader.password:test}")
    private String password;

    @Value("${dhus.product.downloader.target.root}")
    private String targetDir;

    @Value("${dhus.product.downloader.baseurl}")
    private String baseUrl;

    @Value("${dhus.product.downloader.startdate}")
    private String startdate;

    @Value("${dhus.product.downloader.maxrows:100}")
    private int maxrows;

    @Value("${storage.container.object.format:dotted}")
    private String containerFormat;

    @Value("${dhus.product.downloader.filter}")
    private String queryFilter;

    @Value("${dhus.product.downloader.concurrent.download}")
    private int concurrentDownload;

    private static final int BUFFER_SIZE = 4096;

    private static int downloadCounter = 0;

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("YYYY-MM-DD HH:mm:ss");//yyyy-mm-dd hh:mm:ss[.fffffffff]


    public void checkPendingProducts() {

        try {
            jdbcTemplate.update(" UPDATE download_products set status=null where status='PROCESSING' and download_startdate is null");
        } catch (Exception e ){
            logger.error("Error clearing pending products " + e.getMessage());
        }
    }

    private synchronized int getDownloadCounter() {
        return this.downloadCounter;
    }

    private synchronized void incrementDownloadCounter() {
        this.downloadCounter++;
    }

    private synchronized void decrementDownloadCounter() {
        this.downloadCounter--;
    }


    public Boolean getProducts() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        Boolean hasProducts = true;
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();

        requestFactory.setHttpClient(httpClient);

        RestTemplate template = templateBuilder.requestFactory(requestFactory).basicAuthorization(username,password).build();
        String sqlSelect = "SELECT startdate\n" +
                "\tFROM download_start_date where source='DHUS' LIMIT 1;";
        //get start date
        String startDate = null;
        try {
            startDate = jdbcTemplate.queryForObject(sqlSelect, String.class);

        } catch (Exception e) {
            logger.error("No start date found, initialize table download_start_date...");
            jdbcTemplate.update("INSERT INTO download_start_date(\n" +
                            "\tstartdate, source)\n" +
                            "\tVALUES (?, ?);", startdate, "DHUS");
        }
        logger.info("Product list start date is:  " + startDate);
        String beginPosition = (startDate != null) ? startDate : startdate;
        int i=0;
        String queryUrl, query;


        while(hasProducts) {
            query = (queryFilter !=null && !queryFilter.isEmpty()) ?
                    "search?q=beginposition:["+beginPosition+" TO NOW]"+queryFilter+"&format=json&start="+i*maxrows+"&rows="+maxrows+"&orderby=beginposition asc" :
                    "search?q=beginposition:["+beginPosition+" TO NOW]&format=json&start="+i*maxrows+"&rows="+maxrows+"&orderby=beginposition asc";
            queryUrl = baseUrl + query;
            ResponseEntity<Results> response = template.exchange(queryUrl, HttpMethod.GET, null, Results.class);
            Results entity = response.getBody();
            logger.debug("query: "+ queryUrl);


            String sqlInsert = "INSERT INTO download_products(\n" +
                    "\tid, name, status, source, mission, begin_position, sensing_date)\n" +
                    "\tVALUES (?, ?, ?, ?, ?, ?, ?::timestamp);";
            String sqlUpate = "UPDATE download_start_date\n" +
                    "\tSET startdate=?\n" +
                    "\tWHERE source='DHUS';";
            String lastBeginPosition = beginPosition;
            String shortBeginPosition = beginPosition.substring(0, 7);
            String tsSensing;
            try {
                if (entity != null && entity.feed != null && entity.feed.entries != null) {
                    for (Entry entry : entity.feed.entries) {


                        for (EntryDate date : entry.dates) {
                            if (date.name.equalsIgnoreCase("beginposition")) {
                                lastBeginPosition = date.content;
                                shortBeginPosition = date.content.substring(0, 7);
                                break;
                            }
                        }
                        tsSensing = lastBeginPosition.substring(0, 10) + " " + lastBeginPosition.substring(11, 19);
                        try {
                            int count = jdbcTemplate.queryForObject("SELECT count(*) from download_products where id=? and name=?",
                                    Integer.class, entry.id, entry.title);
                            if (count == 0)
                                jdbcTemplate.update(sqlInsert, entry.id, entry.title, null,
                                        "DHUS", entry.title.substring(0, 3), shortBeginPosition, tsSensing);
                            else
                                logger.debug("product " + entry.title + " already present in dowloader list");
                        } catch (Exception e) {
                            logger.trace("Error adding product to download_products table: ");
                        }

                    }
                    jdbcTemplate.update(sqlUpate, lastBeginPosition);

                } else {
                    logger.warn("No products found ");
                    hasProducts = false;
                    // restart check for new products at the end of the loop
                    jdbcTemplate.update(sqlUpate, startdate);
                    break;
                }

            } catch (Exception e) {
                logger.error("Error occurred getting products", e.getMessage());
                e.printStackTrace();
            }
            i++;
        }
        return hasProducts;

    }


    /**
     * Downloads a file locally from a given URL
     * @throws IOException
     */
    public void downloadFile(String productUrl)
            throws IOException {
        URL url = new URL(productUrl);
        logger.debug("property url is: " + productUrl);
        logger.debug("property targetDir is: " + targetDir);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        String userpass = username + ":" + password;
        String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
        httpConn.setRequestProperty ("Authorization", basicAuth);

        int responseCode = httpConn.getResponseCode();

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = "";
            String disposition = httpConn.getHeaderField("Content-Disposition");
            String contentType = httpConn.getContentType();
            int contentLength = httpConn.getContentLength();

            if (disposition != null) {
                // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = productUrl.substring(productUrl.lastIndexOf("/") + 1,
                        productUrl.length());
            }

            logger.debug("Content-Type = " + contentType);
            logger.debug("Content-Disposition = " + disposition);
            logger.debug("Content-Length = " + contentLength);
            logger.debug("fileName = " + fileName);

            // opens input stream from the HTTP connection
            InputStream inputStream = httpConn.getInputStream();
            String saveFilePath = targetDir + File.separator + fileName;

            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);

            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            logger.debug("File downloaded");
        } else {
            logger.warn("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
    }

    @Scheduled(fixedRateString = "${downloader.scheduling.rate}", initialDelay = 5000)
    public void downloadScheduler() throws IOException {
        logger.info("Starting downloadScheduler execution at: " + dateTimeFormatter.format(LocalDateTime.now()));
        String sqlSelect = "SELECT id, name, mission, begin_position FROM download_products where status is null and source = 'DHUS'" +
                " order by sensing_date  asc LIMIT 1 FOR UPDATE;";
        List<Map<String, Object>> products = jdbcTemplate.queryForList(sqlSelect);
        logger.info("- downloadScheduler: retrieved #"+products.size()+" products");

        if(products.size() > 0) {
            for (Map<String, Object> row : products) {

                DownloadProduct p = new DownloadProduct();
                p.setId(row.get("id").toString());
                p.setBeginPosition(row.get("begin_position").toString());
                p.setMission(row.get("mission").toString());
                p.setName(row.get("name").toString());
                uploadFileOnStorage(p);
            }
        } else
            uploadFileOnStorage(null);


    }

    /**
     * Downloads a file from a given URL
     * @throws IOException
     */
    @Async
    public void uploadFileOnStorage(DownloadProduct product)
            throws IOException {
        Account account = storageAccount.createAccount();

        Callable<String> task = () -> {
            if (product == null)
                return "STOP";
            incrementDownloadCounter();
            String result;
            String productUrl = baseUrl + "odata/v1/Products('" + product.getId() + "')/$value";
            URL url = new URL(productUrl);
            logger.debug("property url is: " + productUrl);
            String sqlStartUpdate = "UPDATE download_products set download_startdate=now(), status='PROCESSING' WHERE id = ? and name = ?";
            String sqlEndUpdate = "UPDATE download_products set download_enddate=now(), status= ? WHERE id = ? and name = ?";
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            String userpass = username + ":" + password;
            String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
            httpConn.setRequestProperty("Authorization", basicAuth);

            int responseCode = httpConn.getResponseCode();

            // always check HTTP response code first
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String fileName = "";
                String disposition = httpConn.getHeaderField("Content-Disposition");
                //String contentType = httpConn.getContentType();
                int contentLength = httpConn.getContentLength();

                if (disposition != null) {
                    // extracts file name from header field
                    int index = disposition.indexOf("filename=");
                    if (index > 0) {
                        fileName = disposition.substring(index + 10,
                                disposition.length() - 1);
                    }
                } else {
                    // extracts file name from URL
                    fileName = productUrl.substring(productUrl.lastIndexOf("/") + 1,
                            productUrl.length());
                }
                String containerName = (containerFormat.equalsIgnoreCase("dotted")) ? product.getMission() + "-" + product.getBeginPosition() : product.getMission();
                //logger.debug("Content-Type = " + contentType);
                //logger.debug("Content-Disposition = " + disposition);
                logger.debug("Content-Length = " + contentLength);
                logger.debug("fileName = " + fileName);

                // opens input stream from the HTTP connection
                InputStream inputStream = httpConn.getInputStream();
                //Create Object Storage Container if it not exists
                Container container = account.getContainer(containerName);
                if (!container.exists())
                    container.create();
                //Create Object

                String objName = (containerFormat.equalsIgnoreCase("dotted")) ? fileName
                        : product.getBeginPosition().substring(0, 4) + "/" + product.getBeginPosition().substring(5, 7) + "/" + fileName;
                StoredObject obj = container.getObject(objName);
                //upload object on storage
                if (!obj.exists()) {
                    try {
                        logger.info("Starting uploading object " + objName + " on storage");
                        jdbcTemplate.update(sqlStartUpdate, product.getId(), product.getName());
                        obj.uploadObject(inputStream);
                        jdbcTemplate.update(sqlEndUpdate, "COMPLETED", product.getId(), product.getName());
                        result = "Object " + objName + " successfully uploaded on storage";
                        logger.info(result);

                    } catch (Exception e) {
                        result = "Error uploading object" + objName + " on storage";
                        logger.error("Error uploading object" + objName + " on storage", e);
                    }
                } else {
                    jdbcTemplate.update(sqlEndUpdate, "COMPLETED", product.getId(), product.getName());
                    result = "File " + fileName + " already present on storage";
                    logger.info("File " + fileName + " already present on storage");
                }

                inputStream.close();


            } else {
                result = "\"No file to download. Server replied HTTP code: \" + responseCode";
                logger.warn("No file to download. Server replied HTTP code: " + responseCode +
                        "\n Response message is: " + httpConn.getResponseMessage());
            }
            httpConn.disconnect();
            decrementDownloadCounter();
            return result;
        };
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(task);
        logger.debug("download counter is: " + getDownloadCounter());
        if(getDownloadCounter() == concurrentDownload) {
            try {
                String res = future.get();
                logger.info("TASK TERMINATED _ RESULT IS: " + res);

            } catch (InterruptedException e) {

            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            logger.info("Shutting down executor... ");
            executor.shutdownNow();
            logger.info("Executor shut down... ");
        }

    }

}
