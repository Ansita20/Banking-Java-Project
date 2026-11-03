package bankproject.notification.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bankproject.common.web.ApiException;
import bankproject.notification.domain.Notification;
import bankproject.notification.domain.NotificationRepository;
import bankproject.notification.domain.RecipientRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public record NotificationResponse(UUID id, String category, String title, String message, Instant createdAt,
                                       boolean read) {

        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getCategory(), n.getTitle(), n.getMessage(),
                    n.getCreatedAt(), n.getReadAt() != null);
        }
    }

    private final NotificationRepository notifications;
    private final RecipientRepository recipients;
    private final EmailSender email;

    /** Stores an in-app notification and emails it, once per source event. */
    @Transactional
    public void notify(String userId, String category, String title, String message, String sourceEvent) {
        if (notifications.existsBySourceEvent(sourceEvent)) {
            return;
        }
        notifications.save(new Notification(userId, category, title, message, sourceEvent));
        recipients.findById(userId)
                .ifPresent(r -> email.send(r.getEmail(), title, "Dear " + r.getFullName() + ",\n\n" + message
                        + "\n\nIf you did not expect this, contact the bank immediately.\n\n- CoreBank", false));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(String userId, int page, int size) {
        return notifications.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, Math.min(size, 100)))
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unread(String userId) {
        return notifications.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(String userId, UUID id) {
        notifications.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"))
                .markRead();
    }

    @Transactional
    public void markAllRead(String userId) {
        notifications.markAllRead(userId, Instant.now());
    }
}
