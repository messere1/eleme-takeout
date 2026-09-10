package cn.edu.tju.takeout.merchant;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {
    private final MerchantService merchantService;
    public MerchantController(MerchantService merchantService) { this.merchantService = merchantService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MerchantView> register(@Valid @RequestBody MerchantRegistrationRequest request) {
        return ApiResponse.success(merchantService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<MerchantProfileView> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(merchantService.getMe(principal.userId()));
    }
    @PatchMapping("/me") public ApiResponse<MerchantProfileView> update(@AuthenticationPrincipal UserPrincipal p,@Valid @RequestBody MerchantProfileUpdateRequest r){return ApiResponse.success(merchantService.updateMe(p.userId(),r));}
    @DeleteMapping("/me") public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal p){merchantService.deleteMe(p.userId());return ApiResponse.success(null);}
}
