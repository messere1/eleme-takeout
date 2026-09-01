package cn.edu.tju.takeout.cart;

public record CartItemView(Long id, Long productId, Integer quantity) {
    public static CartItemView from(CartItem item) {
        return new CartItemView(item.getId(), item.getProductId(), item.getQuantity());
    }
}
