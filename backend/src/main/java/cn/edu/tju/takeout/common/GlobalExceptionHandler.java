package cn.edu.tju.takeout.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ApiResponse<Void> body = ApiResponse.failure(exception.code(), exception.getMessage(), null);
        return ResponseEntity.status(exception.status()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationException(
            MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        Map<String, Object> data = Map.of("fieldErrors", fieldErrors);
        ApiResponse<Map<String, Object>> body =
                ApiResponse.failure("VALIDATION_ERROR", "请求参数校验失败", data);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(
                "VALIDATION_ERROR", "请求内容格式不正确", null));
    }

    // 路由不存在 / 接口已下线时不能落进兜底的 500。契约 §1 的「404 不存在」，
    // 以及 EX-030「未知路由 → 404/400/500」。删掉的接口（如 POST /orders/{id}/complete）
    // 应当明确报 404，而不是让调用方以为服务端崩了。
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ApiResponse<Void>> handleNoResourceFound(Exception exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(
                "RESOURCE_NOT_FOUND", "请求的接口不存在", null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            MaxUploadSizeExceededException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(
                "VALIDATION_ERROR", "图片不能超过 5MiB", null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.failure(
                "METHOD_NOT_ALLOWED", "请求方法不被支持", null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        // 兜底分支必须留下服务端痕迹，否则 500 只能靠客户端拿着 traceId 猜
        LOGGER.error("未处理异常：{}", exception.toString(), exception);
        ApiResponse<Void> body =
                ApiResponse.failure("INTERNAL_ERROR", "服务器内部错误，请稍后重试", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
