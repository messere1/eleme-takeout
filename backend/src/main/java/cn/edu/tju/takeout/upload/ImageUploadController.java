package cn.edu.tju.takeout.upload;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.ApiResponse;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/images")
public class ImageUploadController {
    private final ImageUploadService service;
    public ImageUploadController(ImageUploadService service) { this.service = service; }

    @PostMapping
    public ApiResponse<Map<String, String>> upload(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String targetType, @RequestParam Long targetId,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(Map.of("url", service.upload(principal, targetType, targetId, file)));
    }
}
