// 声明测试类所在的包。
package langchain4j.controller;

// ApiResponse 是统一接口响应结构。
import langchain4j.common.ApiResponse;
// Appointment 是预约记录实体。
import langchain4j.entity.Appointment;
// AppointmentStatus 是预约状态枚举。
import langchain4j.entity.AppointmentStatus;
// AppointmentBookingRequest 是预约请求对象。
import langchain4j.service.AppointmentBookingRequest;
// AppointmentBookingResponse 是预约表单响应对象。
import langchain4j.service.AppointmentBookingResponse;
// AppointmentCancelRequest 是取消预约请求对象。
import langchain4j.service.AppointmentCancelRequest;
// AppointmentCancelResponse 是取消预约响应对象。
import langchain4j.service.AppointmentCancelResponse;
// AppointmentCancelResult 是取消预约业务结果对象。
import langchain4j.service.AppointmentCancelResult;
// AppointmentErrorCode 是预约业务错误码枚举。
import langchain4j.service.AppointmentErrorCode;
// AppointmentProvider 是预约能力提供者接口。
import langchain4j.service.AppointmentProvider;
// AppointmentQueryRequest 是查询预约请求对象。
import langchain4j.service.AppointmentQueryRequest;
// AppointmentResult 是预约结果对象。
import langchain4j.service.AppointmentResult;
// AppointmentSummaryResponse 是预约摘要响应对象。
import langchain4j.service.AppointmentSummaryResponse;
// DoctorScheduleService 是医生排班服务接口。
import langchain4j.service.DoctorScheduleService;
// DoctorSchedule 是医生排班实体。
import langchain4j.entity.DoctorSchedule;
// LocalDate 用来构造预约日期。
import java.time.LocalDate;
// Arrays 用来处理反射得到的方法参数数组。
import java.util.Arrays;
// List 表示一组排班。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;
// RequestParam 是 Spring MVC 查询参数注解。
import org.springframework.web.bind.annotation.RequestParam;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// Mockito 的 eq 用来匹配指定参数。
import static org.mockito.ArgumentMatchers.eq;
// Mockito 的 mock 用来创建模拟对象。
import static org.mockito.Mockito.mock;
// Mockito 的 verify 用来验证方法调用。
import static org.mockito.Mockito.verify;
// Mockito 的 when 用来指定 mock 方法返回值。
import static org.mockito.Mockito.when;

// 定义 AppointmentController 的单元测试。
class AppointmentControllerTest {

    // 测试前端表单预约成功时返回结构化响应。
    @Test
    // 方法名描述测试意图：成功提交表单应该返回预约摘要。
    void shouldReturnBookingResponseWhenFormBookingSucceeds() {
        // 创建 AppointmentProvider mock。
        AppointmentProvider provider = mock(AppointmentProvider.class);
        // 创建被测 Controller。
        // 创建医生排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建被测 Controller。
        AppointmentController controller = new AppointmentController(provider, scheduleService);
        // 创建预约请求。
        AppointmentBookingRequest request = validRequest();
        // 创建预约成功结果。
        AppointmentResult result = AppointmentResult.success(appointment());
        // 指定 Provider 返回预约成功结果。
        when(provider.bookAppointment(eq(request))).thenReturn(result);

        // 调用前端预约表单接口。
        ApiResponse<AppointmentBookingResponse> response = controller.book(request);

        // 验证统一响应码为成功。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证响应 data 表示预约成功。
        assertThat(response.getData().isSuccess()).isTrue();
        // 验证响应 data 携带预约 ID。
        assertThat(response.getData().getAppointmentId()).isEqualTo(1001L);
        // 验证身份证号已脱敏。
        assertThat(response.getData().getMaskedIdCard()).isEqualTo("110101********1234");
        // 验证手机号已脱敏。
        assertThat(response.getData().getMaskedPhone()).isEqualTo("138****8000");
        // 验证 Controller 委托了 Provider。
        verify(provider).bookAppointment(request);
    }

    // 测试前端表单预约失败时仍返回结构化错误码。
    @Test
    // 方法名描述测试意图：失败提交表单应该返回错误码和提示。
    void shouldReturnErrorCodeWhenFormBookingFails() {
        // 创建 AppointmentProvider mock。
        AppointmentProvider provider = mock(AppointmentProvider.class);
        // 创建被测 Controller。
        // 创建医生排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建被测 Controller。
        AppointmentController controller = new AppointmentController(provider, scheduleService);
        // 创建预约请求。
        AppointmentBookingRequest request = validRequest();
        // 创建无号源失败结果。
        AppointmentResult result = AppointmentResult.fail(AppointmentErrorCode.NO_AVAILABLE_SCHEDULE, "无可预约号源");
        // 指定 Provider 返回失败结果。
        when(provider.bookAppointment(eq(request))).thenReturn(result);

        // 调用前端预约表单接口。
        ApiResponse<AppointmentBookingResponse> response = controller.book(request);

        // 验证统一响应码为业务失败。
        assertThat(response.getCode()).isEqualTo(400);
        // 验证统一响应消息来自业务结果。
        assertThat(response.getMessage()).isEqualTo("无可预约号源");
        // 验证 data 中仍然携带机器可判断错误码。
        assertThat(response.getData().getErrorCode()).isEqualTo(AppointmentErrorCode.NO_AVAILABLE_SCHEDULE);
        // 验证 data 标记预约失败。
        assertThat(response.getData().isSuccess()).isFalse();
        // 验证 Controller 委托了 Provider。
        verify(provider).bookAppointment(request);
    }

    // 测试可预约号源接口会返回真实排班选项。
    @Test
    // 方法名描述测试意图：前端预约表单应该从后端排班生成医生下拉选项。
    void shouldListAvailableSchedules() {
        // 创建预约 Provider mock。
        AppointmentProvider provider = mock(AppointmentProvider.class);
        // 创建医生排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建被测 Controller。
        AppointmentController controller = new AppointmentController(provider, scheduleService);
        // 创建测试排班。
        DoctorSchedule schedule = schedule();
        // 指定排班服务返回测试排班。
        when(scheduleService.findAvailableSchedulesBetween(eq(LocalDate.parse("2026-07-29")), eq(LocalDate.parse("2026-07-30")), eq("内科"), eq("上午")))
                .thenReturn(List.of(schedule));

        // 调用号源查询接口。
        ApiResponse<?> response = controller.schedules(LocalDate.parse("2026-07-29"), LocalDate.parse("2026-07-30"), "内科", "上午");

        // 验证统一响应码为成功。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证返回了号源数据。
        assertThat((List<?>) response.getData()).hasSize(1);
        // 验证 Controller 委托排班服务查询。
        verify(scheduleService).findAvailableSchedulesBetween(LocalDate.parse("2026-07-29"), LocalDate.parse("2026-07-30"), "内科", "上午");
    }

    // 测试号源查询接口显式声明查询参数名。
    @Test
    // 方法名描述测试意图：没有 -parameters 编译参数时 Spring 仍应能绑定请求参数。
    void shouldDeclareScheduleQueryParameterNamesExplicitly() throws Exception {
        // 通过反射读取 schedules 方法。
        var method = AppointmentController.class.getMethod("schedules", LocalDate.class, LocalDate.class, String.class, String.class);
        // 提取方法参数上的 RequestParam 注解名称。
        List<String> names = Arrays.stream(method.getParameters())
                // 读取每个参数上的 RequestParam 注解。
                .map(parameter -> parameter.getAnnotation(RequestParam.class))
                // 读取注解里的 name。
                .map(RequestParam::name)
                // 收集成列表。
                .toList();

        // 验证四个查询参数都显式声明了名称。
        assertThat(names).containsExactly("startDate", "endDate", "department", "timeSlot");
    }

    // 测试查询本人预约会返回脱敏后的预约摘要。
    @Test
    // 方法名描述测试意图：前端查询预约接口不应该泄露身份证和手机号原文。
    void shouldSearchAppointmentsWithMaskedSummary() {
        // 创建预约 Provider mock。
        AppointmentProvider provider = mock(AppointmentProvider.class);
        // 创建医生排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建被测 Controller。
        AppointmentController controller = new AppointmentController(provider, scheduleService);
        // 创建查询请求。
        AppointmentQueryRequest request = queryRequest();
        // 指定 Provider 返回一条有效预约。
        when(provider.findValidAppointments(eq("张三"), eq("110101199001011234"))).thenReturn(List.of(appointment()));

        // 调用预约查询接口。
        ApiResponse<List<AppointmentSummaryResponse>> response = controller.search(request);

        // 验证统一响应码为成功。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证返回一条预约摘要。
        assertThat(response.getData()).hasSize(1);
        // 验证身份证号已脱敏。
        assertThat(response.getData().get(0).getMaskedIdCard()).isEqualTo("110101********1234");
        // 验证手机号已脱敏。
        assertThat(response.getData().get(0).getMaskedPhone()).isEqualTo("138****8000");
        // 验证 Controller 委托 Provider 查询。
        verify(provider).findValidAppointments("张三", "110101199001011234");
    }

    // 测试取消预约成功时返回结构化响应。
    @Test
    // 方法名描述测试意图：前端取消预约接口应该返回取消结果和预约摘要。
    void shouldCancelAppointmentWithStructuredResponse() {
        // 创建预约 Provider mock。
        AppointmentProvider provider = mock(AppointmentProvider.class);
        // 创建医生排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建被测 Controller。
        AppointmentController controller = new AppointmentController(provider, scheduleService);
        // 创建取消请求。
        AppointmentCancelRequest request = cancelRequest();
        // 指定 Provider 返回取消成功。
        when(provider.cancelAppointment(eq(1001L), eq("110101199001011234")))
                .thenReturn(AppointmentCancelResult.success(appointment()));

        // 调用取消预约接口。
        ApiResponse<AppointmentCancelResponse> response = controller.cancel(1001L, request);

        // 验证统一响应码为成功。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证取消成功标记。
        assertThat(response.getData().isSuccess()).isTrue();
        // 验证预约 ID 会返回给前端刷新状态。
        assertThat(response.getData().getAppointment().getAppointmentId()).isEqualTo(1001L);
        // 验证 Controller 委托 Provider 取消。
        verify(provider).cancelAppointment(1001L, "110101199001011234");
    }

    // 测试取消预约失败时仍返回业务错误码。
    @Test
    // 方法名描述测试意图：身份证不匹配时前端能拿到结构化失败原因。
    void shouldReturnCancelErrorCodeWhenCancelFails() {
        // 创建预约 Provider mock。
        AppointmentProvider provider = mock(AppointmentProvider.class);
        // 创建医生排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建被测 Controller。
        AppointmentController controller = new AppointmentController(provider, scheduleService);
        // 创建取消请求。
        AppointmentCancelRequest request = cancelRequest();
        // 指定 Provider 返回身份证不匹配失败。
        when(provider.cancelAppointment(eq(1001L), eq("110101199001011234")))
                .thenReturn(AppointmentCancelResult.fail(AppointmentErrorCode.ID_CARD_MISMATCH, "身份证号与预约记录不匹配。", appointment()));

        // 调用取消预约接口。
        ApiResponse<AppointmentCancelResponse> response = controller.cancel(1001L, request);

        // 验证业务失败响应码。
        assertThat(response.getCode()).isEqualTo(400);
        // 验证错误码保留给前端。
        assertThat(response.getData().getErrorCode()).isEqualTo(AppointmentErrorCode.ID_CARD_MISMATCH);
        // 验证取消成功标记为 false。
        assertThat(response.getData().isSuccess()).isFalse();
    }

    // 创建合法预约请求。
    private static AppointmentBookingRequest validRequest() {
        // 返回字段完整的预约请求。
        return new AppointmentBookingRequest(
                // 患者姓名。
                "张三",
                // 身份证号。
                "110101199001011234",
                // 手机号。
                "13800138000",
                // 预约科室。
                "内科",
                // 预约医生。
                "王医生",
                // 预约日期。
                LocalDate.now().plusDays(1),
                // 预约时间段。
                "上午"
        );
    }

    // 创建查询预约请求。
    private static AppointmentQueryRequest queryRequest() {
        // 创建查询请求对象。
        AppointmentQueryRequest request = new AppointmentQueryRequest();
        // 设置患者姓名。
        request.setUsername("张三");
        // 设置身份证号。
        request.setIdCard("110101199001011234");
        // 返回查询请求。
        return request;
    }

    // 创建取消预约请求。
    private static AppointmentCancelRequest cancelRequest() {
        // 创建取消请求对象。
        AppointmentCancelRequest request = new AppointmentCancelRequest();
        // 设置身份证号。
        request.setIdCard("110101199001011234");
        // 返回取消请求。
        return request;
    }

    // 创建预约成功后的预约实体。
    private static Appointment appointment() {
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
        // 设置预约科室。
        appointment.setDepartment("内科");
        // 设置预约医生。
        appointment.setDoctorName("王医生");
        // 设置预约日期。
        appointment.setDate(LocalDate.now().plusDays(1));
        // 设置预约时间段。
        appointment.setTimeSlot("上午");
        // 设置预约状态。
        appointment.setStatus(AppointmentStatus.ACTIVE.code());
        // 返回预约实体。
        return appointment;
    }

    // 创建测试排班。
    private static DoctorSchedule schedule() {
        // 创建排班对象。
        DoctorSchedule schedule = new DoctorSchedule();
        // 设置排班 ID。
        schedule.setId(2001L);
        // 设置医生姓名。
        schedule.setDoctorName("王医生");
        // 设置科室。
        schedule.setDepartment("内科");
        // 设置职称。
        schedule.setTitle("主治医师");
        // 设置日期。
        schedule.setDate(LocalDate.parse("2026-07-29"));
        // 设置时间段。
        schedule.setTimeSlot("上午");
        // 设置最大号源。
        schedule.setMaxAppointments(20);
        // 设置已预约人数。
        schedule.setBookedCount(3);
        // 设置正常出诊状态。
        schedule.setStatus(1);
        // 返回测试排班。
        return schedule;
    }
}
