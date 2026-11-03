package bankproject.notification.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    /** topic-partition-offset of the source record; makes redelivered events harmless. */
    @Column(name = "source_event", nullable = false, unique = true)
    private String sourceEvent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    public Notification(String userId, String category, String title, String message, String sourceEvent) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.category = category;
        this.title = title;
        this.message = message;
        this.sourceEvent = sourceEvent;
        this.createdAt = Instant.now();
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }
}
