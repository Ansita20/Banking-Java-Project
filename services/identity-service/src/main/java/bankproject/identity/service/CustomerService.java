package bankproject.identity.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bankproject.common.web.ApiException;
import bankproject.identity.api.UserDtos.KycRequest;
import bankproject.identity.api.UserDtos.UserResponse;
import bankproject.identity.domain.KycProfile;
import bankproject.identity.domain.Role;
import bankproject.identity.domain.UserAccount;
import bankproject.identity.domain.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final int MINIMUM_AGE = 18;

    private final UserRepository users;
    private final CustomerEvents customerEvents;

    @Transactional(readOnly = true)
    public UserResponse get(String userId) {
        return UserResponse.from(find(userId));
    }

    @Transactional
    public UserResponse submitKyc(String userId, KycRequest request) {
        if (Period.between(request.dateOfBirth(), LocalDate.now()).getYears() < MINIMUM_AGE) {
            throw ApiException.badRequest("Customers must be at least " + MINIMUM_AGE + " years old");
        }
        UserAccount user = find(userId);
        KycProfile profile = user.getProfile() == null ? new KycProfile() : user.getProfile();
        profile.setPhone(request.phone());
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(request.gender());
        profile.setAddressLine(request.addressLine().trim());
        profile.setCity(request.city().trim());
        profile.setState(request.state().trim());
        profile.setPostalCode(request.postalCode());
        profile.setPanNumber(request.panNumber());
        profile.setGovernmentId(request.governmentId());
        user.setProfile(profile);
        if (!user.isKycCompleted()) {
            user.setKycCompletedAt(Instant.now());
        }
        customerEvents.publishSnapshot(user);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return users.findAllByOrderByCreatedAtDesc().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse setEnabled(String userId, boolean enabled) {
        UserAccount user = find(userId);
        if (user.getRole() == Role.ADMIN) {
            throw ApiException.badRequest("Administrators cannot be disabled here");
        }
        user.setEnabled(enabled);
        customerEvents.publishSnapshot(user);
        return UserResponse.from(user);
    }

    private UserAccount find(String userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
