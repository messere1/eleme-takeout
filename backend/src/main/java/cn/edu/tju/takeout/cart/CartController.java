package cn.edu.tju.takeout.cart;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) { this.cartService = cartService; }

    @PostMapping("/items")
    public ApiResponse<CartItemView> add(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddCartRequest request) {
        return ApiResponse.success(cartService.add(principal.userId(), request));
    }

    @GetMapping
    public ApiResponse<CartView> get(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(cartService.get(principal.userId()));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartItemView> update(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartRequest request) {
        return ApiResponse.success(cartService.update(principal.userId(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long itemId) {
        cartService.delete(principal.userId(), itemId);
        return ApiResponse.success(null);
    }

    @DeleteMapping
    public ApiResponse<Void> clear(@AuthenticationPrincipal UserPrincipal principal) {
        cartService.clear(principal.userId());
        return ApiResponse.success(null);
    }
}
