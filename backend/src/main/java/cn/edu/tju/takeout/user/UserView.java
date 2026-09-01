package cn.edu.tju.takeout.user;

public record UserView(Long id, String username, String phone, String nickname, String address) {
    static UserView from(User user) {
        return new UserView(user.getId(), user.getUsername(), user.getPhone(), user.getNickname(), user.getAddress());
    }
}

