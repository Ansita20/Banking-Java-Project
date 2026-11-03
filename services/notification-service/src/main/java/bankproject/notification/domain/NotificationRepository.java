package bankproject.notification.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Optional<Notification> findByIdAndUserId(UUID id, String userId);

    long countByUserIdAndReadAtIsNull(String userId);

    boolean existsBySourceEvent(String sourceEvent);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(String userId, Instant now);
}
