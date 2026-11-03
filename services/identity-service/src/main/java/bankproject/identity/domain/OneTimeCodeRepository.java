package bankproject.identity.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import bankproject.common.events.IdentityEvents.OtpPurpose;

public interface OneTimeCodeRepository extends JpaRepository<OneTimeCode, UUID> {

    Optional<OneTimeCode> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(String userId, OtpPurpose purpose);
}
