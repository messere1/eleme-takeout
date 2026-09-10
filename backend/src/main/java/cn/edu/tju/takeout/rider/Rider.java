package cn.edu.tju.takeout.rider;

public class Rider {
    private Long id; private String riderName; private String phone; private String passwordHash; private boolean enabled;
    public Long getId() { return id; } public String getRiderName() { return riderName; }
    public String getPhone() { return phone; } public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }
}
