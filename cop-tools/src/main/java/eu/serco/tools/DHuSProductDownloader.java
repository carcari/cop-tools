package eu.serco.tools;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import eu.serco.tools.data.Product;
import eu.serco.tools.data.dhus.opensearch.Entry;
import eu.serco.tools.data.dhus.opensearch.EntryDate;
import eu.serco.tools.data.dhus.opensearch.Results;
import eu.serco.tools.swift.StorageAccount;
import org.apache.http.HttpEntity;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class DHuSProductDownloader {

    final static Logger logger = LogManager.getLogger(DHuSProductDownloader.class);

    @Autowired
    private RestTemplateBuilder templateBuilder;


    @Autowired
    private StorageAccount storageAccount;

    @Value("${dhus.product.downloader.username}")
    private String username;

    @Value("${dhus.product.downloader.password}")
    private String password;

    @Value("${dhus.product.downloader.target.root}")
    private String targetDir;

    @Value("${dhus.product.downloader.baseurl}")
    private String baseUrl;

    @Value("${dhus.product.downloader.startdate}")
    private String startdate;

    @Value("${dhus.product.downloader.maxrows:100}")
    private String maxrows;

    @Value("${storage.container.object.format:dotted}")
    private String containerFormat;

    private static final int BUFFER_SIZE = 4096;


    public void getProducts() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

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
        //RestTemplate template = templateBuilder.basicAuthorization(username,password).build();
        String query = baseUrl + "search?q=beginposition:["+startdate+"T00:00:00.000Z TO NOW]&format=json&start=0&rows="+maxrows+"&orderby=beginposition asc";
        logger.debug("query: "+ query);
        List<Product> productsToDownload = new ArrayList<>();

        ResponseEntity<Results> response = template.exchange(query, HttpMethod.GET,null, Results.class);
        Results entity = response.getBody();
        logger.debug("entity is " + entity);
        logger.debug("entity.feed is " + entity.feed);
        logger.debug("entity.feed.totalresults is " + entity.feed.totalresults);
        logger.debug("entity.feed.entries is " + entity.feed.entries);
        if(entity != null && entity.feed != null && entity.feed.entries != null) {
            for (Entry entry : entity.feed.entries) {
                Product p = new Product();
                p.setIdentifier(entry.title);
                p.setMission(entry.title.substring(0, 3));
                p.setUuid(entry.id);

                for (EntryDate date : entry.dates) {
                    if (date.name.equalsIgnoreCase("beginposition")) {
                        p.setBeginposition(date.content.substring(0, 7));
                        break;
                    }
                }
                p.setDownloadUrl(baseUrl + "odata/v1/Products('" + p.getUuid() + "')/$value");
                productsToDownload.add(p);


            }
            Account account = storageAccount.createAccount();
            for (Product product : productsToDownload) {
                uploadFileOnStorage(product, account);
            }
        } else {
            logger.warn("No products found ");
        }

    }


    /**
     * Downloads a file from a given URL
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

            System.out.println("File downloaded");
        } else {
            System.out.println("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
    }

    /**
     * Downloads a file from a given URL
     * @throws IOException
     */
    public void uploadFileOnStorage(Product product, Account account)
            throws IOException {
        URL url = new URL(product.getDownloadUrl());
        logger.info("property url is: " + product.getDownloadUrl());

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
                fileName = product.getDownloadUrl().substring(product.getDownloadUrl().lastIndexOf("/") + 1,
                        product.getDownloadUrl().length());
            }
            String containerName = (containerFormat.equalsIgnoreCase("dotted")) ? product.getMission() + "-"+product.getBeginposition() : product.getMission();
            logger.debug("Content-Type = " + contentType);
            logger.debug("Content-Disposition = " + disposition);
            logger.debug("Content-Length = " + contentLength);
            logger.debug("fileName = " + fileName);

            // opens input stream from the HTTP connection
            InputStream inputStream = httpConn.getInputStream();
            //Create Object Storage Container if it not exists
            Container container = account.getContainer(containerName);
            if(!container.exists())
                container.create();
            //Create Object

            String objName = (containerFormat.equalsIgnoreCase("dotted")) ? fileName
                    : product.getBeginposition().substring(0,4) + "/" + product.getBeginposition().substring(5,7) + "/" + fileName;
            StoredObject obj = container.getObject(objName);
            //upload object on storage
            if(!obj.exists())
                obj.uploadObject(inputStream);

            inputStream.close();

            logger.info("File "+ fileName +" uploaded on storage");
        } else {
            logger.info("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
    }
}
