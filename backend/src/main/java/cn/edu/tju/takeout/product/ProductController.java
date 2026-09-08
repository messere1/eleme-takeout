package cn.edu.tju.takeout.product;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) { this.productService = productService; }

    @GetMapping("/shops/{shopId}/products")
    public ApiResponse<List<ProductView>> list(
            @PathVariable Long shopId, @RequestParam Long categoryId) {
        return ApiResponse.success(productService.listVisible(shopId, categoryId));
    }

    @PostMapping("/shops/{shopId}/products")
    public ApiResponse<ProductView> create(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long shopId,
            @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(productService.create(principal.userId(), shopId, request));
    }

    @PatchMapping("/products/{productId}")
    public ApiResponse<ProductView> update(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long productId,
            @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(productService.update(principal.userId(), productId, request));
    }

    @PatchMapping("/products/{productId}/status")
    public ApiResponse<ProductView> changeStatus(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long productId,
            @Valid @RequestBody ProductStatusRequest request) {
        return ApiResponse.success(
                productService.changeStatus(principal.userId(), productId, request));
    }

    @DeleteMapping("/products/{productId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long productId) {
        productService.delete(principal.userId(), productId);
        return ApiResponse.success(null);
    }

    @PatchMapping("/products/{productId}/stock")
    public ApiResponse<ProductView> updateStock(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long productId,
            @Valid @RequestBody StockRequest request) {
        return ApiResponse.success(
                productService.updateStock(principal.userId(), productId, request));
    }

    @PatchMapping("/products/{productId}/price")
    public ApiResponse<ProductView> updatePrice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId,
            @Valid @RequestBody ProductPriceRequest request) {
        return ApiResponse.success(
                productService.updatePrice(principal.userId(), productId, request));
    }

    @GetMapping("/categories/{categoryId}/products")
    public ApiResponse<ProductPage> list(
        @PathVariable Long categoryId,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "20") Integer size) {
            return ApiResponse.success(
                productService.listVisible(categoryId, page,size)
            );
        }
    
    @GetMapping("/products/{productId}")
    public ApiResponse<ProductView> getProduct(
        @PathVariable Long productId) {

        return ApiResponse.success(
            productService.getVisibleProduct(productId)
        );
    }

    @GetMapping("/merchant/products")
    public ApiResponse<List<ProductView>> listForMerchant(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                productService.listForMerchant(principal.userId()));
    }

}
