// 声明测试类所在的包。
package langchain4j.service.impl;

// Appointment 是预约记录实体。
import langchain4j.entity.Appointment;
// AppointmentStatus 是预约状态枚举。
import langchain4j.entity.AppointmentStatus;
// DoctorSchedule 是医生排班实体。
import langchain4j.entity.DoctorSchedule;
// AppointmentMapper 是预约数据访问接口。
import langchain4j.mapper.AppointmentMapper;
// AppointmentBookingRequest 是预约请求对象。
import langchain4j.service.AppointmentBookingRequest;
// AppointmentCancelResult 是取消预约业务结果对象。
import langchain4j.service.AppointmentCancelResult;
// AppointmentErrorCode 是预约错误码枚举。
import langchain4j.service.AppointmentErrorCode;
// AppointmentResult 是预约业务结果对象。
import langchain4j.service.AppointmentResult;
// DoctorScheduleService 是排班服务接口。
import langchain4j.service.DoctorScheduleService;
// LocalDate 用来构造预约日期。
import java.time.LocalDate;
// List 用来构造排班列表。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;
// ReflectionTestUtils 用来给 MyBatis-Plus ServiceImpl 注入 mock baseMapper。
import org.springframework.test.util.ReflectionTestUtils;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// Mockito 的 any 用来匹配任意参数。
import static org.mockito.ArgumentMatchers.any;
// Mockito 的 anyLong 用来匹配任意 Long 参数。
import static org.mockito.ArgumentMatchers.anyLong;
// Mockito 的 eq 用来匹配指定参数。
import static org.mockito.ArgumentMatchers.eq;
// Mockito 的 ArgumentCaptor 用来捕获 mock 方法收到的参数。
import static org.mockito.ArgumentCaptor.forClass;
// Mockito 的 mock 用来创建模拟对象。
import static org.mockito.Mockito.mock;
// Mockito 的 never 用来验证某个调用从未发生。
import static org.mockito.Mockito.never;
// Mockito 的 verify 用来验证方法调用。
import static org.mockito.Mockito.verify;
// Mockito 的 verifyNoInteractions 用来验证 mock 没有发生任何调用。
import static org.mockito.Mockito.verifyNoInteractions;
// Mockito 的 when 用来指定 mock 方法返回值。
import static org.mockito.Mockito.when;

// 定义 AppointmentServiceImpl 的单元测试。
class AppointmentServiceImplTest {

    // 测试预约成功路径。
    @Test
    // 方法名描述测试意图：有号源且无重复预约时应该预约成功。
    void shouldBookAppointmentSuccessfully() {
        // 创建排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建预约 Mapper mock。
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        // 创建被测服务并注入 mock 依赖。
        AppointmentServiceImpl service = testService(scheduleService, appointmentMapper);
        // 创建合法预约请求。
        AppointmentBookingRequest request = validRequest();
        // 创建目标医生排班。
        DoctorSchedule schedule = schedule("王医生");
        // 指定查询号源时返回目标排班。
        when(scheduleService.findAvailableSchedules(eq("内科"), eq(request.getDate()), eq("上午"))).thenReturn(List.of(schedule));
        // 指定没有重复预约。
        when(appointmentMapper.countDuplicateAppointment(eq("110101199001011234"), eq("内科"), eq(request.getDate()), eq("上午"))).thenReturn(0);
        // 指定占号成功。
        when(scheduleService.incrementBookedCount(eq(10L))).thenReturn(true);
        // 指定保存预约记录成功。
        when(appointmentMapper.insert(any(Appointment.class))).thenReturn(1);

        // 执行预约。
        AppointmentResult result = service.bookAppointment(request);

        // 验证预约结果成功。
        assertThat(result.isSuccess()).isTrue();
        // 验证错误码为 SUCCESS。
        assertThat(result.getErrorCode()).isEqualTo(AppointmentErrorCode.SUCCESS);
        // 验证返回了预约实体。
        assertThat(result.getAppointment()).isNotNull();
        // 创建预约实体参数捕获器。
        var appointmentCaptor = forClass(Appointment.class);
        // 捕获保存时传给 Mapper 的预约实体。
        verify(appointmentMapper).insert(appointmentCaptor.capture());
        // 验证保存的预约记录绑定了排班 ID。
        assertThat(appointmentCaptor.getValue().getScheduleId()).isEqualTo(10L);
        // 验证保存的预约记录状态为有效。
        assertThat(appointmentCaptor.getValue().getStatus()).isEqualTo(AppointmentStatus.ACTIVE.code());
    }

    // 测试非法请求不会访问数据库或排班服务。
    @Test
    // 方法名描述测试意图：非法请求应该提前失败。
    void shouldRejectInvalidRequestBeforeQueryingSchedule() {
        // 创建排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建预约 Mapper mock。
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        // 创建被测服务并注入 mock 依赖。
        AppointmentServiceImpl service = testService(scheduleService, appointmentMapper);

        // 执行预约，并传入空请求。
        AppointmentResult result = service.bookAppointment(new AppointmentBookingRequest());

        // 验证预约失败。
        assertThat(result.isSuccess()).isFalse();
        // 验证错误码为非法请求。
        assertThat(result.getErrorCode()).isEqualTo(AppointmentErrorCode.INVALID_REQUEST);
        // 验证没有访问排班服务。
        verifyNoInteractions(scheduleService);
        // 验证没有访问预约 Mapper。
        verifyNoInteractions(appointmentMapper);
    }

    // 测试没有可预约排班时返回明确错误码。
    @Test
    // 方法名描述测试意图：无号源时应该失败。
    void shouldFailWhenNoAvailableSchedule() {
        // 创建排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建预约 Mapper mock。
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        // 创建被测服务并注入 mock 依赖。
        AppointmentServiceImpl service = testService(scheduleService, appointmentMapper);
        // 创建合法预约请求。
        AppointmentBookingRequest request = validRequest();
        // 指定查询号源时返回空列表。
        when(scheduleService.findAvailableSchedules(eq("内科"), eq(request.getDate()), eq("上午"))).thenReturn(List.of());

        // 执行预约。
        AppointmentResult result = service.bookAppointment(request);

        // 验证预约失败。
        assertThat(result.isSuccess()).isFalse();
        // 验证错误码为无可用排班。
        assertThat(result.getErrorCode()).isEqualTo(AppointmentErrorCode.NO_AVAILABLE_SCHEDULE);
        // 验证不会检查重复预约，因为没有号源已经可以提前失败。
        verify(appointmentMapper, never()).countDuplicateAppointment(any(), any(), any(), any());
    }

    // 测试重复预约会提前失败。
    @Test
    // 方法名描述测试意图：重复预约应该被拒绝。
    void shouldFailWhenDuplicateAppointmentExists() {
        // 创建排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建预约 Mapper mock。
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        // 创建被测服务并注入 mock 依赖。
        AppointmentServiceImpl service = testService(scheduleService, appointmentMapper);
        // 创建合法预约请求。
        AppointmentBookingRequest request = validRequest();
        // 指定查询号源时返回目标排班。
        when(scheduleService.findAvailableSchedules(eq("内科"), eq(request.getDate()), eq("上午"))).thenReturn(List.of(schedule("王医生")));
        // 指定已经存在重复预约。
        when(appointmentMapper.countDuplicateAppointment(eq("110101199001011234"), eq("内科"), eq(request.getDate()), eq("上午"))).thenReturn(1);

        // 执行预约。
        AppointmentResult result = service.bookAppointment(request);

        // 验证预约失败。
        assertThat(result.isSuccess()).isFalse();
        // 验证错误码为重复预约。
        assertThat(result.getErrorCode()).isEqualTo(AppointmentErrorCode.DUPLICATE_APPOINTMENT);
        // 验证重复预约时不会继续占号。
        verify(scheduleService, never()).incrementBookedCount(anyLong());
    }

    // 测试并发占号失败会返回号源售罄。
    @Test
    // 方法名描述测试意图：占号失败应该返回号源售罄。
    void shouldFailWhenSlotSoldOutDuringIncrement() {
        // 创建排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建预约 Mapper mock。
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        // 创建被测服务并注入 mock 依赖。
        AppointmentServiceImpl service = testService(scheduleService, appointmentMapper);
        // 创建合法预约请求。
        AppointmentBookingRequest request = validRequest();
        // 指定查询号源时返回目标排班。
        when(scheduleService.findAvailableSchedules(eq("内科"), eq(request.getDate()), eq("上午"))).thenReturn(List.of(schedule("王医生")));
        // 指定没有重复预约。
        when(appointmentMapper.countDuplicateAppointment(eq("110101199001011234"), eq("内科"), eq(request.getDate()), eq("上午"))).thenReturn(0);
        // 指定占号失败。
        when(scheduleService.incrementBookedCount(eq(10L))).thenReturn(false);

        // 执行预约。
        AppointmentResult result = service.bookAppointment(request);

        // 验证预约失败。
        assertThat(result.isSuccess()).isFalse();
        // 验证错误码为号源售罄。
        assertThat(result.getErrorCode()).isEqualTo(AppointmentErrorCode.SLOT_SOLD_OUT);
        // 验证占号失败时不会保存预约记录。
        verify(appointmentMapper, never()).insert(any(Appointment.class));
    }

    // 测试保存预约失败时会释放已占用号源。
    @Test
    // 方法名描述测试意图：保存失败应该释放号源。
    void shouldReleaseSlotWhenSavingAppointmentFails() {
        // 创建排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建预约 Mapper mock。
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        // 创建被测服务并注入 mock 依赖。
        AppointmentServiceImpl service = testService(scheduleService, appointmentMapper);
        // 创建合法预约请求。
        AppointmentBookingRequest request = validRequest();
        // 指定查询号源时返回目标排班。
        when(scheduleService.findAvailableSchedules(eq("内科"), eq(request.getDate()), eq("上午"))).thenReturn(List.of(schedule("王医生")));
        // 指定没有重复预约。
        when(appointmentMapper.countDuplicateAppointment(eq("110101199001011234"), eq("内科"), eq(request.getDate()), eq("上午"))).thenReturn(0);
        // 指定占号成功。
        when(scheduleService.incrementBookedCount(eq(10L))).thenReturn(true);
        // 指定保存预约记录失败。
        when(appointmentMapper.insert(any(Appointment.class))).thenReturn(0);

        // 执行预约。
        AppointmentResult result = service.bookAppointment(request);

        // 验证预约失败。
        assertThat(result.isSuccess()).isFalse();
        // 验证错误码为非法请求，当前阶段先复用该错误码表达保存失败。
        assertThat(result.getErrorCode()).isEqualTo(AppointmentErrorCode.INVALID_REQUEST);
        // 验证保存失败后释放了刚才占用的号源。
        verify(scheduleService).decrementBookedCount(10L);
    }

    // 测试取消预约成功路径。
    @Test
    // 方法名描述测试意图：有效预约应该可以取消并释放号源。
    void shouldCancelAppointmentSuccessfully() {
        // 创建排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建预约 Mapper mock。
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        // 创建被测服务并注入 mock 依赖。
        AppointmentServiceImpl service = testService(scheduleService, appointmentMapper);
        // 创建一条有效预约记录。
        Appointment appointment = activeAppointment();
        // 指定根据 ID 查询时返回该预约。
        when(appointmentMapper.selectById(eq(1001L))).thenReturn(appointment);
        // 指定取消预约 SQL 更新成功。
        when(appointmentMapper.cancelAppointment(eq(1001L))).thenReturn(1);

        // 执行结构化取消预约。
        AppointmentCancelResult result = service.cancelAppointmentWithResult(1001L, "110101199001011234");

        // 验证取消成功。
        assertThat(result.isSuccess()).isTrue();
        // 验证错误码为 SUCCESS。
        assertThat(result.getErrorCode()).isEqualTo(AppointmentErrorCode.SUCCESS);
        // 验证结果里携带了被取消的预约记录。
        assertThat(result.getAppointment()).isSameAs(appointment);
        // 验证取消成功后释放了排班号源。
        verify(scheduleService).decrementBookedCount(10L);
    }

    // 测试取消预约时预约不存在。
    @Test
    // 方法名描述测试意图：不存在的预约不能取消。
    void shouldFailToCancelWhenAppointmentNotFound() {
        // 创建排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建预约 Mapper mock。
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        // 创建被测服务并注入 mock 依赖。
        AppointmentServiceImpl service = testService(scheduleService, appointmentMapper);
        // 指定根据 ID 查询时返回 null。
        when(appointmentMapper.selectById(eq(1001L))).thenReturn(null);

        // 执行结构化取消预约。
        AppointmentCancelResult result = service.cancelAppointmentWithResult(1001L, "110101199001011234");

        // 验证取消失败。
        assertThat(result.isSuccess()).isFalse();
        // 验证错误码为预约不存在。
        assertThat(result.getErrorCode()).isEqualTo(AppointmentErrorCode.APPOINTMENT_NOT_FOUND);
        // 验证预约不存在时不会释放号源。
        verifyNoInteractions(scheduleService);
    }

    // 测试取消预约时身份证不匹配。
    @Test
    // 方法名描述测试意图：身份证不匹配时不能取消。
    void shouldFailToCancelWhenIdCardMismatch() {
        // 创建排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建预约 Mapper mock。
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        // 创建被测服务并注入 mock 依赖。
        AppointmentServiceImpl service = testService(scheduleService, appointmentMapper);
        // 指定根据 ID 查询时返回有效预约。
        when(appointmentMapper.selectById(eq(1001L))).thenReturn(activeAppointment());

        // 执行结构化取消预约，并传入不匹配的身份证号。
        AppointmentCancelResult result = service.cancelAppointmentWithResult(1001L, "110101199001019999");

        // 验证取消失败。
        assertThat(result.isSuccess()).isFalse();
        // 验证错误码为身份证不匹配。
        assertThat(result.getErrorCode()).isEqualTo(AppointmentErrorCode.ID_CARD_MISMATCH);
        // 验证身份证不匹配时不会更新取消状态。
        verify(appointmentMapper, never()).cancelAppointment(anyLong());
        // 验证身份证不匹配时不会释放号源。
        verifyNoInteractions(scheduleService);
    }

    // 测试取消预约时预约已经不是有效状态。
    @Test
    // 方法名描述测试意图：已取消预约不能重复取消。
    void shouldFailToCancelWhenAppointmentIsNotActive() {
        // 创建排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建预约 Mapper mock。
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        // 创建被测服务并注入 mock 依赖。
        AppointmentServiceImpl service = testService(scheduleService, appointmentMapper);
        // 创建一条已取消预约。
        Appointment appointment = activeAppointment();
        // 把预约状态改成已取消。
        appointment.setStatus(AppointmentStatus.CANCELLED.code());
        // 指定根据 ID 查询时返回已取消预约。
        when(appointmentMapper.selectById(eq(1001L))).thenReturn(appointment);

        // 执行结构化取消预约。
        AppointmentCancelResult result = service.cancelAppointmentWithResult(1001L, "110101199001011234");

        // 验证取消失败。
        assertThat(result.isSuccess()).isFalse();
        // 验证错误码为预约非有效状态。
        assertThat(result.getErrorCode()).isEqualTo(AppointmentErrorCode.APPOINTMENT_NOT_ACTIVE);
        // 验证不会重复执行取消 SQL。
        verify(appointmentMapper, never()).cancelAppointment(anyLong());
        // 验证不会重复释放号源。
        verifyNoInteractions(scheduleService);
    }

    // 测试保留的旧 boolean 取消接口会委托结构化结果。
    @Test
    // 方法名描述测试意图：旧取消接口仍然兼容。
    void shouldKeepBooleanCancelMethodCompatible() {
        // 创建排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建预约 Mapper mock。
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        // 创建被测服务并注入 mock 依赖。
        AppointmentServiceImpl service = testService(scheduleService, appointmentMapper);
        // 指定根据 ID 查询时返回有效预约。
        when(appointmentMapper.selectById(eq(1001L))).thenReturn(activeAppointment());
        // 指定取消预约 SQL 更新成功。
        when(appointmentMapper.cancelAppointment(eq(1001L))).thenReturn(1);

        // 执行旧 boolean 取消接口。
        boolean success = service.cancelAppointment(1001L, "110101199001011234");

        // 验证旧接口仍然返回 true。
        assertThat(success).isTrue();
    }

    // 创建注入了 mock 依赖的预约服务。
    private static AppointmentServiceImpl testService(
            // 排班服务 mock。
            DoctorScheduleService scheduleService,
            // 预约 Mapper mock。
            AppointmentMapper appointmentMapper
    ) {
        // 创建被测服务。
        AppointmentServiceImpl service = new AppointmentServiceImpl(scheduleService);
        // 给 MyBatis-Plus ServiceImpl 的 baseMapper 字段注入 mock。
        ReflectionTestUtils.setField(service, "baseMapper", appointmentMapper);
        // 返回注入完成的服务。
        return service;
    }

    // 创建测试用合法预约请求。
    private static AppointmentBookingRequest validRequest() {
        // 返回字段完整、格式合法、日期在未来的预约请求。
        return new AppointmentBookingRequest(
                // 患者姓名。
                "张三",
                // 身份证号。
                "110101199001011234",
                // 手机号。
                "13800138000",
                // 科室。
                "内科",
                // 医生。
                "王医生",
                // 预约日期。
                LocalDate.now().plusDays(1),
                // 时间段。
                "上午"
        );
    }

    // 创建测试用医生排班。
    private static DoctorSchedule schedule(String doctorName) {
        // 创建排班实体。
        DoctorSchedule schedule = new DoctorSchedule();
        // 设置排班 ID。
        schedule.setId(10L);
        // 设置医生姓名。
        schedule.setDoctorName(doctorName);
        // 设置科室。
        schedule.setDepartment("内科");
        // 设置医生职称。
        schedule.setTitle("主任医师");
        // 设置最大号源数。
        schedule.setMaxAppointments(10);
        // 设置已预约人数。
        schedule.setBookedCount(1);
        // 设置排班状态为正常。
        schedule.setStatus(1);
        // 返回排班实体。
        return schedule;
    }

    // 创建测试用有效预约。
    private static Appointment activeAppointment() {
        // 创建预约实体。
        Appointment appointment = new Appointment();
        // 设置预约 ID。
        appointment.setId(1001L);
        // 设置患者姓名。
        appointment.setUsername("张三");
        // 设置身份证号。
        appointment.setIdCard("110101199001011234");
        // 设置手机号。
        appointment.setPhone("13800138000");
        // 设置科室。
        appointment.setDepartment("内科");
        // 设置医生。
        appointment.setDoctorName("王医生");
        // 设置预约日期。
        appointment.setDate(LocalDate.now().plusDays(1));
        // 设置时间段。
        appointment.setTimeSlot("上午");
        // 设置排班 ID。
        appointment.setScheduleId(10L);
        // 设置预约状态为有效。
        appointment.setStatus(AppointmentStatus.ACTIVE.code());
        // 返回预约实体。
        return appointment;
    }
}
