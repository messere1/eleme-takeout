package cn.edu.tju.takeout.rider;

public class Rider {
    private Long id; private String riderName; private String phone; private String passwordHash; private boolean enabled = true;
    public static Rider registered(String riderName, String phone, String passwordHash) {
        Rider rider = new Rider(); rider.riderName = riderName; rider.phone = phone; rider.passwordHash = passwordHash; return rider;
    }
    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; } public String getRiderName() { return riderName; }
    public String getPhone() { return phone; } public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }
}
