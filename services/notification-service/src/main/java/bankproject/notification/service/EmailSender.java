package bankproject.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;

    @Value("${bank.mail.from:no-reply@corebank.local}")
    private String from;

    /** Local development without an SMTP server: write emails to the log instead of sending them. */
    @Value("${bank.mail.log-only:false}")
    private boolean logOnly;

    /** @param critical when true, failures propagate so Kafka retries delivery (e.g. OTPs). */
    public void send(String to, String subject, String body, boolean critical) {
        if (logOnly) {
            log.info("EMAIL to={} subject=\"{}\"\n{}", to, subject, body);
            return;
        }
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            if (critical) {
                throw e;
            }
            log.warn("Could not email {} ({}): {}", to, subject, e.getMessage());
        }
    }
}
