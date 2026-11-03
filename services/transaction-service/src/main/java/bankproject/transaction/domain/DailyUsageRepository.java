package bankproject.transaction.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface DailyUsageRepository extends JpaRepository<DailyUsage, DailyUsage.Key> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DailyUsage d where d.id = :id")
    Optional<DailyUsage> lock(DailyUsage.Key id);
}
