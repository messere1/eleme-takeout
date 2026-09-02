package cn.edu.tju.takeout.product;

import cn.edu.tju.takeout.catalog.CategoryMapper;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;
import java.util.List;
import java.util.ArrayList;
import cn.edu.tju.takeout.catalog.Category;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ShopMapper shopMapper;
    public ProductService(
            ProductMapper productMapper, CategoryMapper categoryMapper, ShopMapper shopMapper) {
        // 仅保留依赖签名，等待功能开发人员实现。
        this.productMapper=productMapper;
        this.categoryMapper=categoryMapper;
        this.shopMapper=shopMapper;
    }

    public ProductView create(Long merchantId, Long shopId, ProductRequest request) {
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
        
        Category category=categoryMapper.findById(request.categoryId())
            .orElseThrow(()->
                new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "RESOURCE_NOT_FOUND",
                    "分类不存在"
                )
            );

        if(!category.getShopId().equals(shopId)){
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "BUSINESS_CONFLICT", 
                "分类不属于该店铺"
            );
        }

        Product product=Product.of(
            null, 
            shopId, 
            category.getId(), 
            request.name(), 
            request.description(), 
            request.price(), 
            request.stock(), 
            "OFF_SALE"
        );

        productMapper.insert(product);
        return ProductView.from(product);

    }

    public ProductView update(Long merchantId, Long productId, ProductRequest request) {
        Product product=productMapper.findById(productId).orElseThrow(()->
            new BusinessException(
                HttpStatus.NOT_FOUND, 
                "RESOURCE_NOT_FOUND",
                "商品不存在"
            )
        );

        Shop shop=shopMapper.findById(product.getShopId()).orElseThrow(()->
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
        
        Category category=categoryMapper.findById(request.categoryId())
            .orElseThrow(()->
                new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "RESOURCE_NOT_FOUND",
                    "分类不存在"
                )
            );

        if(!category.getShopId().equals(product.getShopId())){
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "BUSINESS_CONFLICT", 
                "分类不属于该店铺"
            );
        }        
        
        product.update(request);

        productMapper.update(product);
        return ProductView.from(product);
    }

    public ProductView changeStatus(Long merchantId, Long productId, ProductStatusRequest request) {
        Product product=productMapper.findById(productId).orElseThrow(()->
            new BusinessException(
                HttpStatus.NOT_FOUND, 
                "RESOURCE_NOT_FOUND",
                "商品不存在"
            )
        );

        Shop shop=shopMapper.findById(product.getShopId()).orElseThrow(()->
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
        
        product.changeStatus(request.status());
        productMapper.updateStatus(product);
        return ProductView.from(product);
    }

    public void delete(Long merchantId, Long productId) {
        Product product=productMapper.findById(productId).orElseThrow(()->
            new BusinessException(
                HttpStatus.NOT_FOUND, 
                "RESOURCE_NOT_FOUND",
                "商品不存在"
            )
        );

        Shop shop=shopMapper.findById(product.getShopId()).orElseThrow(()->
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

        productMapper.logicalDelete(productId);
    }

    public List<ProductView> listVisible(Long shopId, Long categoryId) {
        List<Product> products=productMapper.findVisibleByShopAndCategory(shopId,categoryId);
        List<ProductView> productViews=new ArrayList<>();
        for(Product p:products){
            productViews.add(ProductView.from(p));
        }
        return productViews;
    }

    public ProductView updateStock(Long merchantId, Long productId, StockRequest request) {
        Product product=productMapper.findById(productId).orElseThrow(()->
            new BusinessException(
                HttpStatus.NOT_FOUND, 
                "RESOURCE_NOT_FOUND",
                "商品不存在"
            )
        );

        Shop shop=shopMapper.findById(product.getShopId()).orElseThrow(()->
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
        product.updateStock(request.stock());
        productMapper.updateStock(product);

        return ProductView.from(product);
    }

    public void decreaseStock(Long productId, Integer quantity) {
        productMapper.findById(productId).orElseThrow(()->
            new BusinessException(
                HttpStatus.NOT_FOUND, 
                "RESOURCE_NOT_FOUND",
                "商品不存在"
            )
        );

        int affected=productMapper.decreaseStockIfAvailable(productId, quantity);
        if(affected<=0){
            throw new BusinessException(
                HttpStatus.CONFLICT, 
                "INSUFFICIENT_STOCK", 
                "商品库存不足"
            );
        }
    }

    private UnsupportedOperationException pending() {
        return new UnsupportedOperationException("待功能开发：商品业务尚未实现");
    }
}
