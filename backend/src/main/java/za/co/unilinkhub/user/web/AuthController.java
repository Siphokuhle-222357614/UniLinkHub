package za.co.unilinkhub.user.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.security.JwtService;
import za.co.unilinkhub.security.UserPrincipal;
import za.co.unilinkhub.user.application.ConfirmEmailChangeUseCase;
import za.co.unilinkhub.user.application.RegisterUserUseCase;
import za.co.unilinkhub.user.application.RequestPasswordResetUseCase;
import za.co.unilinkhub.user.application.ResetPasswordUseCase;
import za.co.unilinkhub.user.application.UserDTO;
import za.co.unilinkhub.user.application.UserService;
import za.co.unilinkhub.user.domain.AccountStatus;
import za.co.unilinkhub.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import za.co.unilinkhub.security.AuthAbuseGuard;
import za.co.unilinkhub.user.application.StudentEmailPolicy;
import za.co.unilinkhub.user.repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ConfirmEmailChangeUseCase confirmEmailChangeUseCase;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAbuseGuard authAbuseGuard;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody UserRequest.Register request, HttpServletRequest http) {
        authAbuseGuard.beforeRegister(http);
        UserDTO user = registerUserUseCase.execute(
                request.studentNumber(), request.firstName(), request.lastName(),
                request.email(), request.password()
        );
        return UserResponse.from(user);
    }

    @GetMapping("/verify")
    public UserResponse verify(@RequestParam String token) {
        return UserResponse.from(userService.verifyEmail(token));
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification(@Valid @RequestBody UserRequest.ForgotPassword request, HttpServletRequest http) {
        authAbuseGuard.beforeSendingEmail("verify", request.email(), http);
        userService.resendVerification(request.email());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody UserRequest.Login rawRequest, HttpServletRequest http) {
        UserRequest.Login request = new UserRequest.Login(StudentEmailPolicy.normalize(rawRequest.email()), rawRequest.password());
        authAbuseGuard.beforeLogin(request.email(), http);
        try {
            AuthResponse response = doLogin(request);
            authAbuseGuard.loginSucceeded(request.email());
            return response;
        } catch (BadCredentialsException ex) {
            authAbuseGuard.loginFailed(request.email(), http);
            throw ex;
        } catch (DisabledException ex) {
            // A deactivated (self-service) account can reactivate itself simply by logging back
            // in with the right password. A pending-verification account (also "disabled") has to
            // click the link in its verification email first.
            User user = userRepository.findByEmail(request.email()).orElse(null);
            if (user != null && user.getAccountStatus() == AccountStatus.DEACTIVATED
                    && passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                user.reactivate();
                userRepository.save(user);
                return doLogin(request);
            }
            throw new DisabledException("Please verify your email address before logging in. We sent a link to "
                    + request.email() + " when you signed up - check your inbox and spam folder, or ask us to send a new one.");
        } catch (LockedException ex) {
            User user = userRepository.findByEmail(request.email()).orElse(null);
            String reason = user == null ? null : user.getSuspensionReason();
            String message = "Your account has been suspended"
                    + (reason != null && !reason.isBlank() ? ": " + reason : ".")
                    + " Contact an admin if you think this is a mistake.";
            throw new LockedException(message);
        }
    }

    private AuthResponse doLogin(UserRequest.Login request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        UserDTO userDTO = userService.getById(principal.getId());
        return new AuthResponse(token, UserResponse.from(userDTO));
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody UserRequest.ForgotPassword request, HttpServletRequest http) {
        authAbuseGuard.beforeSendingEmail("reset", request.email(), http);
        requestPasswordResetUseCase.execute(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody UserRequest.ResetPassword request) {
        resetPasswordUseCase.execute(request.token(), request.newPassword());
    }

    @GetMapping("/confirm-email-change")
    public UserResponse confirmEmailChange(@RequestParam String token) {
        return UserResponse.from(confirmEmailChangeUseCase.execute(token));
    }
}
