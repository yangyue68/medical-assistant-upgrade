// 声明当前接口所在的包，service 表示它属于预约业务边界。
package langchain4j.service;

// Appointment 表示预约记录实体。
import langchain4j.entity.Appointment;
// List 表示一组预约记录。
import java.util.List;

// 定义预约能力提供者接口，为虚拟示范医院、未来真实医院 API、MCP 工具预留统一边界。
public interface AppointmentProvider {

    // 提交预约挂号请求。
    AppointmentResult bookAppointment(AppointmentBookingRequest request);

    default AppointmentResult bookAppointment(Long userId, AppointmentBookingRequest request) {
        return bookAppointment(request);
    }

    // 取消指定预约。
    AppointmentCancelResult cancelAppointment(Long appointmentId, String idCard);

    default AppointmentCancelResult cancelAppointment(Long userId, Long appointmentId, String idCard) {
        return cancelAppointment(appointmentId, idCard);
    }

    // 查询患者当前所有有效预约。
    List<Appointment> findValidAppointments(String username, String idCard);

    default List<Appointment> findValidAppointments(Long userId, String username, String idCard) {
        return findValidAppointments(username, idCard);
    }
}
