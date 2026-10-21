// 声明当前类所在的包，service 表示它属于预约业务层。
package langchain4j.service;

// Appointment 是取消成功或失败时关联的预约记录实体。
import langchain4j.entity.Appointment;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义取消预约结果对象，用统一结构表达取消是否成功、失败原因和关联预约。
public class AppointmentCancelResult {

    // 标记取消预约操作是否成功。
    private boolean success;

    // 保存机器可判断的业务错误码。
    private AppointmentErrorCode errorCode;

    // 保存用户可读的业务提示。
    private String message;

    // 保存被取消或尝试取消的预约记录。
    private Appointment appointment;

    // 创建取消预约成功结果。
    public static AppointmentCancelResult success(Appointment appointment) {
        // 创建结果对象。
        AppointmentCancelResult result = new AppointmentCancelResult();
        // 标记操作成功。
        result.setSuccess(true);
        // 成功时错误码固定为 SUCCESS。
        result.setErrorCode(AppointmentErrorCode.SUCCESS);
        // 设置用户可读成功提示。
        result.setMessage("取消预约成功");
        // 保存被取消的预约记录。
        result.setAppointment(appointment);
        // 返回构造好的结果对象。
        return result;
    }

    // 创建取消预约失败结果。
    public static AppointmentCancelResult fail(AppointmentErrorCode errorCode, String message) {
        // 委托带 appointment 参数的工厂方法，失败时可以不携带预约记录。
        return fail(errorCode, message, null);
    }

    // 创建取消预约失败结果，并可附带预约记录。
    public static AppointmentCancelResult fail(AppointmentErrorCode errorCode, String message, Appointment appointment) {
        // 创建结果对象。
        AppointmentCancelResult result = new AppointmentCancelResult();
        // 标记操作失败。
        result.setSuccess(false);
        // 设置机器可判断的错误码。
        result.setErrorCode(errorCode);
        // 设置用户可读失败原因。
        result.setMessage(message);
        // 保存关联预约记录，可能为空。
        result.setAppointment(appointment);
        // 返回构造好的结果对象。
        return result;
    }
}
