package cn.edu.tju.takeout.catalog;

public class Category {
    private Long id;
    private Long shopId;
    private String name;
    private Integer sort;

    public static Category of(Long id, Long shopId, String name, Integer sort) {
        Category category = new Category();
        category.id = id;
        category.shopId = shopId;
        category.name = name;
        category.sort = sort;
        return category;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getShopId() { return shopId; }
    public String getName() { return name; }
    public Integer getSort() { return sort; }

    public void update(String name, Integer sort) {
        this.name = name;
        this.sort = sort;
    }
}
