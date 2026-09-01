package cn.edu.tju.takeout.auth;

import cn.edu.tju.takeout.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;

public final class SecurityErrorWriter {
    private SecurityErrorWriter() {}

    public static void write(
            HttpServletResponse response, ObjectMapper objectMapper,
            int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(code, message, null));
    }
}
