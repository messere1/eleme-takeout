package cn.edu.tju.takeout.catalog;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;
import java.util.List;
import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ShopMapper shopMapper;
    public CategoryService(
            CategoryMapper categoryMapper, ProductMapper productMapper, ShopMapper shopMapper) {
        this.categoryMapper=categoryMapper;
        this.productMapper=productMapper;
        this.shopMapper=shopMapper;
    }

    public CategoryView create(Long merchantId, Long shopId, CategoryRequest request) {
        //先找商家
        Shop shop=shopMapper.findById(shopId).orElseThrow(()->
        new BusinessException(
            HttpStatus.NOT_FOUND, 
            "RESOURCE_NOT_FOUND", 
            "店铺不存在")
        );

        if(!shop.getMerchantId().equals(merchantId)){
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
            "无权操作该店铺"
            );
        }

        String name=request.name().trim();

        if(categoryMapper.findByName(shopId, name).isPresent()){
            throw new BusinessException(HttpStatus.CONFLICT,
                "BUSINESS_CONFLICT",
                "分类名称已存在"
            );
        }

        if (categoryMapper.findBySort(shopId,request.sort()).isPresent()) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "BUSINESS_CONFLICT",
                "分类排序值已存在"
            );
        }

        Category category=Category.of(
            null, 
            shopId, 
            name, 
            request.sort()
        );

        categoryMapper.insert(category);
        return CategoryView.from(category);
    }

    public List<CategoryView> list(Long shopId) {
        List<Category> list=categoryMapper.findAllByShopId(shopId);

        List<CategoryView>view=new ArrayList<>();

        for(Category c:list){
            view.add(CategoryView.from(c));
        }

        return view;
    }

    public CategoryView update(Long merchantId, Long categoryId, CategoryRequest request) {
        Category category=categoryMapper.findById(categoryId)
        .orElseThrow(()->
            new BusinessException(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND", 
                "分类不存在"
            )
        );

        Shop shop=shopMapper.findById(category.getShopId())
            .orElseThrow(()->new BusinessException(
                HttpStatus.NOT_FOUND, 
                "RESOURCE_NOT_FOUND",
                "店铺不存在"
            ));

        if(!shop.getMerchantId().equals(merchantId)){
            throw new BusinessException(
                HttpStatus.FORBIDDEN, 
                "FORBIDDEN", 
                "无权修改该分类"
            );
        }

        String name=request.name().trim();
        categoryMapper.findByName(category.getShopId(), name)
        .filter(existing->!existing.getId().equals(categoryId))
        .ifPresent(existing->{
            throw new BusinessException(
                HttpStatus.CONFLICT, 
                "BUSINESS_CONFLICT", 
                "分类名称已存在"
            );
        });

        categoryMapper.findBySort(category.getShopId(),request.sort())
        .filter(existing ->!existing.getId().equals(categoryId))
        .ifPresent(existing -> {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "BUSINESS_CONFLICT",
                    "分类排序值已存在"
            );
        });

        category.update(name, request.sort());
        categoryMapper.update(category);
        return CategoryView.from(category);
    }

    public void delete(Long merchantId, Long categoryId) {
        Category category=categoryMapper.findById(categoryId)
        .orElseThrow(()->
            new BusinessException(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND", 
                "分类不存在"
            )
        );

        Shop shop=shopMapper.findById(category.getShopId())
            .orElseThrow(()->new BusinessException(
                HttpStatus.NOT_FOUND, 
                "RESOURCE_NOT_FOUND",
                "店铺不存在"
            ));

        if(!shop.getMerchantId().equals(merchantId)){
            throw new BusinessException(
                HttpStatus.FORBIDDEN, 
                "FORBIDDEN", 
                "无权修改该分类"
            );
        }

        int productcount=productMapper.countByCategoryId(categoryId);
        if(productcount>0){
            throw new BusinessException(
                HttpStatus.CONFLICT, 
                "BUSINESS_CONFLICT",
                "分类下存在商品，无法删除"
            );
        }
        categoryMapper.deleteById(categoryId);
        
    }

}
