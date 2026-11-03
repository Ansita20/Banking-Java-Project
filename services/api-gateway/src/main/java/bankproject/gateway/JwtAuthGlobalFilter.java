package bankproject.gateway;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import bankproject.common.security.AuthenticatedUser;
import bankproject.common.security.JwtService;
import io.jsonwebtoken.JwtException;
import reactor.core.publisher.Mono;

/**
 * Rejects unauthenticated traffic at the edge so it never reaches a service, and stamps the
 * verified identity on the request for logging. Services still verify the token themselves.
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/**",
            "/actuator/health/**");

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final JwtService jwtService;

    public JwtAuthGlobalFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        String path = request.getPath().value();
        if (request.getMethod() == HttpMethod.OPTIONS || PUBLIC_PATHS.stream().anyMatch(p -> matcher.match(p, path))) {
            return chain.filter(exchange);
        }

        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return reject(exchange, "Missing bearer token");
        }
        AuthenticatedUser user;
        try {
            user = jwtService.verify(header.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            return reject(exchange, "Invalid or expired token");
        }
        if (path.startsWith("/api/admin/") && !user.isAdmin()) {
            return reject(exchange, HttpStatus.FORBIDDEN, "Admin only");
        }

        var mutated = request.mutate()
                .headers(h -> {
                    h.set("X-User-Id", user.userId());
                    h.set("X-User-Role", user.role());
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        return reject(exchange, HttpStatus.UNAUTHORIZED, message);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"code\":\"" + status.name() + "\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
