package bankproject.account.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Optional<LedgerEntry> findByReferenceAndEntryType(String reference, EntryType entryType);

    Page<LedgerEntry> findByAccountNumberAndCreatedAtBetweenOrderByCreatedAtDesc(String accountNumber, Instant from,
                                                                               Instant to, Pageable pageable);
}
