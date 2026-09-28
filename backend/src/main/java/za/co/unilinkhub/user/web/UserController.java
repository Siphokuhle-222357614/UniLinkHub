package za.co.unilinkhub.user.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.security.CurrentUser;
import za.co.unilinkhub.user.application.ChangePasswordUseCase;
import za.co.unilinkhub.user.application.DeactivateAccountUseCase;
import za.co.unilinkhub.user.application.RequestEmailChangeUseCase;
import za.co.unilinkhub.user.application.UpdateProfileUseCase;
import za.co.unilinkhub.user.application.UserDTO;
import za.co.unilinkhub.user.application.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final RequestEmailChangeUseCase requestEmailChangeUseCase;
    private final DeactivateAccountUseCase deactivateAccountUseCase;

    public record SuspendRequest(String reason) {
    }

    @GetMapping("/api/users/me")
    public UserResponse me(@CurrentUser UUID userId) {
        return UserResponse.from(userService.getById(userId));
    }

    @PatchMapping("/api/users/me")
    public UserResponse updateProfile(@CurrentUser UUID userId, @Valid @RequestBody UserRequest.UpdateProfile request) {
        UserDTO updated = updateProfileUseCase.execute(userId, request.firstName(), request.lastName(), request.phoneNumber());
        return UserResponse.from(updated);
    }

    @PostMapping("/api/users/me/change-password")
    public void changePassword(@CurrentUser UUID userId, @Valid @RequestBody UserRequest.ChangePassword request) {
        changePasswordUseCase.execute(userId, request.currentPassword(), request.newPassword());
    }

    @PostMapping("/api/users/me/change-email")
    public void changeEmail(@CurrentUser UUID userId, @Valid @RequestBody UserRequest.ChangeEmail request) {
        requestEmailChangeUseCase.execute(userId, request.newEmail(), request.currentPassword());
    }

    @PostMapping("/api/users/me/become-seller")
    public UserResponse becomeSeller(@CurrentUser UUID userId) {
        return UserResponse.from(userService.becomeSeller(userId));
    }

    @PostMapping("/api/users/me/deactivate")
    public void deactivateAccount(@CurrentUser UUID userId, @Valid @RequestBody UserRequest.DeactivateAccount request) {
        deactivateAccountUseCase.execute(userId, request.currentPassword());
    }

    @PatchMapping("/api/users/me/notification-preferences")
    public UserResponse updateNotificationPreferences(@CurrentUser UUID userId, @RequestBody UserRequest.NotificationPreferences request) {
        return UserResponse.from(userService.updateNotificationPreferences(userId, request.disabledCategories()));
    }

    @GetMapping("/api/users/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return UserResponse.from(userService.getById(id));
    }

    @GetMapping("/api/admin/users/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> pendingAccounts() {
        return userService.listPendingAccounts().stream().map(UserResponse::from).toList();
    }

    @PostMapping("/api/admin/users/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse approveAccount(@PathVariable UUID id) {
        return UserResponse.from(userService.approveAccount(id));
    }

    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> listAccounts(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword) {
        return userService.listAccounts(status, keyword).stream().map(UserResponse::from).toList();
    }

    @PostMapping("/api/admin/users/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse suspendAccount(@CurrentUser UUID adminId, @PathVariable UUID id,
                                        @RequestBody(required = false) SuspendRequest request) {
        return UserResponse.from(userService.suspendAccount(id, adminId, request == null ? null : request.reason()));
    }

    @PostMapping("/api/admin/users/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse reactivateAccount(@CurrentUser UUID adminId, @PathVariable UUID id) {
        return UserResponse.from(userService.reactivateAccount(id, adminId));
    }

    @PostMapping("/api/admin/users/{id}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse promoteToAdmin(@CurrentUser UUID adminId, @PathVariable UUID id) {
        return UserResponse.from(userService.promoteToAdmin(id, adminId));
    }
}
