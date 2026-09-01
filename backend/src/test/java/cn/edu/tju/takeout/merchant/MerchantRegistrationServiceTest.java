package cn.edu.tju.takeout.merchant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MerchantRegistrationServiceTest {
    @Mock private MerchantMapper merchantMapper;
    @Mock private ShopMapper shopMapper;
    private MerchantService merchantService;

    @BeforeEach
    void setUp() {
        merchantService = new MerchantService(merchantMapper, shopMapper, new BCryptPasswordEncoder());
    }

    @Test
    void registersMerchantAndCreatesClosedShopAtomically() {
        when(merchantMapper.findByName("北洋餐厅")).thenReturn(Optional.empty());
        when(merchantMapper.findByPhone("13800138000")).thenReturn(Optional.empty());
        org.mockito.Mockito.doAnswer(invocation -> {
            ((Merchant) invocation.getArgument(0)).setId(12L);
            return null;
        }).when(merchantMapper).insert(any());

        MerchantView result = merchantService.register(new MerchantRegistrationRequest(
                "北洋餐厅", "13800138000", "abc12345", "中式快餐"));

        ArgumentCaptor<Merchant> merchant = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantMapper).insert(merchant.capture());
        assertThat(merchant.getValue().getPasswordHash()).startsWith("$2");
        ArgumentCaptor<Shop> shop = ArgumentCaptor.forClass(Shop.class);
        verify(shopMapper).insert(shop.capture());
        assertThat(shop.getValue().getMerchantId()).isEqualTo(12L);
        assertThat(shop.getValue().getStatus()).isEqualTo("CLOSED");
        assertThat(result.shopStatus()).isEqualTo("CLOSED");
    }

    @Test
    void duplicateMerchantNameIsRejected() {
        when(merchantMapper.findByName("北洋餐厅")).thenReturn(Optional.of(new Merchant()));

        assertThatThrownBy(() -> merchantService.register(new MerchantRegistrationRequest(
                "北洋餐厅", "13800138000", "abc12345", "中式快餐")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("MERCHANT_ALREADY_EXISTS");
        verify(merchantMapper, never()).insert(any());
        verify(shopMapper, never()).insert(any());
    }
}

