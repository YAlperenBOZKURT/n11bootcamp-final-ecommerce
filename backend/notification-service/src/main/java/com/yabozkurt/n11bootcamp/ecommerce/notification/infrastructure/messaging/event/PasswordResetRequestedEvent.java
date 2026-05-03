package com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event;

public class PasswordResetRequestedEvent {
    private Long userId;
    private String email;
    private String firstName;
    private String resetToken;
    private Long expiresInSeconds;

    public PasswordResetRequestedEvent() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public Long getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(Long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }
}
