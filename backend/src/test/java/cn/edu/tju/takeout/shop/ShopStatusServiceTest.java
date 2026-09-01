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
class ShopStatusServiceTest {
    @Mock private ShopMapper shopMapper;
    private ShopService shopService;

    @BeforeEach
    void setUp() {
        shopService = new ShopService(shopMapper);
    }

    @Test
    void ownerCanOpenClosedShop() {
        Shop shop = shop(20L, 12L);
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop));

        ShopView result = shopService.changeStatus(12L, 20L, new ShopStatusRequest("OPEN"));

        verify(shopMapper).updateStatus(shop);
        assertThat(result.status()).isEqualTo("OPEN");
    }

    @Test
    void ownerCanTemporarilyCloseShop() {
        Shop shop = shop(20L, 12L);
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop));

        assertThat(shopService.changeStatus(12L, 20L,
                new ShopStatusRequest("TEMP_CLOSED")).status()).isEqualTo("TEMP_CLOSED");
    }

    @Test
    void anotherMerchantCannotChangeShopStatus() {
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(20L, 12L)));

        assertThatThrownBy(() -> shopService.changeStatus(99L, 20L, new ShopStatusRequest("OPEN")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("FORBIDDEN");
    }

    private static Shop shop(Long id, Long merchantId) {
        Shop shop = Shop.initiallyClosed(merchantId, "北洋餐厅");
        shop.setId(id);
        return shop;
    }
}

