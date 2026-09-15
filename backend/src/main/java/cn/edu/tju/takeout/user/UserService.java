package cn.edu.tju.takeout.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.edu.tju.takeout.common.BusinessException;

@Service
public class UserService {

    //注入构造器
    private final UserMapper userMapper;

    //加密密码
    private final PasswordEncoder passwordEncoder;
    private final cn.edu.tju.takeout.order.OrderMapper orderMapper;
    private final cn.edu.tju.takeout.recommend.SearchHistoryMapper searchHistoryMapper;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this(userMapper, passwordEncoder, null);
    }

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder,
            cn.edu.tju.takeout.order.OrderMapper orderMapper) {
        this(userMapper, passwordEncoder, orderMapper, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder,
            cn.edu.tju.takeout.order.OrderMapper orderMapper,
            cn.edu.tju.takeout.recommend.SearchHistoryMapper searchHistoryMapper) {
        this.userMapper=userMapper;
        this.passwordEncoder=passwordEncoder;
        this.orderMapper=orderMapper;
        this.searchHistoryMapper=searchHistoryMapper;
    }

    // 异常辅助方法
    private BusinessException userAlreadyExists(String message) {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "USER_ALREADY_EXISTS",
                message
        );
    }


    public UserView register(UserRegistrationRequest request) {
        String username=request.username().trim();
        if (userMapper.findByUsername(username).isPresent()){
            throw userAlreadyExists("用户名已存在");
        }
        
        if (userMapper.findByPhone(request.phone()).isPresent()){
            throw userAlreadyExists("手机号已存在");
        }

        String passwordHash=passwordEncoder.encode(request.password());
        
        User user=User.registered(username, request.phone(), passwordHash);

        userMapper.insert(user);

        return UserView.from(user);

    }

    public UserView getProfile(Long userId) {
        User user=userMapper.findById(userId).orElseThrow(()->
        new BusinessException(
            HttpStatus.NOT_FOUND, 
            "RESOURCE_NOT_FOUND",
            "用户不存在"));

        return UserView.from(user);

    }

    public UserView updateProfile(Long userId, UserProfileUpdateRequest request) {
        // 查找用户
        User user=userMapper.findById(userId).orElseThrow(()->
        new BusinessException(
            HttpStatus.NOT_FOUND, 
            "RESOURCE_NOT_FOUND",
            "用户不存在"));

        // 查找手机号
        userMapper.findByPhone(request.phone())
            .filter(existing -> !existing.getId().equals(userId))
            .ifPresent(existing->{
                throw userAlreadyExists("手机号已存在");
            });        

        user.updateProfile(
            request.nickname().trim(), 
            request.phone(), 
            request.address().trim()
        );

        userMapper.updateProfile(user);
        return UserView.from(user);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        if (orderMapper != null && orderMapper.countActiveByUser(userId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT,"BUSINESS_CONFLICT","存在进行中订单，暂不能注销");
        }
        if (userMapper.softDelete(userId) == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","用户不存在或已注销");
        }
        // 搜索历史属于个人数据，注销时一并清除
        if (searchHistoryMapper != null) {
            searchHistoryMapper.deleteByUserId(userId);
        }
    }

}
