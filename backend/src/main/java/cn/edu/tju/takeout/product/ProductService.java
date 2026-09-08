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

        if(request.stock()==null || request.stock()<0){
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR", 
                "库存不能小于0"
            );
        }

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
        if(quantity==null || quantity<=0){
            throw new BusinessException(
                HttpStatus.BAD_REQUEST, 
                "VALIDATION_ERROR", 
                "扣减数量必须大于0"
            );
        }

        int affected=productMapper.decreaseStockIfAvailable(productId, quantity);
        if(affected<=0){
            throw new BusinessException(
                HttpStatus.CONFLICT, 
                "BUSINESS_CONFLICT", 
                "商品库存不足"
            );
        }
    }

    public ProductPage listVisible(
        Long categoryId,
        Integer page,
        Integer size
    ){
        int currentpage=page==null?1:page;
        int pagesize=size==null?20:size;
        if(currentpage<1){
            throw new BusinessException(
                HttpStatus.BAD_REQUEST, 
                "VALIDATION_ERROR", 
                "页码必须大于等于1"
            );
        }

        if(pagesize<1||pagesize>100){
            throw new BusinessException(
                HttpStatus.BAD_REQUEST, 
                "VALIDATION_ERROR",
                "每页数量必须在1到100之间"
            );
        }

        categoryMapper.findById(categoryId)
            .orElseThrow(()->new BusinessException(
                HttpStatus.NOT_FOUND, 
                "RESOURCE_NOT_FOUND", 
                "分类不存在"
                )
            );

        int offset = (currentpage - 1) * pagesize;


        List<ProductView> items = productMapper
            .findVisiblePageByCategoryId(categoryId,pagesize, offset)
            .stream()
            .map(ProductView::from)
            .toList();

        long total = productMapper.countVisibleByCategoryId(categoryId);
        int totalPages = (int) ((total + pagesize - 1) / pagesize);

        return new ProductPage(
            items,
            currentpage,
            pagesize,
            total,
            totalPages
        );
    }

    public ProductView getVisibleProduct(Long productId) {
        Product product = productMapper.findVisibleById(productId)
            .orElseThrow(() -> new BusinessException(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "商品不存在"
            ));
        return ProductView.from(product);   
    }

    public ProductView updatePrice(
            Long merchantId, Long productId, ProductPriceRequest request) {
        Product product = productMapper.findById(productId)
            .orElseThrow(() -> new BusinessException(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "商品不存在"
            ));

        Shop shop = shopMapper.findById(product.getShopId())
            .orElseThrow(() -> new BusinessException(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "店铺不存在"
            ));

        if (!shop.getMerchantId().equals(merchantId)) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "无权修改该商品"
            );
        }

        product.updatePrice(request.price());
        productMapper.updatePrice(product);
        return ProductView.from(product);
    }

    public List<ProductView> listForMerchant(Long merchantId) {
        Shop shop = shopMapper.findByMerchantId(merchantId)
            .orElseThrow(() -> new BusinessException(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "当前商家尚未创建店铺"
            ));

        return productMapper.findAllByShopId(shop.getId())
            .stream()
            .map(ProductView::from)
            .toList();
    }
}
