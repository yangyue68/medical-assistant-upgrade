// 声明测试类所在的包。
package langchain4j.service.impl;

// AppointmentBookingRequest 是预约请求对象。
import langchain4j.service.AppointmentBookingRequest;
// AppointmentCancelResult 是取消预约结果对象。
import langchain4j.service.AppointmentCancelResult;
// AppointmentErrorCode 是预约业务错误码枚举。
import langchain4j.service.AppointmentErrorCode;
// AppointmentResult 是预约结果对象。
import langchain4j.service.AppointmentResult;
// AppointmentService 是虚拟示范医院 Provider 当前委托的本地服务。
import langchain4j.service.AppointmentService;
// LocalDate 用来构造预约日期。
import java.time.LocalDate;
// List 用来验证查询委托。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

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

// 定义 DemoHospitalAppointmentProvider 的单元测试。
class DemoHospitalAppointmentProviderTest {

    // 测试提交预约会委托 AppointmentService。
    @Test
    // 方法名描述测试意图：Provider 应该委托本地预约服务提交预约。
    void shouldDelegateBookingToAppointmentService() {
        // 创建 AppointmentService mock。
        AppointmentService appointmentService = mock(AppointmentService.class);
        // 创建被测 Provider。
        DemoHospitalAppointmentProvider provider = new DemoHospitalAppointmentProvider(appointmentService);
        // 创建预约请求。
        AppointmentBookingRequest request = validRequest();
        // 创建失败结果，当前测试只关心是否原样返回。
        AppointmentResult expected = AppointmentResult.fail(AppointmentErrorCode.NO_AVAILABLE_SCHEDULE, "无号源");
        // 指定服务返回 expected。
        when(appointmentService.bookAppointment(eq(request))).thenReturn(expected);

        // 调用 Provider 提交预约。
        AppointmentResult actual = provider.bookAppointment(request);

        // 验证 Provider 原样返回服务结果。
        assertThat(actual).isSameAs(expected);
        // 验证 Provider 委托了 AppointmentService。
        verify(appointmentService).bookAppointment(request);
    }

    // 测试取消预约会委托 AppointmentService。
    @Test
    // 方法名描述测试意图：Provider 应该委托本地预约服务取消预约。
    void shouldDelegateCancelToAppointmentService() {
        // 创建 AppointmentService mock。
        AppointmentService appointmentService = mock(AppointmentService.class);
        // 创建被测 Provider。
        DemoHospitalAppointmentProvider provider = new DemoHospitalAppointmentProvider(appointmentService);
        // 创建取消失败结果。
        AppointmentCancelResult expected = AppointmentCancelResult.fail(AppointmentErrorCode.APPOINTMENT_NOT_FOUND, "未找到");
        // 指定服务返回 expected。
        when(appointmentService.cancelAppointmentWithResult(eq(1001L), eq("110101199001011234"))).thenReturn(expected);

        // 调用 Provider 取消预约。
        AppointmentCancelResult actual = provider.cancelAppointment(1001L, "110101199001011234");

        // 验证 Provider 原样返回服务结果。
        assertThat(actual).isSameAs(expected);
        // 验证 Provider 委托了 AppointmentService。
        verify(appointmentService).cancelAppointmentWithResult(1001L, "110101199001011234");
    }

    // 测试查询有效预约会委托 AppointmentService。
    @Test
    // 方法名描述测试意图：Provider 应该委托本地预约服务查询有效预约。
    void shouldDelegateQueryToAppointmentService() {
        // 创建 AppointmentService mock。
        AppointmentService appointmentService = mock(AppointmentService.class);
        // 创建被测 Provider。
        DemoHospitalAppointmentProvider provider = new DemoHospitalAppointmentProvider(appointmentService);
        // 指定服务返回空列表。
        when(appointmentService.findValidAppointmentsByNameAndIdCard(eq("张三"), eq("110101199001011234"))).thenReturn(List.of());

        // 调用 Provider 查询有效预约。
        List<?> appointments = provider.findValidAppointments("张三", "110101199001011234");

        // 验证返回空列表。
        assertThat(appointments).isEmpty();
        // 验证 Provider 委托了 AppointmentService。
        verify(appointmentService).findValidAppointmentsByNameAndIdCard("张三", "110101199001011234");
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
}
