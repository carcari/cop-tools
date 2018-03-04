package eu.serco.tools;


import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Downloader {

    private String appname;

    @Value("${downloader.baseurl}")
    private String value;

    final static Logger logger = LogManager.getLogger(Downloader.class);

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public void printMessage() {

        System.out.println("Starting Downloader...");
        System.out.println("Downloader " + appname +" Started.");
        logger.debug("debug info: " + value);
        logger.info("info info: " + value);
        logger.error("error info: " + value);
        logger.warn("warn info: " + value);
    }
}
