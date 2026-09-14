package cn.edu.tju.takeout.rider;
import cn.edu.tju.takeout.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/riders")
public class RiderRegistrationController{
    private final RiderService service;
    public RiderRegistrationController(RiderService service){this.service=service;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RiderView> register(@Valid @RequestBody RiderRegistrationRequest request){return ApiResponse.success(service.register(request));}
}
