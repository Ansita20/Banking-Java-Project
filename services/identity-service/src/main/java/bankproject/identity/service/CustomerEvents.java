package bankproject.identity.service;

import org.springframework.stereotype.Component;

import bankproject.common.events.IdentityEvents.CustomerUpdated;
import bankproject.common.events.Topics;
import bankproject.common.outbox.OutboxWriter;
import bankproject.identity.domain.UserAccount;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CustomerEvents {

    private final OutboxWriter outbox;

    void publishSnapshot(UserAccount user) {
        outbox.enqueue(Topics.IDENTITY_EVENTS, user.getId(), new CustomerUpdated(user.getId(), user.getEmail(),
                user.fullName(), user.isEmailVerified(), user.isKycCompleted(), user.isEnabled()));
    }
}
