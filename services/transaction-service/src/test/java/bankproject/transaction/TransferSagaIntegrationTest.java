package bankproject.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import bankproject.common.events.AccountCommands.CreditAccount;
import bankproject.common.events.AccountCommands.DebitAccount;
import bankproject.common.events.AccountCommands.RefundAccount;
import bankproject.common.events.AccountReplies.AccountCredited;
import bankproject.common.events.AccountReplies.AccountDebited;
import bankproject.common.events.AccountReplies.AccountRefunded;
import bankproject.common.events.AccountReplies.CreditRejected;
import bankproject.common.events.AccountReplies.DebitRejected;
import bankproject.common.events.BankEvent;
import bankproject.common.events.EventCodec;
import bankproject.common.events.Topics;
import bankproject.common.events.TransferEvents.TransferCompleted;
import bankproject.common.events.TransferEvents.TransferFailed;
import bankproject.common.security.AuthenticatedUser;
import bankproject.common.security.JwtService;
import bankproject.transaction.domain.TransferRepository;
import bankproject.transaction.domain.TransferStatus;

/**
 * Plays account-service's role over real (embedded) Kafka and checks the orchestrator's state
 * machine, idempotency and limits. account-service's HTTP API is absent, so this also covers
 * the circuit-breaker fallback on the destination lookup.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {Topics.IDENTITY_EVENTS, Topics.ACCOUNT_COMMANDS, Topics.ACCOUNT_REPLIES,
        Topics.ACCOUNT_EVENTS, Topics.TRANSFER_EVENTS})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransferSagaIntegrationTest {

    private static final String FROM = "500100000017";
    private static final String TO = "500100000025";

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired TransferRepository transfers;

    private Consumer<String, String> consumer;
    private final List<BankEvent> seen = new ArrayList<>();
    private final String user = UUID.randomUUID().toString();
    private String token;

    @BeforeAll
    void setUp() {
        consumer = new DefaultKafkaConsumerFactory<>(KafkaTestUtils.consumerProps("test-saga", "false", broker),
                new StringDeserializer(), new StringDeserializer()).createConsumer();
        broker.consumeFromEmbeddedTopics(consumer, Topics.ACCOUNT_COMMANDS, Topics.TRANSFER_EVENTS);
        token = jwt.issue(new AuthenticatedUser(user, "u@test.dev", "USER"));
    }

    @AfterAll
    void tearDown() {
        consumer.close();
    }

    @Test
    void happyPathCompensationAndFailure() throws Exception {
        // 1. Happy path, with an idempotent retry of the initiating request.
        UUID t1 = initiate("k-1", "300.00");
        assertThat(initiate("k-1", "300.00")).isEqualTo(t1);
        mvc.perform(transferRequest("k-1", "999.00")).andExpect(status().isConflict());

        assertThat(expect(DebitAccount.class, t1).amount()).isEqualByComparingTo("300.00");
        reply(new AccountDebited(t1, FROM, new BigDecimal("700.00")));
        CreditAccount credit = expect(CreditAccount.class, t1);
        assertThat(credit.accountNumber()).isEqualTo(TO);
        reply(new AccountCredited(t1, TO));
        expect(TransferCompleted.class, t1);
        assertStatus(t1, TransferStatus.COMPLETED);

        // A late duplicate reply changes nothing.
        reply(new AccountDebited(t1, FROM, new BigDecimal("700.00")));
        Thread.sleep(500);
        assertStatus(t1, TransferStatus.COMPLETED);

        // 2. Credit leg fails -> refund -> REVERSED, and the limit is given back.
        UUID t2 = initiate("k-2", "200.00");
        expect(DebitAccount.class, t2);
        reply(new AccountDebited(t2, FROM, new BigDecimal("500.00")));
        expect(CreditAccount.class, t2);
        reply(new CreditRejected(t2, TO, "Beneficiary account cannot receive funds"));
        RefundAccount refund = expect(RefundAccount.class, t2);
        assertThat(refund.accountNumber()).isEqualTo(FROM);
        assertStatus(t2, TransferStatus.COMPENSATING);
        reply(new AccountRefunded(t2, FROM));
        assertThat(expect(TransferFailed.class, t2).reason()).contains("cannot receive");
        assertStatus(t2, TransferStatus.REVERSED);

        // 3. Debit refused -> FAILED.
        UUID t3 = initiate("k-3", "100.00");
        expect(DebitAccount.class, t3);
        reply(new DebitRejected(t3, FROM, "Insufficient funds"));
        expect(TransferFailed.class, t3);
        assertStatus(t3, TransferStatus.FAILED);

        // Only the completed 300 counts against today's 1000 limit.
        mvc.perform(get("/api/transfers/limits").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.usedToday").value(300.00))
                .andExpect(jsonPath("$.remainingToday").value(700.00));

        // Limits: per-transfer 800, daily 1000.
        mvc.perform(transferRequest("k-4", "850.00"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LIMIT_EXCEEDED"));
        initiate("k-5", "700.00");
        mvc.perform(transferRequest("k-6", "1.00"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LIMIT_EXCEEDED"));

        // History is per user.
        mvc.perform(get("/api/transfers").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(4));
        String stranger = jwt.issue(new AuthenticatedUser("someone-else", "x@test.dev", "USER"));
        mvc.perform(get("/api/transfers/" + t1).header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void validationAndDegradedLookup() throws Exception {
        mvc.perform(post("/api/transfers").header("Authorization", "Bearer " + token).header("Idempotency-Key", "v-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccount\":\"" + FROM + "\",\"toAccount\":\"" + FROM + "\",\"amount\":10}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/transfers").header("Authorization", "Bearer " + token).header("Idempotency-Key", "v-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccount\":\"" + FROM + "\",\"toAccount\":\"" + TO + "\",\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        // Saving a payee needs verification, which is unavailable here: 503 rather than a blind save.
        mvc.perform(post("/api/beneficiaries").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Mom\",\"accountNumber\":\"" + TO + "\"}"))
                .andExpect(status().isServiceUnavailable());
        mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private UUID initiate(String key, String amount) throws Exception {
        String body = mvc.perform(transferRequest(key, amount))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.transferId"));
    }

    private org.springframework.test.web.servlet.RequestBuilder transferRequest(String key, String amount) {
        return post("/api/transfers").header("Authorization", "Bearer " + token).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccount\":\"" + FROM + "\",\"toAccount\":\"" + TO + "\",\"amount\":" + amount
                        + ",\"description\":\"test\"}");
    }

    private void reply(BankEvent event) {
        kafka.send(Topics.ACCOUNT_REPLIES, UUID.randomUUID().toString(), EventCodec.encode(event)).join();
    }

    private void assertStatus(UUID id, TransferStatus expected) {
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(transfers.findById(id).orElseThrow().getStatus()).isEqualTo(expected));
    }

    private <T extends BankEvent> T expect(Class<T> type, UUID transferId) {
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            for (BankEvent e : seen) {
                if (type.isInstance(e) && e.toString().contains(transferId.toString())) {
                    seen.remove(e);
                    return type.cast(e);
                }
            }
            KafkaTestUtils.getRecords(consumer, Duration.ofMillis(500))
                    .forEach(r -> seen.add(EventCodec.decode(r.value())));
        }
        throw new AssertionError("No " + type.getSimpleName() + " for " + transferId);
    }
}
