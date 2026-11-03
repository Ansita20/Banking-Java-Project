package bankproject.notification.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import bankproject.common.events.AccountEvents.AccountStatusChanged;
import bankproject.common.events.AccountEvents.LedgerPosted;
import bankproject.common.events.BankEvent;
import bankproject.common.events.EventCodec;
import bankproject.common.events.IdentityEvents.CustomerUpdated;
import bankproject.common.events.IdentityEvents.OtpIssued;
import bankproject.common.events.IdentityEvents.OtpPurpose;
import bankproject.common.events.Topics;
import bankproject.common.events.TransferEvents.TransferFailed;
import bankproject.notification.domain.Recipient;
import bankproject.notification.domain.RecipientRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EventListeners {

    private static final String GROUP = "notification-service";

    private final NotificationService notifications;
    private final RecipientRepository recipients;
    private final EmailSender email;

    @KafkaListener(topics = Topics.IDENTITY_EVENTS, groupId = GROUP)
    @Transactional
    public void onIdentity(@Payload String payload) {
        BankEvent event = EventCodec.decode(payload);
        if (event instanceof CustomerUpdated e) {
            Recipient r = recipients.findById(e.userId()).orElseGet(() -> new Recipient(e.userId()));
            r.setEmail(e.email());
            r.setFullName(e.fullName());
            recipients.save(r);
        } else if (event instanceof OtpIssued e) {
            boolean verification = e.purpose() == OtpPurpose.EMAIL_VERIFICATION;
            email.send(e.email(),
                    verification ? "Verify your CoreBank email" : "Your CoreBank password reset code",
                    "Hi " + e.firstName() + ",\n\nYour one-time code is: " + e.otp()
                            + "\n\nIt expires in 10 minutes. Never share this code with anyone, "
                            + "including bank staff.\n\n- CoreBank",
                    true);
        }
    }

    @KafkaListener(topics = Topics.ACCOUNT_EVENTS, groupId = GROUP)
    public void onAccount(@Payload String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset) {
        String source = topic + "-" + partition + "-" + offset;
        BankEvent event = EventCodec.decode(payload);
        if (event instanceof AccountStatusChanged e) {
            String acc = mask(e.accountNumber());
            String message = switch (e.status()) {
                case "PENDING_APPROVAL" -> "Your " + e.accountType().toLowerCase() + " account " + acc
                        + " application was received and is awaiting approval.";
                case "ACTIVE" -> "Your account " + acc + " is active and ready to use.";
                case "REJECTED" -> "Your account application " + acc + " was declined: " + e.reason();
                case "FROZEN" -> "Your account " + acc + " has been frozen: " + e.reason();
                case "CLOSED" -> "Your account " + acc + " has been closed.";
                default -> "Your account " + acc + " status is now " + e.status() + ".";
            };
            notifications.notify(e.userId(), "ACCOUNT", "Account update", message, source);
        } else if (event instanceof LedgerPosted e) {
            boolean credit = "CREDIT".equals(e.direction());
            String message = money(e.amount()) + (credit ? " credited to " : " debited from ") + "account "
                    + mask(e.accountNumber()) + " (" + e.description() + "). Available balance: "
                    + money(e.balanceAfter()) + ".";
            notifications.notify(e.userId(), credit ? "CREDIT" : "DEBIT",
                    credit ? "Money received" : "Money sent", message, source);
        }
    }

    @KafkaListener(topics = Topics.TRANSFER_EVENTS, groupId = GROUP)
    public void onTransfer(@Payload String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                           @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                           @Header(KafkaHeaders.OFFSET) long offset) {
        if (EventCodec.decode(payload) instanceof TransferFailed e) {
            notifications.notify(e.initiatorUserId(), "TRANSFER", "Transfer failed",
                    "Your transfer of " + money(e.amount()) + " to " + mask(e.toAccount()) + " failed: " + e.reason()
                            + ". Any amount debited has been returned.",
                    topic + "-" + partition + "-" + offset);
        }
    }

    private static String mask(String accountNumber) {
        return "XXXX" + accountNumber.substring(Math.max(0, accountNumber.length() - 4));
    }

    private static String money(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(amount);
    }
}
