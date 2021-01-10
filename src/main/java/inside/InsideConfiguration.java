package inside;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EntityScan("inside")
@EnableJpaRepositories("inside")
@ComponentScan("inside")
@Configuration
@EnableScheduling
@EnableTransactionManagement
public class InsideConfiguration{
    @Bean
    public MessageSource messageSource(){
        ResourceBundleMessageSource bundle = new ResourceBundleMessageSource();
        bundle.setBasename("bundle");
        bundle.setDefaultEncoding("utf-8");
        return bundle;
    }
}


