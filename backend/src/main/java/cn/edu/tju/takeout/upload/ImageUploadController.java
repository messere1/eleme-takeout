package cn.edu.tju.takeout.upload;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.*;
import cn.edu.tju.takeout.product.*;
import cn.edu.tju.takeout.shop.*;
import cn.edu.tju.takeout.user.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/images")
public class ImageUploadController {
    private static final long MAX = 5L * 1024 * 1024;
    private final Path root; private final UserMapper users; private final ShopMapper shops; private final ProductMapper products;
    public ImageUploadController(@Value("${takeout.upload-dir:uploads}") String dir, UserMapper users, ShopMapper shops, ProductMapper products) {
        this.root=Paths.get(dir).toAbsolutePath().normalize();this.users=users;this.shops=shops;this.products=products;
    }
    @PostMapping public ApiResponse<Map<String,String>> upload(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam String targetType,@RequestParam Long targetId,@RequestPart("file") MultipartFile file) throws IOException {
        byte[] bytes=file.getBytes();String ext=extension(bytes);
        if(bytes.length==0||bytes.length>MAX||ext==null)throw bad("仅支持不超过5MiB的JPEG、PNG或WebP图片");
        authorizeAndCheck(p,targetType,targetId);
        Files.createDirectories(root);String name=UUID.randomUUID()+ext;Files.write(root.resolve(name),bytes,StandardOpenOption.CREATE_NEW);
        String url="/uploads/"+name;int changed=switch(targetType){case "USER_AVATAR"->users.updateAvatar(targetId,url);case "SHOP_IMAGE"->shops.updateImage(targetId,url);case "SHOP_COVER"->shops.updateCover(targetId,url);case "PRODUCT_IMAGE"->products.updateImage(targetId,url);default->0;};
        if(changed==0){Files.deleteIfExists(root.resolve(name));throw bad("目标类型或目标资源不合法");}
        return ApiResponse.success(Map.of("url",url));
    }
    private void authorizeAndCheck(UserPrincipal p,String type,Long id){
        if("ADMIN".equals(p.role()))return;
        if("USER_AVATAR".equals(type)&&"CUSTOMER".equals(p.role())&&p.userId().equals(id))return;
        if(("SHOP_IMAGE".equals(type)||"SHOP_COVER".equals(type))&&"MERCHANT".equals(p.role())&&shops.findById(id).map(s->p.userId().equals(s.getMerchantId())).orElse(false))return;
        if("PRODUCT_IMAGE".equals(type)&&"MERCHANT".equals(p.role())&&products.findById(id).flatMap(x->shops.findById(x.getShopId())).map(s->p.userId().equals(s.getMerchantId())).orElse(false))return;
        throw new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","无权上传该资源图片");
    }
    private String extension(byte[] b){if(b.length>=8&&b[0]==(byte)0x89&&b[1]==0x50&&b[2]==0x4e&&b[3]==0x47)return ".png";if(b.length>=3&&b[0]==(byte)0xff&&b[1]==(byte)0xd8&&b[2]==(byte)0xff)return ".jpg";if(b.length>=12&&new String(b,0,4).equals("RIFF")&&new String(b,8,4).equals("WEBP"))return ".webp";return null;}
    private BusinessException bad(String m){return new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR",m);}
}
