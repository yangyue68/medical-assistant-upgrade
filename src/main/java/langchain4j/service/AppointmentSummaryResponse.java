// 声明当前类所在的包，service 表示它服务于预约业务接口。
package langchain4j.service;

// Appointment 是预约记录实体。
import langchain4j.entity.Appointment;
// SensitiveDataMasker 用来脱敏身份证号和手机号。
import langchain4j.util.SensitiveDataMasker;
// LocalDate 表示预约日期。
import java.time.LocalDate;
// Data 会让 Lombok 生成 getter、setter、toString、equals、HashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义前端展示预约记录时使用的安全摘要响应。
public class AppointmentSummaryResponse {

    // 预约 ID，用户后续取消预约时需要使用。
    private Long appointmentId;

    // 患者姓名。
    private String username;

    // 脱敏后的身份证号。
    private String maskedIdCard;

    // 脱敏后的手机号。
    private String maskedPhone;

    // 预约科室。
    private String department;

    // 预约医生。
    private String doctorName;

    // 预约日期。
    private LocalDate date;

    // 预约时间段。
    private String timeSlot;

    // 预约状态码。
    private Integer status;

    // 根据预约实体创建前端安全摘要。
    public static AppointmentSummaryResponse from(Appointment appointment) {
        // 创建响应对象。
        AppointmentSummaryResponse response = new AppointmentSummaryResponse();
        // 设置预约 ID。
        response.setAppointmentId(appointment.getId());
        // 设置患者姓名。
        response.setUsername(appointment.getUsername());
        // 设置脱敏身份证号。
        response.setMaskedIdCard(SensitiveDataMasker.maskIdCard(appointment.getIdCard()));
        // 设置脱敏手机号。
        response.setMaskedPhone(SensitiveDataMasker.maskPhone(appointment.getPhone()));
        // 设置科室。
        response.setDepartment(appointment.getDepartment());
        // 设置医生。
        response.setDoctorName(appointment.getDoctorName());
        // 设置日期。
        response.setDate(appointment.getDate());
        // 设置时间段。
        response.setTimeSlot(appointment.getTimeSlot());
        // 设置状态。
        response.setStatus(appointment.getStatus());
        // 返回响应对象。
        return response;
    }
}
