package bankproject.account.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, String> {

    /** SELECT ... FOR UPDATE: serializes every balance change on one account. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> lockByAccountNumber(String accountNumber);

    List<Account> findByUserIdOrderByOpenedAt(String userId);

    List<Account> findByStatusOrderByOpenedAt(AccountStatus status);

    List<Account> findAllByOrderByOpenedAtDesc();

    boolean existsByUserIdAndTypeAndStatusIn(String userId, AccountType type, Collection<AccountStatus> statuses);

    long countByStatus(AccountStatus status);

    @Query("select coalesce(sum(a.balance), 0) from Account a where a.status in ('ACTIVE', 'FROZEN')")
    java.math.BigDecimal totalDeposits();
}
