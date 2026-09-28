package za.co.unilinkhub.user.web;

public record AuthResponse(String token, UserResponse user) {
}
