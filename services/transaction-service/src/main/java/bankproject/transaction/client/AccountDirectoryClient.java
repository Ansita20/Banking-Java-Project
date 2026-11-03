package bankproject.transaction.client;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Synchronous lookup against account-service, resolved through Eureka and guarded by a
 * circuit breaker. Callers must cope with {@link LookupResult#unavailable()}.
 */
@Component
public class AccountDirectoryClient {

    private static final Logger log = LoggerFactory.getLogger(AccountDirectoryClient.class);

    public record AccountLookup(String accountNumber, String holderName, String type, boolean canReceive) {
    }

    public record LookupResult(Optional<AccountLookup> account, boolean unavailable) {
    }

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public AccountDirectoryClient(RestClient.Builder loadBalancedBuilder, CircuitBreakerFactory<?, ?> factory) {
        this.restClient = loadBalancedBuilder.baseUrl("http://account-service").build();
        this.circuitBreaker = factory.create("account-directory");
    }

    public LookupResult lookup(String accountNumber) {
        return circuitBreaker.run(() -> {
            try {
                AccountLookup found = restClient.get()
                        .uri("/internal/accounts/{no}/lookup", accountNumber)
                        .retrieve()
                        .body(AccountLookup.class);
                return new LookupResult(Optional.ofNullable(found), false);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    return new LookupResult(Optional.empty(), false);
                }
                throw e;
            }
        }, failure -> {
            log.warn("account-service lookup unavailable: {}", failure.toString());
            return new LookupResult(Optional.empty(), true);
        });
    }
}
