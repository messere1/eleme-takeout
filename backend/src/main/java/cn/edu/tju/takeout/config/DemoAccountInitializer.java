package cn.edu.tju.takeout.config;

import cn.edu.tju.takeout.admin.AdminMapper;
import cn.edu.tju.takeout.rider.RiderMapper;
import cn.edu.tju.takeout.catalog.BusinessCategoryMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoAccountInitializer implements CommandLineRunner {
    private final AdminMapper admins; private final RiderMapper riders; private final PasswordEncoder encoder; private final BusinessCategoryMapper categories;
    public DemoAccountInitializer(AdminMapper admins, RiderMapper riders, PasswordEncoder encoder, BusinessCategoryMapper categories) {
        this.admins = admins; this.riders = riders; this.encoder = encoder; this.categories=categories;
    }
    @Override public void run(String... args) {
        if (admins.countAll() == 0) admins.insert("admin", encoder.encode("Admin123"));
        if (riders.countAll() == 0) riders.insert("rider", "13900000000", encoder.encode("Rider123"));
        for(String name:new String[]{"快餐便当","奶茶饮品","小吃炸物","汉堡披萨","日韩料理","烧烤夜宵","甜品烘焙","健康轻食"})
            if(categories.findByName(name).isEmpty())categories.insertDefault(name);
    }
}
