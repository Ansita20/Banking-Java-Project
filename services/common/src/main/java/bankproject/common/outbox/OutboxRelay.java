package bankproject.common.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Polls the outbox and publishes to Kafka. Delivery is at-least-once: if the process dies
 * after sending but before committing, the row is sent again, so every consumer is idempotent.
 */
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 100;

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${bank.outbox.poll-interval-ms:250}")
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : repository.lockNextBatch(PageRequest.of(0, BATCH_SIZE))) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);
                event.markPublished();
            } catch (Exception e) {
                // Stop here to preserve ordering; rows marked so far still commit.
                log.warn("Outbox publish failed for {} ({}), will retry: {}", event.getId(), event.getEventType(),
                        e.getMessage());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
        }
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    @Transactional
    public void purgePublished() {
        int removed = repository.deletePublishedBefore(Instant.now().minus(Duration.ofDays(7)));
        if (removed > 0) {
            log.info("Purged {} published outbox rows", removed);
        }
    }
}
