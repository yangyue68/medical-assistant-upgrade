// 声明当前类所在的包，impl 表示它是业务服务接口的实现类。
package langchain4j.service.impl;

// LambdaQueryWrapper 是 MyBatis-Plus 提供的类型安全查询条件构造器。
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
// ServiceImpl 是 MyBatis-Plus 提供的通用 Service 基类。
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
// Appointment 是预约记录实体。
import langchain4j.entity.Appointment;
// AppointmentStatus 是预约状态枚举，用来替代 0/1/2 魔法数字。
import langchain4j.entity.AppointmentStatus;
// DoctorSchedule 是医生排班实体。
import langchain4j.entity.DoctorSchedule;
// AppointmentMapper 是预约表的数据访问接口。
import langchain4j.mapper.AppointmentMapper;
// AppointmentBookingRequest 是预约挂号请求对象。
import langchain4j.service.AppointmentBookingRequest;
// AppointmentCancelResult 是取消预约业务统一结果对象。
import langchain4j.service.AppointmentCancelResult;
// AppointmentErrorCode 是预约业务错误码枚举。
import langchain4j.service.AppointmentErrorCode;
// AppointmentResult 是预约业务统一结果对象。
import langchain4j.service.AppointmentResult;
// AppointmentService 是预约业务服务接口。
import langchain4j.service.AppointmentService;
// DoctorScheduleService 是医生排班业务服务接口。
import langchain4j.service.DoctorScheduleService;
import langchain4j.auth.AccountContext;
// LocalDate 表示不带时间的预约日期。
import java.time.LocalDate;
// List 表示一组预约或排班记录。
import java.util.List;
// Service 表示把当前类注册成 Spring 服务组件。
import org.springframework.stereotype.Service;
// Transactional 表示当前类或方法需要事务保护。
import org.springframework.transaction.annotation.Transactional;

// 标记当前类为 Spring Service。
@Service
// 默认让当前服务的 public 方法运行在事务中。
@Transactional
// 定义预约服务实现类，继承 MyBatis-Plus 通用能力并实现项目业务接口。
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment>
        // 声明当前类实现 AppointmentService 接口。
        implements AppointmentService {

    // 保存医生排班服务依赖，用于查询号源、占号和释放号源。
    private final DoctorScheduleService doctorScheduleService;

    // 构造器注入排班服务，让依赖关系明确，也方便单元测试注入 mock。
    public AppointmentServiceImpl(
            // Spring 会自动注入 DoctorScheduleService Bean。
            DoctorScheduleService doctorScheduleService
    ) {
        // 保存医生排班服务到成员变量。
        this.doctorScheduleService = doctorScheduleService;
    }

    // 根据预约实体中非空字段查询一条有效预约。
    @Override
    public Appointment getOne(Appointment appointment) {
        // 创建 MyBatis-Plus 查询条件对象。
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();

        // 如果传入了预约 ID，就按预约 ID 查询。
        if (appointment.getId() != null) {
            // 添加 ID 等值条件。
            wrapper.eq(Appointment::getId, appointment.getId());
        }
        // 如果传入了患者姓名，就按患者姓名查询。
        if (appointment.getUsername() != null) {
            // 添加患者姓名等值条件。
            wrapper.eq(Appointment::getUsername, appointment.getUsername());
        }
        // 如果传入了身份证号，就按身份证号查询。
        if (appointment.getIdCard() != null) {
            // 添加身份证号等值条件。
            wrapper.eq(Appointment::getIdCard, appointment.getIdCard());
        }
        // 如果传入了手机号，就按手机号查询。
        if (appointment.getPhone() != null) {
            // 添加手机号等值条件。
            wrapper.eq(Appointment::getPhone, appointment.getPhone());
        }
        // 如果传入了科室，就按科室查询。
        if (appointment.getDepartment() != null) {
            // 添加科室等值条件。
            wrapper.eq(Appointment::getDepartment, appointment.getDepartment());
        }
        // 如果传入了医生姓名，就按医生姓名查询。
        if (appointment.getDoctorName() != null) {
            // 添加医生姓名等值条件。
            wrapper.eq(Appointment::getDoctorName, appointment.getDoctorName());
        }
        // 如果传入了预约日期，就按日期查询。
        if (appointment.getDate() != null) {
            // 添加预约日期等值条件。
            wrapper.eq(Appointment::getDate, appointment.getDate());
        }
        // 如果传入了时间段，就按时间段查询。
        if (appointment.getTimeSlot() != null) {
            // 添加时间段等值条件。
            wrapper.eq(Appointment::getTimeSlot, appointment.getTimeSlot());
        }

        // 只查询有效预约。
        wrapper.eq(Appointment::getStatus, AppointmentStatus.ACTIVE.code());

        // 调用 MyBatis-Plus 的 getOne 执行查询。
        return this.getOne(wrapper);
    }

    // 根据身份证号查询该患者所有有效预约。
    @Override
    public List<Appointment> findValidAppointmentsByIdCard(String idCard) {
        // 委托 Mapper 执行带排序的查询。
        return baseMapper.findValidAppointmentsByIdCard(idCard);
    }

    // 根据患者姓名和身份证号查询有效预约。
    @Override
    public List<Appointment> findValidAppointmentsByNameAndIdCard(String username, String idCard) {
        // 创建 MyBatis-Plus 查询条件对象。
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        // 组合患者姓名、身份证号、有效状态，并按日期和时间段升序排列。
        wrapper.eq(Appointment::getUsername, username)
                // 添加身份证号等值条件。
                .eq(Appointment::getIdCard, idCard)
                // 只查询有效预约。
                .eq(Appointment::getStatus, AppointmentStatus.ACTIVE.code())
                // 按日期和时间段升序排列。
                .orderByAsc(Appointment::getDate, Appointment::getTimeSlot);
        if (AccountContext.currentUserId() != null) {
            wrapper.eq(Appointment::getUserId, AccountContext.currentUserId());
        }
        // 执行查询并返回列表。
        return this.list(wrapper);
    }

    // 使用预约请求对象执行预约，这是后续推荐使用的工业级入口。
    @Override
    @Transactional
    public AppointmentResult bookAppointment(AppointmentBookingRequest request) {
        // 如果请求对象本身为空，就直接返回非法请求。
        if (request == null) {
            // 返回带错误码的失败结果。
            return AppointmentResult.fail(AppointmentErrorCode.INVALID_REQUEST, "预约请求不能为空。");
        }

        // 执行请求对象自带的参数校验。
        List<String> validationErrors = request.validate();
        // 如果存在校验错误，就不继续访问数据库和号源。
        if (!validationErrors.isEmpty()) {
            // 把多个错误拼接成一个用户可读提示。
            return AppointmentResult.fail(AppointmentErrorCode.INVALID_REQUEST, String.join(" ", validationErrors));
        }

        // 根据科室、日期、时间段查询可预约排班。
        List<DoctorSchedule> schedules = doctorScheduleService.findAvailableSchedules(
                // 传入预约科室。
                request.getDepartment(),
                // 传入预约日期。
                request.getDate(),
                // 传入预约时间段。
                request.getTimeSlot()
        );

        // 从可用排班中寻找用户指定医生的排班。
        DoctorSchedule targetSchedule = findTargetSchedule(schedules, request.getDoctorName());
        // 如果没有找到目标医生排班，说明该医生在该时间没有可预约号源。
        if (targetSchedule == null) {
            // 返回无可用排班错误。
            return AppointmentResult.fail(
                    // 设置机器可判断错误码。
                    AppointmentErrorCode.NO_AVAILABLE_SCHEDULE,
                    // 设置用户可读提示。
                    "该医生在" + request.getDate() + request.getTimeSlot() + "没有可预约的号源"
            );
        }

        // 检查该患者是否已在同科室同日期同时间段预约过。
        if (hasDuplicateAppointment(request.getIdCard(), request.getDepartment(), request.getDate(), request.getTimeSlot())) {
            // 返回重复预约错误。
            return AppointmentResult.fail(
                    // 设置机器可判断错误码。
                    AppointmentErrorCode.DUPLICATE_APPOINTMENT,
                    // 设置用户可读提示。
                    "您已在" + request.getDate() + request.getTimeSlot() + "预约了" + request.getDepartment() + "的号，请勿重复预约"
            );
        }

        // 通过排班服务执行占号，底层 SQL 会用 booked_count < max_appointments 做并发保护。
        boolean bookedCountIncremented = doctorScheduleService.incrementBookedCount(targetSchedule.getId());
        // 如果占号失败，说明号源可能刚刚被其他用户抢完。
        if (!bookedCountIncremented) {
            // 返回号源已售罄错误。
            return AppointmentResult.fail(AppointmentErrorCode.SLOT_SOLD_OUT, "号源已被抢完，请选择其他医生或时间段");
        }

        // 根据请求和排班创建预约实体。
        Appointment appointment = buildAppointment(request, targetSchedule);
        // 保存预约记录。
        boolean saved = this.save(appointment);
        // 如果保存失败，需要释放刚才占用的号源。
        if (!saved) {
            // 调用排班服务释放号源，尽量恢复库存。
            doctorScheduleService.decrementBookedCount(targetSchedule.getId());
            // 返回非法请求错误；后续可以升级为系统错误码。
            return AppointmentResult.fail(AppointmentErrorCode.INVALID_REQUEST, "预约记录保存失败，请稍后重试。");
        }

        // 返回预约成功结果。
        return AppointmentResult.success(appointment);
    }

    @Override
    public AppointmentResult bookAppointment(Long userId, AppointmentBookingRequest request) {
        return AccountContext.withUser(userId, () -> bookAppointment(request));
    }

    // 保留原散参数预约入口，兼容当前 AppointmentTools 的调用方式。
    @Override
    @Transactional
    public AppointmentResult bookAppointment(String username, String idCard, String phone,
                                             String department, String doctorName,
                                             LocalDate date, String timeSlot) {
        // 把散参数收束成请求对象。
        AppointmentBookingRequest request = new AppointmentBookingRequest(
                // 患者姓名。
                username,
                // 身份证号。
                idCard,
                // 手机号。
                phone,
                // 预约科室。
                department,
                // 预约医生。
                doctorName,
                // 预约日期。
                date,
                // 预约时间段。
                timeSlot
        );
        // 委托新的请求对象入口执行预约。
        return bookAppointment(request);
    }

    // 取消预约并返回结构化结果，这是后续推荐使用的工业级入口。
    @Override
    @Transactional
    public AppointmentCancelResult cancelAppointmentWithResult(Long appointmentId, String idCard) {
        // 如果预约 ID 为空，就无法定位要取消的预约。
        if (appointmentId == null) {
            // 返回非法请求错误。
            return AppointmentCancelResult.fail(AppointmentErrorCode.INVALID_REQUEST, "请提供要取消的预约ID。");
        }
        // 如果身份证号为空，就无法校验用户是否有权取消该预约。
        if (idCard == null || idCard.trim().isEmpty()) {
            // 返回非法请求错误。
            return AppointmentCancelResult.fail(AppointmentErrorCode.INVALID_REQUEST, "请提供患者身份证号。");
        }
        // 根据预约 ID 查询预约记录。
        Appointment appointment = this.getById(appointmentId);
        // 如果预约不存在，直接取消失败。
        if (appointment == null) {
            // 返回预约不存在错误。
            return AppointmentCancelResult.fail(AppointmentErrorCode.APPOINTMENT_NOT_FOUND, "未找到可取消的预约记录。");
        }
        if (AccountContext.currentUserId() != null
                && !AccountContext.currentUserId().equals(appointment.getUserId())) {
            return AppointmentCancelResult.fail(AppointmentErrorCode.APPOINTMENT_NOT_FOUND, "未找到可取消的预约记录。");
        }

        // 如果身份证号不匹配，说明用户无权取消这条预约。
        if (!appointment.getIdCard().equals(idCard)) {
            // 返回身份证不匹配错误。
            return AppointmentCancelResult.fail(AppointmentErrorCode.ID_CARD_MISMATCH, "身份证号与预约记录不匹配。", appointment);
        }

        // 如果预约状态不是有效状态，说明已经取消或完成。
        if (!AppointmentStatus.ACTIVE.matches(appointment.getStatus())) {
            // 返回预约非有效状态错误。
            return AppointmentCancelResult.fail(AppointmentErrorCode.APPOINTMENT_NOT_ACTIVE, "该预约已取消或已完成，不能重复取消。", appointment);
        }

        // 调用 Mapper 把预约状态更新为已取消。
        int affected = baseMapper.cancelAppointment(appointmentId);
        // 如果没有更新任何行，说明数据库状态已经发生变化。
        if (affected == 0) {
            // 返回预约非有效状态错误。
            return AppointmentCancelResult.fail(AppointmentErrorCode.APPOINTMENT_NOT_ACTIVE, "该预约状态已变化，请刷新后重试。", appointment);
        }

        // 释放该预约占用的排班号源。
        doctorScheduleService.decrementBookedCount(appointment.getScheduleId());

        // 返回取消成功结果。
        return AppointmentCancelResult.success(appointment);
    }

    // 取消预约，保留 boolean 返回值兼容当前 AppointmentTools。
    @Override
    @Transactional
    public boolean cancelAppointment(Long appointmentId, String idCard) {
        // 调用结构化取消入口，并把结果转换成旧接口需要的 boolean。
        return cancelAppointmentWithResult(appointmentId, idCard).isSuccess();
    }

    // 检查是否存在重复预约。
    @Override
    public boolean hasDuplicateAppointment(String idCard, String department, LocalDate date, String timeSlot) {
        // 委托 Mapper 执行重复预约计数查询。
        Long userId = AccountContext.currentUserId();
        return userId == null
                ? baseMapper.countDuplicateAppointment(idCard, department, date, timeSlot) > 0
                : baseMapper.countDuplicateAppointmentForUser(userId, idCard, department, date, timeSlot) > 0;
    }

    @Override
    public AppointmentCancelResult cancelAppointmentWithResult(Long userId, Long appointmentId, String idCard) {
        return AccountContext.withUser(userId, () -> cancelAppointmentWithResult(appointmentId, idCard));
    }

    @Override
    public List<Appointment> findValidAppointmentsByNameAndIdCard(Long userId, String username, String idCard) {
        return AccountContext.withUser(userId, () -> findValidAppointmentsByNameAndIdCard(username, idCard));
    }

    // 从排班列表中找到指定医生的排班。
    private DoctorSchedule findTargetSchedule(List<DoctorSchedule> schedules, String doctorName) {
        // 如果排班列表为空，就没有目标排班。
        if (schedules == null || schedules.isEmpty()) {
            // 返回 null 表示没有找到。
            return null;
        }
        // 遍历所有可用排班。
        for (DoctorSchedule schedule : schedules) {
            // 如果当前排班医生姓名与用户指定医生一致，就命中目标排班。
            if (doctorName.equals(schedule.getDoctorName())) {
                // 返回目标排班。
                return schedule;
            }
        }
        // 遍历结束仍未命中，返回 null。
        return null;
    }

    // 根据预约请求和目标排班构建预约实体。
    private Appointment buildAppointment(AppointmentBookingRequest request, DoctorSchedule targetSchedule) {
        // 创建预约实体。
        Appointment appointment = new Appointment();
        appointment.setUserId(AccountContext.currentUserId());
        // 设置患者姓名。
        appointment.setUsername(request.getUsername());
        // 设置身份证号。
        appointment.setIdCard(request.getIdCard());
        // 设置手机号。
        appointment.setPhone(request.getPhone());
        // 设置预约科室。
        appointment.setDepartment(request.getDepartment());
        // 设置预约医生。
        appointment.setDoctorName(request.getDoctorName());
        // 设置预约日期。
        appointment.setDate(request.getDate());
        // 设置预约时间段。
        appointment.setTimeSlot(request.getTimeSlot());
        // 绑定排班 ID，方便取消预约时释放对应号源。
        appointment.setScheduleId(targetSchedule.getId());
        // 设置预约状态为有效。
        appointment.setStatus(AppointmentStatus.ACTIVE.code());
        // 返回构建完成的预约实体。
        return appointment;
    }
}
