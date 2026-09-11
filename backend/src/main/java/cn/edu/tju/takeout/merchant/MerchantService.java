package cn.edu.tju.takeout.merchant;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;
import cn.edu.tju.takeout.catalog.BusinessCategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantService {
    private final ShopMapper shopMapper;
    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;
    private final BusinessCategoryMapper businessCategoryMapper;

    public MerchantService(MerchantMapper merchantMapper, ShopMapper shopMapper, PasswordEncoder passwordEncoder) {
        this(merchantMapper, shopMapper, passwordEncoder, null);
    }

    @Autowired
    public MerchantService(MerchantMapper merchantMapper, ShopMapper shopMapper,
            PasswordEncoder passwordEncoder, BusinessCategoryMapper businessCategoryMapper) {
        this.merchantMapper=merchantMapper;
        this.shopMapper=shopMapper;
        this.passwordEncoder=passwordEncoder;
        this.businessCategoryMapper = businessCategoryMapper;

    }

    private BusinessException merchantAlreadyExists(String message) {
        return new BusinessException(
            HttpStatus.CONFLICT,
            "MERCHANT_ALREADY_EXISTS",
            message
        );
    }

    public MerchantProfileView getMe(Long merchantId) {
        Merchant merchant = merchantMapper.findById(merchantId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "商家不存在"));
        Shop shop = shopMapper.findByMerchantId(merchantId).orElse(null);
        return new MerchantProfileView(
                merchant.getId(), merchant.getMerchantName(), merchant.getPhone(),
                merchant.getBusinessScope(),
                shop != null ? shop.getId() : null,
                shop != null ? shop.getShopName() : null,
                shop != null ? shop.getStatus() : null,
                shop != null ? shop.getShopAddress() : null,
                shop != null ? shop.getImageUrl() : null,
                shop != null ? shop.getCoverImageUrl() : null);
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

        String scope = request.businessScope().trim();
        if (businessCategoryMapper != null && businessCategoryMapper.findByName(scope).isEmpty()) {
            businessCategoryMapper.insert(new BusinessCategoryMapper.MutableCategory(scope));
        }

        Shop shop=Shop.initiallyClosed(merchant.getId(), merchantname);
        shop.setShopAddress(request.shopAddress() == null ? "地址待完善" : request.shopAddress().trim());

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

    @Transactional public MerchantProfileView updateMe(Long merchantId,MerchantProfileUpdateRequest r){
        String scope=r.businessScope().trim();if(businessCategoryMapper!=null&&businessCategoryMapper.findByName(scope).isEmpty())businessCategoryMapper.insert(new BusinessCategoryMapper.MutableCategory(scope));
        if(merchantMapper.updateScope(merchantId,scope)==0)throw new BusinessException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","商家不存在");
        Shop shop=shopMapper.findByMerchantId(merchantId).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","店铺不存在"));
        shopMapper.updateProfile(shop.getId(),r.shopName().trim(),r.shopAddress().trim());return getMe(merchantId);
    }

    @Transactional public void deleteMe(Long merchantId){if(merchantMapper.softDelete(merchantId)==0)throw new BusinessException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","商家不存在或已注销");}
}
