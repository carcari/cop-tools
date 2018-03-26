package eu.serco.tools;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import eu.serco.tools.data.dhus.opensearch.Entry;
import eu.serco.tools.data.dhus.opensearch.EntryDate;
import eu.serco.tools.data.dhus.opensearch.Results;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

@Service
public class DHuSTest {

    final static Logger logger = LogManager.getLogger(DHuSTest.class);

    @Autowired
    private RestTemplateBuilder templateBuilder;

    @Value("${dhus.product.downloader.username:test}")
    private String username;

    @Value("${dhus.product.downloader.password:test}")
    private String password;

    @Value("${dhus.product.downloader.baseurl}")
    private String baseUrl;

    @Value("${dhus.product.downloader.startdate}")
    private String startdate;

    @Value("${dhus.product.downloader.target.root}")
    private String targetDir;

    private static final int BUFFER_SIZE = 4096;

    public Boolean getProducts() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        boolean hasProducts=true;
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
        int i=0;
        int maxrows=2;
        String query, productUrl;

        while(hasProducts) {
            query = baseUrl + "search?q=*&format=json&start="+i*maxrows+"&rows="+maxrows+"&orderby=beginposition asc";
            logger.debug("query: "+ query);
            try {
                ResponseEntity<Results> response = template.exchange(query, HttpMethod.GET, null, Results.class);
                Results entity = response.getBody();





                if (entity != null && entity.feed != null && entity.feed.entries != null) {
                    for (Entry entry : entity.feed.entries) {

                        productUrl = baseUrl + "odata/v1/Products('"+entry.id+"')/$value";
                        logger.info("product to download is  " + productUrl);
                        downloadFile(productUrl);

                    }

                } else {
                    logger.warn("No products found ");
                    hasProducts = false;
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
}
