package bankproject.notification;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import bankproject.common.events.AccountEvents.LedgerPosted;
import bankproject.common.events.EventCodec;
import bankproject.common.events.IdentityEvents.CustomerUpdated;
import bankproject.common.events.Topics;
import bankproject.common.security.AuthenticatedUser;
import bankproject.common.security.JwtService;
import bankproject.notification.domain.NotificationRepository;
import bankproject.notification.domain.RecipientRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {Topics.IDENTITY_EVENTS, Topics.ACCOUNT_EVENTS, Topics.TRANSFER_EVENTS})
class NotificationIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired NotificationRepository notifications;
    @Autowired RecipientRepository recipients;

    @Test
    void ledgerPostingsBecomeNotifications() throws Exception {
        String user = UUID.randomUUID().toString();
        kafka.send(Topics.IDENTITY_EVENTS, user,
                EventCodec.encode(new CustomerUpdated(user, "n@test.dev", "Neha", true, true, true))).join();
        await().atMost(Duration.ofSeconds(20)).until(() -> recipients.existsById(user));

        // No SMTP server in tests: email fails, but the in-app notification is still stored.
        kafka.send(Topics.ACCOUNT_EVENTS, "500100000017", EventCodec.encode(new LedgerPosted("500100000017", user,
                "DEPOSIT", "CREDIT", new BigDecimal("1500.00"), new BigDecimal("1500.00"), "k", "Cash deposit")))
                .join();
        await().atMost(Duration.ofSeconds(20)).until(() -> notifications.countByUserIdAndReadAtIsNull(user) == 1);

        String token = "Bearer " + jwt.issue(new AuthenticatedUser(user, "n@test.dev", "USER"));
        mvc.perform(get("/api/notifications").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Money received"))
                .andExpect(jsonPath("$.content[0].message").value(org.hamcrest.Matchers.containsString("XXXX0017")));
        mvc.perform(post("/api/notifications/read-all").header("Authorization", token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", token))
                .andExpect(jsonPath("$.unread").value(0));
    }
}
