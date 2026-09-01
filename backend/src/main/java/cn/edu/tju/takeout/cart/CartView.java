package cn.edu.tju.takeout.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartView(List<CartLineView> items, BigDecimal totalAmount) {}
