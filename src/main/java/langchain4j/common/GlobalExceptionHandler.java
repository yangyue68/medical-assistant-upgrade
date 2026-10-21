// 声明当前类位于 common 包，表示它处理的是全局通用能力。
package langchain4j.common;

// SensitiveDataMasker 用来在记录异常消息前做脱敏。
import langchain4j.util.SensitiveDataMasker;
import langchain4j.multimodal.extraction.MedicalOcrException;
// Logger 是 SLF4J 的日志接口。
import org.slf4j.Logger;
// LoggerFactory 用于创建当前类的日志对象。
import org.slf4j.LoggerFactory;
// HttpStatus 表示 HTTP 状态码。
import org.springframework.http.HttpStatus;
// ResponseEntity 可以同时控制响应体和 HTTP 状态码。
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
// ExceptionHandler 标记某个方法用于处理指定类型的异常。
import org.springframework.web.bind.annotation.ExceptionHandler;
// RestControllerAdvice 表示这是面向所有 RestController 的全局增强类。
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 标记当前类为全局 REST 异常处理器。
@RestControllerAdvice
// 定义全局异常处理类，避免非流式接口把 Java 异常堆栈直接暴露给前端。
public class GlobalExceptionHandler {

    // 创建当前异常处理器的日志对象。
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MedicalOcrException.class)
    public ResponseEntity<ApiResponse<Void>> handleMedicalOcrException(MedicalOcrException exception) {
        log.warn("Medical OCR request failed: {}", SensitiveDataMasker.mask(exception.getMessage()));
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail(503, SensitiveDataMasker.mask(exception.getMessage())));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception
    ) {
        log.warn("Uploaded medical file exceeds configured multipart size limit.");
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.fail(413, "文件超过允许的最大大小。"));
    }

    // 捕获 IllegalArgumentException，通常代表客户端参数不合法。
    @ExceptionHandler(IllegalArgumentException.class)
    // 返回统一 ApiResponse，同时把 HTTP 状态码设置成 400。
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
        // 把异常消息脱敏后写入 warn 日志，便于排查但不泄露敏感信息。
        log.warn("Bad request: {}", SensitiveDataMasker.mask(exception.getMessage()));
        // 构造 400 响应，业务 code 使用 400，message 使用脱敏后的异常说明。
        return ResponseEntity
                // 设置 HTTP 状态码为 400 Bad Request。
                .status(HttpStatus.BAD_REQUEST)
                // 设置统一响应体。
                .body(ApiResponse.fail(400, SensitiveDataMasker.mask(exception.getMessage())));
    }

    // 捕获所有未被上面更具体方法处理的异常。
    @ExceptionHandler(Exception.class)
    // 返回统一 ApiResponse，同时把 HTTP 状态码设置成 500。
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        // 把完整异常写入 error 日志，工程排查需要堆栈，但日志内容仍要注意避免主动拼接敏感数据。
        log.error("Unhandled server exception.", exception);
        // 构造 500 响应，给前端的 message 使用通用文案，避免暴露服务端细节。
        return ResponseEntity
                // 设置 HTTP 状态码为 500 Internal Server Error。
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                // 设置统一响应体。
                .body(ApiResponse.fail(500, "系统繁忙，请稍后重试。"));
    }
}
