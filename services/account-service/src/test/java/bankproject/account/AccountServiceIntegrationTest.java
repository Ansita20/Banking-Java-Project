package bankproject.account;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

import bankproject.account.domain.AccountRepository;
import bankproject.account.domain.CustomerRepository;
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
import bankproject.common.events.IdentityEvents.CustomerUpdated;
import bankproject.common.events.Topics;
import bankproject.common.security.AuthenticatedUser;
import bankproject.common.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {Topics.IDENTITY_EVENTS, Topics.ACCOUNT_COMMANDS, Topics.ACCOUNT_REPLIES,
        Topics.ACCOUNT_EVENTS, Topics.TRANSFER_EVENTS})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountServiceIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired AccountRepository accounts;
    @Autowired CustomerRepository customers;

    private Consumer<String, String> replies;
    private final String alice = UUID.randomUUID().toString();
    private final String bob = UUID.randomUUID().toString();
    private String aliceToken;
    private String bobToken;
    private String adminToken;

    @BeforeAll
    void setUp() {
        replies = new DefaultKafkaConsumerFactory<>(KafkaTestUtils.consumerProps("test-replies", "false", broker),
                new StringDeserializer(), new StringDeserializer()).createConsumer();
        broker.consumeFromAnEmbeddedTopic(replies, Topics.ACCOUNT_REPLIES);

        aliceToken = jwt.issue(new AuthenticatedUser(alice, "alice@test.dev", "USER"));
        bobToken = jwt.issue(new AuthenticatedUser(bob, "bob@test.dev", "USER"));
        adminToken = jwt.issue(new AuthenticatedUser("admin", "admin@test.dev", "ADMIN"));

        // The customer replica is fed by identity events, exactly as in production.
        publish(Topics.IDENTITY_EVENTS, alice, new CustomerUpdated(alice, "alice@test.dev", "Alice Rao", true, true, true));
        publish(Topics.IDENTITY_EVENTS, bob, new CustomerUpdated(bob, "bob@test.dev", "Bob Shah", true, true, true));
        await().atMost(Duration.ofSeconds(20))
                .until(() -> customers.existsById(alice) && customers.existsById(bob));
    }

    @AfterAll
    void tearDown() {
        replies.close();
    }

    @Test
    void accountLifecycleCashAndSagaCommands() throws Exception {
        String aliceAcc = openAndApprove(aliceToken, "SAVINGS");
        String bobAcc = openAndApprove(bobToken, "CURRENT");

        // Duplicate account type is refused.
        mvc.perform(post("/api/accounts").header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"SAVINGS\"}"))
                .andExpect(status().isConflict());

        // Idempotent deposit: the retry returns the same ledger entry and does not double-credit.
        String first = deposit(aliceToken, aliceAcc, "1000.00", "dep-1");
        String retry = deposit(aliceToken, aliceAcc, "1000.00", "dep-1");
        assertThat(JsonPath.<String>read(retry, "$.id")).isEqualTo(JsonPath.<String>read(first, "$.id"));
        assertThat(balance(aliceAcc)).isEqualByComparingTo("1000.00");

        mvc.perform(post("/api/accounts/" + aliceAcc + "/withdrawals").header("Authorization", "Bearer " + aliceToken)
                        .header("Idempotency-Key", "wd-1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":5000}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        // Other customers cannot see the account.
        mvc.perform(get("/api/accounts/" + aliceAcc).header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());

        // Saga debit, delivered twice: processed once, answered twice.
        UUID t1 = UUID.randomUUID();
        var debit = new DebitAccount(t1, aliceAcc, new BigDecimal("300.00"), alice, "rent");
        publish(Topics.ACCOUNT_COMMANDS, aliceAcc, debit);
        publish(Topics.ACCOUNT_COMMANDS, aliceAcc, debit);
        assertThat(nextReply(t1)).isInstanceOf(AccountDebited.class);
        assertThat(nextReply(t1)).isInstanceOf(AccountDebited.class);
        assertThat(balance(aliceAcc)).isEqualByComparingTo("700.00");

        publish(Topics.ACCOUNT_COMMANDS, bobAcc, new CreditAccount(t1, bobAcc, new BigDecimal("300.00"), "rent"));
        assertThat(nextReply(t1)).isInstanceOf(AccountCredited.class);
        assertThat(balance(bobAcc)).isEqualByComparingTo("300.00");

        // A debit by someone who does not own the account is rejected.
        UUID t2 = UUID.randomUUID();
        publish(Topics.ACCOUNT_COMMANDS, aliceAcc, new DebitAccount(t2, aliceAcc, BigDecimal.TEN, bob, "theft"));
        assertThat(nextReply(t2)).isInstanceOf(DebitRejected.class);

        // A rejected debit stays rejected on redelivery, even once the balance would allow it.
        UUID t3 = UUID.randomUUID();
        var big = new DebitAccount(t3, aliceAcc, new BigDecimal("5000.00"), alice, "big");
        publish(Topics.ACCOUNT_COMMANDS, aliceAcc, big);
        assertThat(((DebitRejected) nextReply(t3)).reason()).isEqualTo("Insufficient funds");
        deposit(aliceToken, aliceAcc, "10000.00", "dep-2");
        publish(Topics.ACCOUNT_COMMANDS, aliceAcc, big);
        assertThat(nextReply(t3)).isInstanceOf(DebitRejected.class);
        assertThat(balance(aliceAcc)).isEqualByComparingTo("10700.00");

        // Compensation puts the money back.
        publish(Topics.ACCOUNT_COMMANDS, aliceAcc, new RefundAccount(t1, aliceAcc, new BigDecimal("300.00"), "test"));
        assertThat(nextReply(t1)).isInstanceOf(AccountRefunded.class);
        assertThat(balance(aliceAcc)).isEqualByComparingTo("11000.00");

        // Frozen accounts cannot transact.
        mvc.perform(post("/api/admin/accounts/" + bobAcc + "/freeze").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"AML review\"}"))
                .andExpect(status().isOk());
        UUID t4 = UUID.randomUUID();
        publish(Topics.ACCOUNT_COMMANDS, bobAcc, new DebitAccount(t4, bobAcc, BigDecimal.ONE, bob, "x"));
        assertThat(nextReply(t4)).isInstanceOf(DebitRejected.class);

        mvc.perform(get("/api/accounts/" + aliceAcc + "/statement").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(4));
    }

    @Test
    void concurrentDepositsAreSerialized() throws Exception {
        String acc = openAndApprove(aliceToken, "CURRENT");
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            String key = "conc-" + i;
            results.add(pool.submit(() -> deposit(aliceToken, acc, "25.00", key)));
        }
        for (Future<String> f : results) {
            f.get();
        }
        pool.shutdown();
        assertThat(balance(acc)).isEqualByComparingTo("1000.00");
    }

    @Test
    void customersWithoutKycCannotOpenAccounts() throws Exception {
        String carol = UUID.randomUUID().toString();
        publish(Topics.IDENTITY_EVENTS, carol, new CustomerUpdated(carol, "c@test.dev", "Carol", true, false, true));
        await().atMost(Duration.ofSeconds(20)).until(() -> customers.existsById(carol));
        mvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + jwt.issue(new AuthenticatedUser(carol, "c@test.dev", "USER")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"SAVINGS\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("KYC_REQUIRED"));
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        mvc.perform(get("/api/admin/accounts").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
    }

    private String openAndApprove(String token, String type) throws Exception {
        String body = mvc.perform(post("/api/accounts").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"" + type + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        String accountNumber = JsonPath.read(body, "$.accountNumber");
        mvc.perform(post("/api/admin/accounts/" + accountNumber + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        return accountNumber;
    }

    private String deposit(String token, String account, String amount, String key) throws Exception {
        return mvc.perform(post("/api/accounts/" + account + "/deposits").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private BigDecimal balance(String account) {
        return accounts.findById(account).orElseThrow().getBalance();
    }

    private void publish(String topic, String key, BankEvent event) {
        kafka.send(topic, key, EventCodec.encode(event)).join();
    }

    /** Next reply on account.replies for this transfer, skipping others. */
    private BankEvent nextReply(UUID transferId) {
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            KafkaTestUtils.getRecords(replies, Duration.ofSeconds(1))
                    .forEach(record -> pending.add(EventCodec.decode(record.value())));
            for (BankEvent e : pending) {
                if (transferIdOf(e).equals(transferId)) {
                    pending.remove(e);
                    return e;
                }
            }
        }
        throw new AssertionError("No reply for " + transferId);
    }

    private final List<BankEvent> pending = new ArrayList<>();

    private static UUID transferIdOf(BankEvent e) {
        if (e instanceof AccountDebited r) return r.transferId();
        if (e instanceof DebitRejected r) return r.transferId();
        if (e instanceof AccountCredited r) return r.transferId();
        if (e instanceof AccountRefunded r) return r.transferId();
        if (e instanceof CreditRejected r) return r.transferId();
        throw new IllegalArgumentException(e.toString());
    }
}
