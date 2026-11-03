package bankproject.common.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A message waiting to be published. Written in the same database transaction as the
 * business change, so a state change and its event are either both committed or neither.
 */
@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    OutboxEvent(String topic, String messageKey, String eventType, String payload) {
        this.id = UUID.randomUUID();
        this.topic = topic;
        this.messageKey = messageKey;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    void markPublished() {
        this.publishedAt = Instant.now();
    }
}
