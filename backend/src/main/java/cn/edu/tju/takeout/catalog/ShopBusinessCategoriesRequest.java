package cn.edu.tju.takeout.catalog;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
public record ShopBusinessCategoriesRequest(
        @NotEmpty(message="至少选择一个经营品类")
        @Size(max=3,message="最多选择三个经营品类") List<Long> categoryIds) {}
