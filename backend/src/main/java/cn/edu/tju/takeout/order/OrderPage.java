package cn.edu.tju.takeout.order;

import java.util.List;

public record OrderPage(
        List<OrderSummaryView> items, int page, int size, long total, int totalPages) {}
