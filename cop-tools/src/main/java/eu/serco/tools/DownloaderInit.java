package eu.serco.tools;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*public class DownloaderInit
{
    public static void main( String[] args )
    {
        ApplicationContext context = new ClassPathXmlApplicationContext("spring-context.xml");
        Downloader downloader = (Downloader) context.getBean("downloaderBean");
        downloader.printMessage();


    }
}*/

@SpringBootApplication
@EnableScheduling
public class DownloaderInit implements CommandLineRunner {

    @Autowired
    private DHuSProductDownloader dhusDownloader;

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

        System.out.println("Input arguments length is: " + args.length);
//        if(args.length < 2) {
//            System.out.println("Usage: required input parameters are <downloadUrl> <targetDir>");
//            return;
//        }

        try {
//            System.out.println("First argument is: " + args[0]);
//            System.out.println("Second argument is: " + args[1]);
            //dhusDownloader.downloadFile();

            System.out.println("Starting execution at " + dateTimeFormatter.format(LocalDateTime.now()));
            Runnable runnable = new Runnable() {
                public void run() {
                    while (true) {
                        // ------- code for task to run
                        try {
                            dhusDownloader.getProducts();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (KeyStoreException e) {
                            e.printStackTrace();
                        } catch (KeyManagementException e) {
                            e.printStackTrace();
                        }
                        // ------- ends here
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();


            dhusDownloader.downloadScheduler();
            //dhusDownloader.testStorage();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
