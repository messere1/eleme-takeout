package cn.edu.tju.takeout.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DemoDataContractTest {
    private static final Pattern BCRYPT = Pattern.compile("\\$2[aby]\\$10\\$[./A-Za-z0-9]{53}");

    @Test
    void demoSqlProvidesUsableCustomerAndMerchantAccounts() throws IOException {
        Path sqlPath = Path.of(System.getProperty("user.dir"), "..", "database", "data.sql").normalize();
        String sql = Files.readString(sqlPath);

        assertTrue(sql.contains("INSERT INTO users"), "测试数据必须提供顾客账号");
        assertTrue(sql.contains("INSERT INTO merchants"), "测试数据必须提供商家账号");
        assertFalse(sql.contains("稍后替换"), "测试数据不能保留密码哈希占位符");

        Matcher matcher = BCRYPT.matcher(sql);
        assertTrue(matcher.find(), "测试数据必须包含 cost=10 的 BCrypt 哈希");
        assertTrue(new BCryptPasswordEncoder().matches("abc123", matcher.group()),
                "文档约定的演示密码 abc123 必须能通过哈希校验");
    }
}
