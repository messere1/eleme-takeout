package cn.edu.tju.takeout.cart;

public class CartItem {
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;

    public static CartItem of(Long id, Long userId, Long productId, Integer quantity) {
        CartItem item = new CartItem();
        item.id = id;
        item.userId = userId;
        item.productId = productId;
        item.quantity = quantity;
        return item;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public Long getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public void increase(Integer amount) { quantity += amount; }
    public void changeQuantity(Integer quantity) { this.quantity = quantity; }
}
