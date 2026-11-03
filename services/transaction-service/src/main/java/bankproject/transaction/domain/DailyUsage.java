package bankproject.transaction.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Amount a customer has committed to outgoing transfers on a given day; locked while checking limits. */
@Entity
@Table(name = "daily_usage")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyUsage {

    @EmbeddedId
    private Key id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal used;

    public DailyUsage(String userId, LocalDate day) {
        this.id = new Key(userId, day);
        this.used = BigDecimal.ZERO.setScale(2);
    }

    public void add(BigDecimal amount) {
        used = used.add(amount);
    }

    public void release(BigDecimal amount) {
        used = used.subtract(amount).max(BigDecimal.ZERO);
    }

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Key implements Serializable {

        @Column(name = "user_id")
        private String userId;

        @Column(name = "usage_date")
        private LocalDate day;

        public Key(String userId, LocalDate day) {
            this.userId = userId;
            this.day = day;
        }
    }
}
