package cn.edu.tju.takeout.catalog;

import cn.edu.tju.takeout.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business-categories")
public class BusinessCategoryController {
    private final BusinessCategoryMapper mapper;
    public BusinessCategoryController(BusinessCategoryMapper mapper) { this.mapper = mapper; }
    @GetMapping public ApiResponse<List<BusinessCategory>> list() { return ApiResponse.success(mapper.findEnabled()); }
}
