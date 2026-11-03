package bankproject.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import bankproject.common.security.JwtService;
import bankproject.common.security.ServiceSecurity;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        return ServiceSecurity.baseline(http, jwtService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/internal/**", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .build();
    }
}
