package cn.edu.tju.takeout.catalog;

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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/shops/{shopId}/categories")
    public ApiResponse<List<CategoryView>> list(@PathVariable Long shopId) {
        return ApiResponse.success(categoryService.list(shopId));
    }

    @PostMapping("/shops/{shopId}/categories")
    public ApiResponse<CategoryView> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long shopId,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(categoryService.create(principal.userId(), shopId, request));
    }

    @PatchMapping("/categories/{categoryId}")
    public ApiResponse<CategoryView> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(categoryService.update(principal.userId(), categoryId, request));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long categoryId) {
        categoryService.delete(principal.userId(), categoryId);
        return ApiResponse.success(null);
    }
}
