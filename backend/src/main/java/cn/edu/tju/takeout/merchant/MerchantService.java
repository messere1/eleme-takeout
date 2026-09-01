package cn.edu.tju.takeout.merchant;

import cn.edu.tju.takeout.shop.ShopMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MerchantService {
    public MerchantService(
            MerchantMapper merchantMapper, ShopMapper shopMapper, PasswordEncoder passwordEncoder) {
        // 仅保留依赖签名，等待功能开发人员实现。
    }

    public MerchantView register(MerchantRegistrationRequest request) {
        throw new UnsupportedOperationException("待功能开发：商家注册业务尚未实现");
    }
}
