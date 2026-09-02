package cn.edu.tju.takeout.merchant;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantService {
    private final ShopMapper shopMapper;
    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;

    public MerchantService(MerchantMapper merchantMapper, ShopMapper shopMapper, PasswordEncoder passwordEncoder) {
        // 仅保留依赖签名，等待功能开发人员实现。
        this.merchantMapper=merchantMapper;
        this.shopMapper=shopMapper;
        this.passwordEncoder=passwordEncoder;

    }

    private BusinessException merchantAlreadyExists(String message) {
        return new BusinessException(
            HttpStatus.CONFLICT,
            "MERCHANT_ALREADY_EXISTS",
            message
        );
    }

    @Transactional
    public MerchantView register(MerchantRegistrationRequest request) {
        String merchantname=request.merchantName().trim();

        if (merchantMapper.findByName(merchantname).isPresent()){
            throw merchantAlreadyExists("商家名称已存在");
        }

        if (merchantMapper.findByPhone(request.phone()).isPresent()){
            throw merchantAlreadyExists("手机号已存在");
        }

        String passwordHash=passwordEncoder.encode(request.password());
        Merchant merchant=Merchant.registered(
            merchantname, 
            request.phone(), 
            passwordHash, 
            request.businessScope().trim()
        );
        
        merchantMapper.insert(merchant);

        Shop shop=Shop.initiallyClosed(merchant.getId(), merchantname);

        shopMapper.insert(shop);

        return new MerchantView(
            merchant.getId(),
            merchant.getMerchantName(),
            merchant.getPhone(),
            merchant.getBusinessScope(),
            shop.getId(),
            shop.getStatus()
        );
    }
}
