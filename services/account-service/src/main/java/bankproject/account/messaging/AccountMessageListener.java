package bankproject.account.messaging;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import bankproject.account.domain.Customer;
import bankproject.account.domain.CustomerRepository;
import bankproject.common.events.AccountCommands.CreditAccount;
import bankproject.common.events.AccountCommands.DebitAccount;
import bankproject.common.events.AccountCommands.RefundAccount;
import bankproject.common.events.BankEvent;
import bankproject.common.events.EventCodec;
import bankproject.common.events.IdentityEvents.CustomerUpdated;
import bankproject.common.events.Topics;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountMessageListener {

    private static final Logger log = LoggerFactory.getLogger(AccountMessageListener.class);

    private final TransferCommandHandler commandHandler;
    private final CustomerRepository customers;

    @KafkaListener(topics = Topics.ACCOUNT_COMMANDS, groupId = "account-service")
    public void onCommand(String payload) {
        BankEvent event = EventCodec.decode(payload);
        if (event instanceof DebitAccount cmd) {
            commandHandler.handle(cmd);
        } else if (event instanceof CreditAccount cmd) {
            commandHandler.handle(cmd);
        } else if (event instanceof RefundAccount cmd) {
            commandHandler.handle(cmd);
        } else {
            log.debug("Ignoring {}", event);
        }
    }

    @KafkaListener(topics = Topics.IDENTITY_EVENTS, groupId = "account-service")
    @Transactional
    public void onIdentityEvent(String payload) {
        if (EventCodec.decode(payload) instanceof CustomerUpdated e) {
            Customer customer = customers.findById(e.userId()).orElseGet(() -> new Customer(e.userId()));
            customer.setEmail(e.email());
            customer.setFullName(e.fullName());
            customer.setEmailVerified(e.emailVerified());
            customer.setKycCompleted(e.kycCompleted());
            customer.setEnabled(e.enabled());
            customer.setUpdatedAt(Instant.now());
            customers.save(customer);
        }
    }
}
