package bankproject.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"bankproject.notification", "bankproject.common.security",
        "bankproject.common.web", "bankproject.common.kafka"})
@EntityScan("bankproject.notification")
@EnableJpaRepositories("bankproject.notification")
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
