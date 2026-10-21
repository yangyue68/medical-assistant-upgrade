package langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import langchain4j.entity.Appointment;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService extends IService<Appointment> {

    /**
     * 根据条件查询单条预约
     */
    Appointment getOne(Appointment appointment);

    /**
     * 查询患者的所有有效预约
     */
    List<Appointment> findValidAppointmentsByIdCard(String idCard);

    /**
     * 查询患者的所有有效预约（按姓名和身份证）
     */
    List<Appointment> findValidAppointmentsByNameAndIdCard(String username, String idCard);

    /**
     * 执行预约（事务）- 工业级入口
     */
    AppointmentResult bookAppointment(AppointmentBookingRequest request);

    default AppointmentResult bookAppointment(Long userId, AppointmentBookingRequest request) {
        return bookAppointment(request);
    }

    /**
     * 执行预约（事务）- 增加 phone 参数
     */
    AppointmentResult bookAppointment(String username, String idCard, String phone,
                                      String department, String doctorName,
                                      LocalDate date, String timeSlot);

    /**
     * 取消预约（事务）- 工业级入口
     */
    AppointmentCancelResult cancelAppointmentWithResult(Long appointmentId, String idCard);

    default AppointmentCancelResult cancelAppointmentWithResult(Long userId, Long appointmentId, String idCard) {
        return cancelAppointmentWithResult(appointmentId, idCard);
    }

    default List<Appointment> findValidAppointmentsByNameAndIdCard(Long userId, String username, String idCard) {
        return findValidAppointmentsByNameAndIdCard(username, idCard);
    }

    /**
     * 取消预约（事务）- 兼容旧工具层
     */
    boolean cancelAppointment(Long appointmentId, String idCard);

    /**
     * 检查是否存在重复预约
     */
    boolean hasDuplicateAppointment(String idCard, String department, LocalDate date, String timeSlot);

    default boolean hasDuplicateAppointment(Long userId, String idCard, String department,
                                            LocalDate date, String timeSlot) {
        return hasDuplicateAppointment(idCard, department, date, timeSlot);
    }
}
