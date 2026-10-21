// 声明当前类位于 common 包，表示它是多个接口都可以复用的通用返回结构。
package langchain4j.common;

// LocalDateTime 用来记录响应生成时间，方便排查接口调用链路。
import java.time.LocalDateTime;

// 定义统一 API 响应对象，T 表示 data 字段可以承载任意类型的数据。
public class ApiResponse<T> {

    // code 表示业务响应码；0 代表成功，非 0 代表失败或异常。
    private final int code;

    // message 表示给前端或调用方看的简短响应说明。
    private final String message;

    // data 表示接口真正返回的业务数据。
    private final T data;

    // timestamp 表示后端生成这个响应对象的时间。
    private final LocalDateTime timestamp;

    // 私有构造方法强制外部通过 success/fail 这类静态工厂方法创建对象。
    //后面的四个静态工厂方法都在内部使用new关键字创建对象，也就是调用本构造方法，从而自动填充timestamp字段
    private ApiResponse(int code, String message, T data) {
        // 保存业务响应码。
        this.code = code;
        // 保存响应说明。
        this.message = message;
        // 保存业务数据。
        this.data = data;
        // 每次创建响应对象时自动记录当前时间。
        this.timestamp = LocalDateTime.now();
    }

    // 创建成功响应，但不携带业务数据。
    public static <T> ApiResponse<T> success() {
        // 成功响应统一使用 code=0 和 message=success。
        return new ApiResponse<>(0, "success", null);
    }

    // 创建成功响应，并携带业务数据。
    public static <T> ApiResponse<T> success(T data) {
        // 成功响应统一使用 code=0，并把调用方传入的数据放进 data。
        return new ApiResponse<>(0, "success", data);
    }

    // 创建失败响应，不携带业务数据。
    public static <T> ApiResponse<T> fail(int code, String message) {
        // 失败响应使用调用方指定的错误码和错误说明。
        return new ApiResponse<>(code, message, null);
    }

    // 创建失败响应，并携带业务数据。
    public static <T> ApiResponse<T> fail(int code, String message, T data) {
        // 失败响应使用调用方指定的错误码、错误说明和业务数据。
        return new ApiResponse<>(code, message, data);
    }

    // 返回业务响应码，供 Jackson 序列化 JSON 时读取。
    public int getCode() {
        // 返回当前对象保存的 code。
        return code;
    }

    // 返回响应说明，供 Jackson 序列化 JSON 时读取。
    public String getMessage() {
        // 返回当前对象保存的 message。
        return message;
    }

    // 返回业务数据，供 Jackson 序列化 JSON 时读取。
    public T getData() {
        // 返回当前对象保存的 data。
        return data;
    }

    // 返回响应生成时间，供 Jackson 序列化 JSON 时读取。
    public LocalDateTime getTimestamp() {
        // 返回当前对象保存的 timestamp。
        return timestamp;
    }
}
