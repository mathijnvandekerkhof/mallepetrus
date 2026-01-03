package nl.mallepetrus.jiptv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class JiptvApplication {

    public static void main(String[] args) {
        SpringApplication.run(JiptvApplication.class, args);
    }

}