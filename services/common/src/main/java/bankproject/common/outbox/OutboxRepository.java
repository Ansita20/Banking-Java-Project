package bankproject.common.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * FOR UPDATE SKIP LOCKED: several replicas of a service can run the relay concurrently
     * without publishing the same row twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select o from OutboxEvent o where o.publishedAt is null order by o.createdAt")
    List<OutboxEvent> lockNextBatch(Pageable page);

    @Modifying
    @Query("delete from OutboxEvent o where o.publishedAt < :before")
    int deletePublishedBefore(Instant before);
}
