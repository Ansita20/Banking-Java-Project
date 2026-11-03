package bankproject.notification.api;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import bankproject.common.security.CurrentUser;
import bankproject.notification.service.NotificationService;
import bankproject.notification.service.NotificationService.NotificationResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifications;

    @GetMapping
    public Page<NotificationResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return notifications.list(CurrentUser.get().userId(), page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread() {
        return Map.of("unread", notifications.unread(CurrentUser.get().userId()));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID id) {
        notifications.markRead(CurrentUser.get().userId(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead() {
        notifications.markAllRead(CurrentUser.get().userId());
    }
}
