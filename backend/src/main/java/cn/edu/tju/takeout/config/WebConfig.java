package cn.edu.tju.takeout.config;
import java.nio.file.Paths;import org.springframework.beans.factory.annotation.Value;import org.springframework.context.annotation.Configuration;import org.springframework.web.servlet.config.annotation.*;
@Configuration public class WebConfig implements WebMvcConfigurer{
 private final String dir;public WebConfig(@Value("${takeout.upload-dir:uploads}")String dir){this.dir=dir;}
 @Override public void addResourceHandlers(ResourceHandlerRegistry r){r.addResourceHandler("/uploads/**").addResourceLocations(Paths.get(dir).toAbsolutePath().normalize().toUri().toString());}
}
