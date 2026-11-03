package bankproject.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and verifies HS256 access tokens. identity-service issues them; the gateway and
 * every service verify them with the same shared secret.
 */
public class JwtService {

    private static final String ISSUER = "corebank-identity";

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(String secret, Duration ttl) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("bank.jwt.secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
    }

    public String issue(AuthenticatedUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.userId())
                .claim("email", user.email())
                .claim("role", user.role())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** @throws JwtException if the token is invalid, tampered with or expired */
    public AuthenticatedUser verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthenticatedUser(claims.getSubject(), claims.get("email", String.class),
                claims.get("role", String.class));
    }

    public Duration ttl() {
        return ttl;
    }
}
