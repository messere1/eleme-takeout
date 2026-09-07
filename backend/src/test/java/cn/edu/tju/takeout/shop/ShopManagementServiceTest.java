package cn.edu.tju.takeout.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.common.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShopManagementServiceTest {
    @Mock private ShopMapper shopMapper;
    private ShopService shopService;

    @BeforeEach
    void setUp() { shopService = new ShopService(shopMapper); }

    @Test
    void shopDetailsExposeCurrentInformation() {
        Shop shop = shop(20L, 12L);
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop));

        ShopView result = shopService.getShop(20L);

        assertThat(result.shopName()).isEqualTo("北洋餐厅");
        assertThat(result.status()).isEqualTo("CLOSED");
    }

    @Test
    void ownerCanUpdateShopNameAndNotice() {
        Shop shop = shop(20L, 12L);
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop));

        ShopView result = shopService.updateShop(12L, 20L,
                new ShopUpdateRequest("北洋风味餐厅", "今日满20减3"));

        verify(shopMapper).updateInfo(shop);
        assertThat(result.shopName()).isEqualTo("北洋风味餐厅");
        assertThat(result.notice()).isEqualTo("今日满20减3");
    }

    @Test
    void nonOwnerCannotUpdateShop() {
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(20L, 12L)));

        assertThatThrownBy(() -> shopService.updateShop(99L, 20L,
                new ShopUpdateRequest("越权修改", "无")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void missingShopReturnsNotFound() {
        when(shopMapper.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.getShop(404L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    private static Shop shop(Long id, Long merchantId) {
        Shop shop = Shop.initiallyClosed(merchantId, "北洋餐厅");
        shop.setId(id);
        return shop;
    }
}
