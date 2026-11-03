package bankproject.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import bankproject.common.events.EventCodec;
import bankproject.common.events.IdentityEvents.OtpIssued;
import bankproject.common.events.Topics;
import bankproject.common.outbox.OutboxRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = Topics.IDENTITY_EVENTS)
class IdentityFlowIntegrationTest {

    private static final String PASSWORD = "Secret123";

    @Autowired MockMvc mvc;
    @Autowired OutboxRepository outbox;
    @Autowired JdbcTemplate jdbc;

    @Test
    void registerVerifyLoginAndKyc() throws Exception {
        register("asha@test.dev").andExpect(status().isCreated());
        register("ASHA@test.dev").andExpect(status().isConflict());

        login("asha@test.dev", PASSWORD).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        String otp = latestOtp("asha@test.dev");
        String wrong = otp.equals("000000") ? "111111" : "000000";
        verify("asha@test.dev", wrong).andExpect(status().isBadRequest());
        verify("asha@test.dev", otp).andExpect(status().isOk());

        String body = login("asha@test.dev", PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.kycCompleted").value(false))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        mvc.perform(put("/api/users/me/kyc").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"9876543210","dateOfBirth":"1995-04-12","gender":"FEMALE",
                                 "addressLine":"12 MG Road","city":"Pune","state":"MH","postalCode":"411001",
                                 "panNumber":"ABCDE1234F","governmentId":"123412341234"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycCompleted").value(true))
                .andExpect(jsonPath("$.profile.panNumberMasked").value("******234F"));

        // PII is encrypted at rest.
        String storedPan = jdbc.queryForObject("select pan_number from users where email = ?", String.class,
                "asha@test.dev");
        assertThat(storedPan).isNotEqualTo("ABCDE1234F").doesNotContain("ABCDE");

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("asha@test.dev"));
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void repeatedFailuresLockTheAccount() throws Exception {
        register("ravi@test.dev").andExpect(status().isCreated());
        verify("ravi@test.dev", latestOtp("ravi@test.dev")).andExpect(status().isOk());
        for (int i = 0; i < 5; i++) {
            login("ravi@test.dev", "WrongPass1").andExpect(status().isUnauthorized());
        }
        login("ravi@test.dev", PASSWORD).andExpect(status().isLocked());
    }

    @Test
    void adminIsBootstrappedAndCanListUsers() throws Exception {
        String body = login("admin@corebank.local", "Admin@12345").andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + JsonPath.read(body, "$.accessToken")))
                .andExpect(status().isOk());
    }

    @Test
    void weakPasswordsAreRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"w@test.dev\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    private ResultActions register(String email) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"" + email + "\",\"password\":\""
                        + PASSWORD + "\"}"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions verify(String email, String code) throws Exception {
        return mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"));
    }

    /** The OTP only exists in plaintext inside the queued email event. */
    private String latestOtp(String email) {
        return outbox.findAll().stream()
                .filter(e -> e.getEventType().equals("OtpIssued"))
                .map(e -> (OtpIssued) EventCodec.decode(e.getPayload()))
                .filter(e -> e.email().equals(email))
                .reduce((a, b) -> b)
                .orElseThrow()
                .otp();
    }
}
