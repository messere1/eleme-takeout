package cn.edu.tju.takeout.user;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private String username;
    private String phone;
    private String passwordHash;
    private String nickname;
    private String address;
    private String avatarUrl;
    private boolean enabled = true;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;

    public static User registered(String username, String phone, String passwordHash) {
        User user = new User();
        user.username = username;
        user.phone = phone;
        user.passwordHash = passwordHash;
        user.nickname = username;
        user.createdAt = LocalDateTime.now();
        return user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public String getNickname() { return nickname; }
    public String getAddress() { return address; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getAvatarUrl() { return avatarUrl; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void disable(LocalDateTime deletedAt) { this.enabled = false; this.deletedAt = deletedAt; }
    public void updateProfile(String nickname, String phone, String address) {
        this.nickname = nickname;
        this.phone = phone;
        this.address = address;
    }
}
