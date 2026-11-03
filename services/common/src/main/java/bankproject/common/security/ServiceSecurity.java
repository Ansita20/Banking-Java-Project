package bankproject.common.security;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Shared stateless JWT setup for the servlet-based services. */
@Configuration
@ConditionalOnClass(HttpSecurity.class)
public class ServiceSecurity {

    @Bean
    JwtService jwtService(@Value("${bank.jwt.secret}") String secret,
                          @Value("${bank.jwt.ttl:PT1H}") Duration ttl) {
        return new JwtService(secret, ttl);
    }

    /**
     * Applies the common baseline; each service then declares its own path rules.
     * /internal/** is only reachable inside the cluster (the gateway refuses to route it).
     */
    public static HttpSecurity baseline(HttpSecurity http, JwtService jwtService) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
    }
}
