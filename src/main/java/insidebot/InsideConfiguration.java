package insidebot;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EntityScan("insidebot")
@EnableJpaRepositories("insidebot")
@ComponentScan("insidebot")
@Configuration
@EnableTransactionManagement
public class InsideConfiguration{

}


