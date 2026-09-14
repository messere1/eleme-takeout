package cn.edu.tju.takeout.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.shop.ShopMapper;
import cn.edu.tju.takeout.user.User;
import cn.edu.tju.takeout.user.UserMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

class ImageUploadWriteFailureTest {
    @TempDir Path uploadDir;

    @Test
    void partialWriteFailureDoesNotLeaveEmptyImageOrUpdateDatabase() throws Exception {
        UserMapper users = mock(UserMapper.class);
        User user = User.registered("user001", "13800138000", "hash");
        user.setId(7L);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        ImageUploadService service = new ImageUploadService(
                uploadDir.toString(), users, mock(ShopMapper.class), mock(ProductMapper.class));
        ImageUploadController controller = new ImageUploadController(service);

        byte[] png = {
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", png);

        try (MockedStatic<Files> files = mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.copy(
                            any(InputStream.class),
                            any(Path.class),
                            eq(StandardCopyOption.REPLACE_EXISTING)))
                    .thenAnswer(invocation -> {
                        Files.createFile(invocation.getArgument(1));
                        throw new IOException("No space left on device");
                    });
            assertThatThrownBy(() -> controller.upload(
                    new UserPrincipal(7L, "CUSTOMER"), "USER_AVATAR", 7L, file))
                    .isInstanceOf(BusinessException.class)
                    .extracting(error -> ((BusinessException) error).code())
                    .isEqualTo("UPLOAD_FAILED");
        }

        try (var entries = Files.list(uploadDir)) {
            assertThat(entries.toList()).isEmpty();
        }
        verify(users, never()).updateAvatar(eq(7L), any());
    }
}
