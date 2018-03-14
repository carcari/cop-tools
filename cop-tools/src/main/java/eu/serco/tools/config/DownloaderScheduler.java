package eu.serco.tools.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

@Configuration
@EnableScheduling
@Service
public class DownloaderScheduler implements SchedulingConfigurer {

    @Value("${downloader.threadpool.size}")
    private int poolsize;

    @Value("${downloader.threadpool.prefix}")
    private String poolprefix;

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        System.out.println("poolsize is: " + poolsize);
        System.out.println("poolprefix is: " + poolprefix);
        threadPoolTaskScheduler.setPoolSize(poolsize);
        //threadPoolTaskScheduler.setPoolSize(10);
        threadPoolTaskScheduler.setThreadNamePrefix(poolprefix);
        //threadPoolTaskScheduler.setThreadNamePrefix("test-pool-");
        threadPoolTaskScheduler.initialize();

        scheduledTaskRegistrar.setTaskScheduler(threadPoolTaskScheduler);
    }
}
