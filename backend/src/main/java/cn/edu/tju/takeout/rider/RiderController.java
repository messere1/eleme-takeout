package cn.edu.tju.takeout.rider;
import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.ApiResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/rider/orders")
public class RiderController {
    private final RiderService service;
    public RiderController(RiderService service) { this.service = service; }
    @GetMapping("/available") public ApiResponse<List<RiderOrderView>> available(){return ApiResponse.success(service.available());}
    @GetMapping public ApiResponse<List<RiderOrderView>> mine(@AuthenticationPrincipal UserPrincipal p){return ApiResponse.success(service.mine(p.userId()));}
    @PostMapping("/{id}/claim") public ApiResponse<RiderOrderView> claim(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long id){return ApiResponse.success(service.claim(p.userId(),id));}
    @PostMapping("/{id}/deliver") public ApiResponse<RiderOrderView> deliver(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long id){return ApiResponse.success(service.deliver(p.userId(),id));}
}
