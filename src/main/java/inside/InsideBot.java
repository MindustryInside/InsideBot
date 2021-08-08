package inside;

import inside.data.cache.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.function.Function;

@EntityScan("inside.data.entity")
@EnableJpaRepositories("inside.data.repository")
@ConfigurationPropertiesScan("inside")
@EnableScheduling
@EnableTransactionManagement
@SpringBootApplication
public class InsideBot{

    public static void main(String[] args){
        SpringApplication.run(InsideBot.class, args);
    }

    @Bean
    public EntityCacheManager cacheManager(){
        return new CaffeineEntityCacheManager(Function.identity());
    }

    @Bean
    public MessageSource messageSource(){
        ResourceBundleMessageSource bundle = new ResourceBundleMessageSource();
        bundle.setBasename("bundle");
        bundle.setDefaultEncoding("utf-8");
        return bundle;
    }
}
