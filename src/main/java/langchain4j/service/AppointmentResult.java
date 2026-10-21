// 声明当前类所在的包，service 表示它属于预约业务层。
package langchain4j.service;

// Appointment 是预约成功后返回的预约记录实体。
import langchain4j.entity.Appointment;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义预约业务结果对象，用统一结构表达成功、失败原因和预约记录。
public class AppointmentResult {

    // 标记预约操作是否成功。
    private boolean success;

    // 保存机器可判断的业务错误码。
    private AppointmentErrorCode errorCode;

    // 保存用户可读的业务提示。
    private String message;

    // 保存预约成功后创建出的预约记录。
    private Appointment appointment;

    // 创建预约成功结果。
    public static AppointmentResult success(Appointment appointment) {
        // 创建结果对象。
        AppointmentResult result = new AppointmentResult();
        // 标记当前操作成功。
        result.setSuccess(true);
        // 成功时错误码固定为 SUCCESS。
        result.setErrorCode(AppointmentErrorCode.SUCCESS);
        // 设置用户可读成功提示。
        result.setMessage("预约成功");
        // 设置预约记录。
        result.setAppointment(appointment);
        // 返回构造好的结果对象。
        return result;
    }

    // 创建预约失败结果。
    public static AppointmentResult fail(AppointmentErrorCode errorCode, String message) {
        // 创建结果对象。
        AppointmentResult result = new AppointmentResult();
        // 标记当前操作失败。
        result.setSuccess(false);
        // 设置机器可判断的错误码。
        result.setErrorCode(errorCode);
        // 设置用户可读失败原因。
        result.setMessage(message);
        // 返回构造好的结果对象。
        return result;
    }

    // 保留旧的失败工厂方法，兼容暂时还没迁移的调用点。
    public static AppointmentResult fail(String message) {
        // 默认归类为非法请求，后续调用点会逐步迁移到明确错误码。
        return fail(AppointmentErrorCode.INVALID_REQUEST, message);
    }
}
