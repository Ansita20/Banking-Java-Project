package bankproject.transaction.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByInitiatorUserIdAndIdempotencyKey(String userId, String idempotencyKey);

    Page<Transfer> findByInitiatorUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Transfer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Transfer> findByStatusOrderByCreatedAtDesc(TransferStatus status, Pageable pageable);

    List<Transfer> findTop100ByStatusInAndUpdatedAtBefore(Collection<TransferStatus> statuses, Instant before);
}
