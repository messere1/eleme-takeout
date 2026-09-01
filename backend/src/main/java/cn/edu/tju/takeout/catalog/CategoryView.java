package cn.edu.tju.takeout.catalog;

public record CategoryView(Long id, Long shopId, String name, Integer sort) {
    public static CategoryView from(Category category) {
        return new CategoryView(
                category.getId(), category.getShopId(), category.getName(), category.getSort());
    }
}
