package cn.edu.tju.takeout.admin;

public class Admin {
    private Long id; private String username; private String passwordHash; private boolean enabled;
    public Long getId() { return id; } public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; } public boolean isEnabled() { return enabled; }
}
