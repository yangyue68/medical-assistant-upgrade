// 声明当前类所在的包，service 表示它服务于预约业务接口。
package langchain4j.service;

// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义前端取消预约后的结构化响应。
public class AppointmentCancelResponse {

    // 标记取消是否成功。
    private boolean success;

    // 机器可判断的业务错误码。
    private AppointmentErrorCode errorCode;

    // 用户可读提示。
    private String message;

    // 被取消或尝试取消的预约摘要。
    private AppointmentSummaryResponse appointment;

    // 根据业务取消结果创建前端响应。
    public static AppointmentCancelResponse from(AppointmentCancelResult result) {
        // 创建响应对象。
        AppointmentCancelResponse response = new AppointmentCancelResponse();
        // 设置成功标记。
        response.setSuccess(result.isSuccess());
        // 设置错误码。
        response.setErrorCode(result.getErrorCode());
        // 设置用户提示。
        response.setMessage(result.getMessage());
        // 如果业务结果里带预约记录，就转成安全摘要。
        if (result.getAppointment() != null) {
            // 设置预约摘要。
            response.setAppointment(AppointmentSummaryResponse.from(result.getAppointment()));
        }
        // 返回响应对象。
        return response;
    }
}
