package bankproject.account.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Remembers the reply already given to a saga command. Kafka delivers at least once, so a
 * redelivered command gets the same answer instead of being re-evaluated (a rejected debit
 * must never succeed later just because the balance changed in the meantime).
 */
@Entity
@Table(name = "processed_command")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedCommand {

    @EmbeddedId
    private Key id;

    @Column(name = "reply_payload", nullable = false, columnDefinition = "text")
    private String replyPayload;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedCommand(UUID transferId, String commandType, String replyPayload) {
        this.id = new Key(transferId, commandType);
        this.replyPayload = replyPayload;
        this.processedAt = Instant.now();
    }

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Key implements Serializable {

        @Column(name = "transfer_id")
        private UUID transferId;

        @Column(name = "command_type")
        private String commandType;

        public Key(UUID transferId, String commandType) {
            this.transferId = transferId;
            this.commandType = commandType;
        }
    }
}
