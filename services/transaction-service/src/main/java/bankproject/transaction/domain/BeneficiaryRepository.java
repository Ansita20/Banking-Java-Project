package bankproject.transaction.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    List<Beneficiary> findByUserIdOrderByNickname(String userId);

    Optional<Beneficiary> findByIdAndUserId(UUID id, String userId);

    boolean existsByUserIdAndAccountNumber(String userId, String accountNumber);
}
