package bankproject.gateway;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Target of the circuit breakers: answers fast with 503 while a downstream service is unhealthy. */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public ResponseEntity<Map<String, String>> fallback(@PathVariable String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "message", service + " is temporarily unavailable, please retry shortly"));
    }
}
