package nl.mallepetrus.jiptv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {RedisRepositoriesAutoConfiguration.class})
@EnableJpaRepositories(basePackages = "nl.mallepetrus.jiptv.repository")
@EnableScheduling
@EnableAsync
public class JiptvApplication {

    public static void main(String[] args) {
        SpringApplication.run(JiptvApplication.class, args);
    }

}