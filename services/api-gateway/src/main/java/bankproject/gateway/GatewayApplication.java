package bankproject.gateway;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import bankproject.common.security.JwtService;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    JwtService jwtService(@Value("${bank.jwt.secret}") String secret,
                          @Value("${bank.jwt.ttl:PT1H}") Duration ttl) {
        return new JwtService(secret, ttl);
    }
}
