package cn.edu.tju.takeout.user;

public record UserView(Long id, String username, String phone, String nickname, String address,
                       String avatarUrl, boolean enabled) {
    public UserView(Long id, String username, String phone, String nickname, String address) {
        this(id, username, phone, nickname, address, null, true);
    }
    static UserView from(User user) {
        return new UserView(user.getId(), user.getUsername(), user.getPhone(), user.getNickname(), user.getAddress(),
                user.getAvatarUrl(), user.isEnabled());
    }
}
