// 声明当前类所在的包，impl 表示它是业务接口的实现类。
package langchain4j.service.impl;

// Appointment 表示预约记录实体。
import langchain4j.entity.Appointment;
// AppointmentBookingRequest 是前端表单或 Agent 工具提交的结构化预约请求。
import langchain4j.service.AppointmentBookingRequest;
// AppointmentCancelResult 是取消预约的结构化结果。
import langchain4j.service.AppointmentCancelResult;
// AppointmentProvider 是预约能力提供者接口。
import langchain4j.service.AppointmentProvider;
// AppointmentResult 是提交预约的结构化结果。
import langchain4j.service.AppointmentResult;
// AppointmentService 是当前本地数据库预约服务。
import langchain4j.service.AppointmentService;
// List 表示一组预约记录。
import java.util.List;
// Service 表示把当前类注册成 Spring 服务组件。
import org.springframework.stereotype.Service;

// 标记当前类为 Spring Service，让 Controller 可以按 AppointmentProvider 接口注入它。
@Service
// 定义虚拟示范医院预约提供者，当前阶段委托本地数据库服务实现。
public class DemoHospitalAppointmentProvider implements AppointmentProvider {

    // 保存本地数据库预约服务依赖。
    private final AppointmentService appointmentService;

    // 构造器注入预约服务。
    public DemoHospitalAppointmentProvider(
            // Spring 会自动注入 AppointmentService Bean。
            AppointmentService appointmentService
    ) {
        // 保存预约服务到成员变量。
        this.appointmentService = appointmentService;
    }

    // 提交预约挂号请求。
    @Override
    public AppointmentResult bookAppointment(AppointmentBookingRequest request) {
        // 委托本地预约服务完成预约。
        return appointmentService.bookAppointment(request);
    }

    @Override
    public AppointmentResult bookAppointment(Long userId, AppointmentBookingRequest request) {
        return appointmentService.bookAppointment(userId, request);
    }

    // 取消指定预约。
    @Override
    public AppointmentCancelResult cancelAppointment(Long appointmentId, String idCard) {
        // 委托本地预约服务完成取消。
        return appointmentService.cancelAppointmentWithResult(appointmentId, idCard);
    }

    @Override
    public AppointmentCancelResult cancelAppointment(Long userId, Long appointmentId, String idCard) {
        return appointmentService.cancelAppointmentWithResult(userId, appointmentId, idCard);
    }

    // 查询患者当前所有有效预约。
    @Override
    public List<Appointment> findValidAppointments(String username, String idCard) {
        // 委托本地预约服务按姓名和身份证查询有效预约。
        return appointmentService.findValidAppointmentsByNameAndIdCard(username, idCard);
    }

    @Override
    public List<Appointment> findValidAppointments(Long userId, String username, String idCard) {
        return appointmentService.findValidAppointmentsByNameAndIdCard(userId, username, idCard);
    }
}
