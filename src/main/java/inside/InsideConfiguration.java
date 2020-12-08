package inside;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EntityScan("insidebot")
@EnableJpaRepositories("inside")
@ComponentScan("inside")
@Configuration
@EnableScheduling
@EnableTransactionManagement
public class InsideConfiguration{
    @Autowired
    private Settings settings;

    @Bean
    public MessageSource messageSource(){
        ResourceBundleMessageSource b = new ResourceBundleMessageSource();
        b.setBasename("bundle");
        b.setDefaultEncoding("utf-8");
        return b;
    }

    @Bean("taskScheduler")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(settings.executor.schedulerPoolSize);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setThreadNamePrefix("taskScheduler");
        return scheduler;
    }
}


