package cn.edu.tju.takeout.upload;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.product.*;
import cn.edu.tju.takeout.shop.*;
import cn.edu.tju.takeout.user.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageUploadService {
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("USER_AVATAR", "SHOP_IMAGE", "SHOP_COVER", "PRODUCT_IMAGE");
    private final Path root;
    private final UserMapper users;
    private final ShopMapper shops;
    private final ProductMapper products;

    public ImageUploadService(@Value("${takeout.upload-dir:uploads}") String directory,
            UserMapper users, ShopMapper shops, ProductMapper products) {
        root = Paths.get(directory).toAbsolutePath().normalize();
        this.users = users; this.shops = shops; this.products = products;
    }

    @Transactional
    public String upload(UserPrincipal principal, String type, Long id, MultipartFile file) {
        if (!TYPES.contains(type) || id == null || id <= 0) throw bad("目标类型或目标资源不合法");
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE) throw bad("图片不能为空且不能超过5MiB");
        String oldUrl = authorizeAndGetOldUrl(principal, type, id);
        Path newFile = null;
        try {
            String extension = detectExtension(file);
            Files.createDirectories(root);
            newFile = root.resolve(UUID.randomUUID() + extension).normalize();
            if (!root.equals(newFile.getParent())) throw bad("文件路径不合法");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, newFile, StandardCopyOption.REPLACE_EXISTING);
            }
            String url = "/uploads/" + newFile.getFileName();
            if (updateTarget(type, id, url) == 0) throw notFound();
            deleteOldAfterCommit(oldUrl, newFile);
            return url;
        } catch (IOException | RuntimeException exception) {
            deleteQuietly(newFile);
            if (exception instanceof BusinessException businessException) throw businessException;
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD_FAILED", "图片保存失败");
        }
    }

    private String authorizeAndGetOldUrl(UserPrincipal principal, String type, Long id) {
        if ("USER_AVATAR".equals(type)) {
            User user = users.findById(id).orElseThrow(this::notFound);
            require("ADMIN".equals(principal.role()) || ("CUSTOMER".equals(principal.role()) && principal.userId().equals(id)));
            return user.getAvatarUrl();
        }
        if ("SHOP_IMAGE".equals(type) || "SHOP_COVER".equals(type)) {
            Shop shop = shops.findById(id).orElseThrow(this::notFound);
            require("ADMIN".equals(principal.role()) || ("MERCHANT".equals(principal.role()) && principal.userId().equals(shop.getMerchantId())));
            return "SHOP_IMAGE".equals(type) ? shop.getImageUrl() : shop.getCoverImageUrl();
        }
        Product product = products.findById(id).orElseThrow(this::notFound);
        Shop shop = shops.findById(product.getShopId()).orElseThrow(this::notFound);
        require("ADMIN".equals(principal.role()) || ("MERCHANT".equals(principal.role()) && principal.userId().equals(shop.getMerchantId())));
        return product.getImageUrl();
    }

    private int updateTarget(String type, Long id, String url) {
        return switch (type) {
            case "USER_AVATAR" -> users.updateAvatar(id, url);
            case "SHOP_IMAGE" -> shops.updateImage(id, url);
            case "SHOP_COVER" -> shops.updateCover(id, url);
            case "PRODUCT_IMAGE" -> products.updateImage(id, url);
            default -> 0;
        };
    }

    private String detectExtension(MultipartFile file) throws IOException {
        byte[] header = new byte[12]; int read;
        try (InputStream input = file.getInputStream()) { read = input.read(header); }
        if (read >= 8 && header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4e && header[3] == 0x47
                && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a) return ".png";
        if (read >= 3 && header[0] == (byte) 0xff && header[1] == (byte) 0xd8 && header[2] == (byte) 0xff) return ".jpg";
        if (read >= 12 && "RIFF".equals(new String(header, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(header, 8, 4, StandardCharsets.US_ASCII))) return ".webp";
        throw bad("仅支持真实的JPEG、PNG或WebP图片");
    }

    private void deleteOldAfterCommit(String oldUrl, Path newFile) {
        if (oldUrl == null || !oldUrl.startsWith("/uploads/")) return;
        Path oldFile = root.resolve(oldUrl.substring(9)).normalize();
        if (!root.equals(oldFile.getParent()) || oldFile.equals(newFile)) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { deleteQuietly(oldFile); }
            });
        } else deleteQuietly(oldFile);
    }

    private void require(boolean allowed) {
        if (!allowed) throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "无权上传该资源图片");
    }
    private BusinessException notFound() { return new BusinessException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "目标资源不存在"); }
    private BusinessException bad(String message) { return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message); }
    private static void deleteQuietly(Path path) { if (path != null) try { Files.deleteIfExists(path); } catch (IOException ignored) { } }
}
