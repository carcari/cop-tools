package eu.serco.tools;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
/**
 * Hello world!
 *
 */
public class DownloaderInit
{
    public static void main( String[] args )
    {
        ApplicationContext context = new ClassPathXmlApplicationContext("spring-context.xml");
        Downloader downloader = (Downloader) context.getBean("downloaderBean");
        downloader.printMessage();

    }
}
