package bankproject.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Contact directory replicated from identity-service's CustomerUpdated events. */
@Entity
@Table(name = "recipient")
@Getter
@Setter
@NoArgsConstructor
public class Recipient {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    public Recipient(String userId) {
        this.userId = userId;
    }
}
