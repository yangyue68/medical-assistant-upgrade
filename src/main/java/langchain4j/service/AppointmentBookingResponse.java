// 声明当前类所在的包，service 表示它服务于预约业务接口。
package langchain4j.service;

// Appointment 是预约记录实体。
import langchain4j.entity.Appointment;
// SensitiveDataMasker 用来脱敏身份证号和手机号。
import langchain4j.util.SensitiveDataMasker;
// LocalDate 表示不带时间的预约日期。
import java.time.LocalDate;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义前端预约表单提交后的响应数据。
public class AppointmentBookingResponse {

    // 标记预约是否成功。
    private boolean success;

    // 保存机器可判断的错误码。
    private AppointmentErrorCode errorCode;

    // 保存用户可读提示。
    private String message;

    // 保存预约 ID，成功后前端可用于取消预约或展示详情。
    private Long appointmentId;

    // 保存患者姓名。
    private String username;

    // 保存脱敏后的身份证号。
    private String maskedIdCard;

    // 保存脱敏后的手机号。
    private String maskedPhone;

    // 保存预约科室。
    private String department;

    // 保存预约医生。
    private String doctorName;

    // 保存预约日期。
    private LocalDate date;

    // 保存预约时间段。
    private String timeSlot;

    // 根据 AppointmentResult 创建前端响应对象。
    public static AppointmentBookingResponse from(AppointmentResult result) {
        // 创建响应对象。
        AppointmentBookingResponse response = new AppointmentBookingResponse();
        // 设置成功标记。
        response.setSuccess(result.isSuccess());
        // 设置错误码。
        response.setErrorCode(result.getErrorCode());
        // 设置用户提示。
        response.setMessage(result.getMessage());
        // 如果结果中包含预约实体，就继续填充预约摘要。
        if (result.getAppointment() != null) {
            // 把预约实体信息复制到响应对象。
            response.fillAppointment(result.getAppointment());
        }
        // 返回构造完成的响应对象。
        return response;
    }

    // 从预约实体中填充前端展示字段。
    private void fillAppointment(Appointment appointment) {
        // 设置预约 ID。
        this.appointmentId = appointment.getId();
        // 设置患者姓名。
        this.username = appointment.getUsername();
        // 设置脱敏身份证号。
        this.maskedIdCard = SensitiveDataMasker.maskIdCard(appointment.getIdCard());
        // 设置脱敏手机号。
        this.maskedPhone = SensitiveDataMasker.maskPhone(appointment.getPhone());
        // 设置预约科室。
        this.department = appointment.getDepartment();
        // 设置预约医生。
        this.doctorName = appointment.getDoctorName();
        // 设置预约日期。
        this.date = appointment.getDate();
        // 设置预约时间段。
        this.timeSlot = appointment.getTimeSlot();
    }
}
