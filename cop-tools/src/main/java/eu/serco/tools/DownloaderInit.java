package eu.serco.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@SpringBootApplication
@EnableScheduling
@EnableAsync
public class DownloaderInit implements CommandLineRunner {

    final static Logger logger = LogManager.getLogger(DownloaderInit.class);

    @Autowired
    private DHuSProductDownloader dhusDownloader;

    @Autowired
    private DHuSTest dhusTest;

    @Value("${downloader.list.sleep:1000}")
    private long wait;

    @Value("${downloader.list.nofill:false}")
    private boolean nofill;

    public static void main(String[] args) throws Exception {

        //disabled banner, don't want to see the spring logo
        SpringApplication app = new SpringApplication(DownloaderInit.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);

    }

    // Put your logic here.
    @Override
    public void run(String... args) throws Exception {


        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");


        try {

            logger.info("Product Downloader STARTED at  " + dateTimeFormatter.format(LocalDateTime.now()));
            // Clear pending products from database
            if(!nofill) {
                dhusDownloader.checkPendingProducts();
                Runnable runnable = () -> {

                    Boolean checkStatus = true;
                    while (checkStatus) {
                        // ------- code for task to run
                        try {
                            checkStatus = dhusDownloader.getProducts();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (KeyStoreException e) {
                            e.printStackTrace();
                        } catch (KeyManagementException e) {
                            e.printStackTrace();
                        }
                        // ------- ends here
                        try {
                            Thread.sleep(wait);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                };
                Thread thread = new Thread(runnable);
                thread.start();
            }
            //dhusDownloader.downloadScheduler();


            //TEST ONLY BEGIN
            /*ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

            for(int i = 0; i< 3; i++) {
                TimeUnit.SECONDS.sleep(2);
                Runnable task = () -> {
                    try {
                        dhusDownloader.downloadScheduler();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                };
                executor.schedule(task, 3000 * i, TimeUnit.MILLISECONDS);
            }
            try {
                executor.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            executor.shutdown();*/



            /*ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

            for(int i = 0; i< 3; i++) {
                Runnable task = () -> {
                    try {
                        TimeUnit.SECONDS.sleep(2);
                        dhusTest.getProducts();
                    } catch (InterruptedException e) {
                        System.err.println("task interrupted");
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (KeyStoreException e) {
                        e.printStackTrace();
                    } catch (KeyManagementException e) {
                        e.printStackTrace();
                    }
                };
                executor.scheduleAtFixedRate(task, 1000, 1000, TimeUnit.MILLISECONDS);
            }*/

            //TEST ONLY END



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
