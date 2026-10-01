package za.co.unilinkhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UniLinkHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniLinkHubApplication.class, args);
    }
}
