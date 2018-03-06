package eu.serco.tools;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

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



        System.out.println("Input arguments length is: " + args.length);
//        if(args.length < 2) {
//            System.out.println("Usage: required input parameters are <downloadUrl> <targetDir>");
//            return;
//        }

        try {
//            System.out.println("First argument is: " + args[0]);
//            System.out.println("Second argument is: " + args[1]);
            //dhusDownloader.downloadFile();
            dhusDownloader.getProducts();
            //dhusDownloader.testStorage();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
