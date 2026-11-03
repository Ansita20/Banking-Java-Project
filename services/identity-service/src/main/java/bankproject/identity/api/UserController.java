package bankproject.identity.api;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import bankproject.common.security.CurrentUser;
import bankproject.identity.api.AuthDtos.ChangePasswordRequest;
import bankproject.identity.api.UserDtos.KycRequest;
import bankproject.identity.api.UserDtos.UserResponse;
import bankproject.identity.api.UserDtos.UserStatusRequest;
import bankproject.identity.service.AuthService;
import bankproject.identity.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final CustomerService customerService;
    private final AuthService authService;

    @GetMapping("/api/users/me")
    public UserResponse me() {
        return customerService.get(CurrentUser.get().userId());
    }

    @PutMapping("/api/users/me/kyc")
    public UserResponse submitKyc(@Valid @RequestBody KycRequest request) {
        return customerService.submitKyc(CurrentUser.get().userId(), request);
    }

    @PostMapping("/api/users/me/change-password")
    public Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(CurrentUser.get().userId(), request);
        return Map.of("message", "Password changed");
    }

    @GetMapping("/api/admin/users")
    public List<UserResponse> allUsers() {
        return customerService.listAll();
    }

    @PatchMapping("/api/admin/users/{userId}/status")
    public UserResponse setStatus(@PathVariable String userId, @RequestBody UserStatusRequest request) {
        return customerService.setEnabled(userId, request.enabled());
    }
}
