// 声明测试类所在的包。
package langchain4j.service;

// LocalDate 用来构造预约日期。
import java.time.LocalDate;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;

// 定义预约请求对象的单元测试。
class AppointmentBookingRequestTest {

    // 测试完整合法请求可以通过校验。
    @Test
    // 方法名描述测试意图：合法预约请求应该通过校验。
    void shouldValidateValidRequest() {
        // 创建一个合法预约请求。
        AppointmentBookingRequest request = validRequest();

        // 验证校验错误为空。
        assertThat(request.validate()).isEmpty();
        // 验证 isValid 返回 true。
        assertThat(request.isValid()).isTrue();
    }

    // 测试缺少必填字段时能返回所有错误。
    @Test
    // 方法名描述测试意图：缺少必填字段时应该返回错误。
    void shouldRejectMissingRequiredFields() {
        // 创建一个空请求。
        AppointmentBookingRequest request = new AppointmentBookingRequest();

        // 执行校验并断言包含关键错误。
        assertThat(request.validate())
                // 验证姓名缺失错误。
                .contains("请提供患者姓名。")
                // 验证身份证缺失错误。
                .contains("请提供患者身份证号。")
                // 验证手机号缺失错误。
                .contains("请提供患者手机号。")
                // 验证科室缺失错误。
                .contains("请提供预约科室。")
                // 验证医生缺失错误。
                .contains("请提供预约医生姓名。")
                // 验证日期缺失错误。
                .contains("请提供预约日期。");
    }

    // 测试格式错误会被识别。
    @Test
    // 方法名描述测试意图：非法格式应该被拒绝。
    void shouldRejectInvalidFormats() {
        // 创建一个基础合法请求。
        AppointmentBookingRequest request = validRequest();
        // 设置非法身份证号。
        request.setIdCard("123");
        // 设置非法手机号。
        request.setPhone("10086");
        // 设置非法时间段。
        request.setTimeSlot("晚上");

        // 执行校验并断言包含格式错误。
        assertThat(request.validate())
                // 验证身份证格式错误。
                .contains("身份证号格式不正确，请提供18位身份证号。")
                // 验证手机号格式错误。
                .contains("手机号格式不正确，请提供11位大陆手机号。")
                // 验证时间段错误。
                .contains("预约时间段只能是上午或下午。");
    }

    // 测试过去日期会被拒绝。
    @Test
    // 方法名描述测试意图：预约日期不能早于今天。
    void shouldRejectPastDate() {
        // 创建一个基础合法请求。
        AppointmentBookingRequest request = validRequest();
        // 设置昨天作为预约日期。
        request.setDate(LocalDate.now().minusDays(1));

        // 验证校验结果包含过去日期错误。
        assertThat(request.validate()).contains("预约日期不能早于今天。");
    }

    // 创建测试用合法预约请求。
    private static AppointmentBookingRequest validRequest() {
        // 返回一个字段完整、格式合法、日期在未来的请求。
        return new AppointmentBookingRequest(
                // 患者姓名。
                "张三",
                // 18 位身份证号。
                "110101199001011234",
                // 11 位大陆手机号。
                "13800138000",
                // 预约科室。
                "内科",
                // 预约医生。
                "王医生",
                // 明天预约，避免测试受当天日期影响。
                LocalDate.now().plusDays(1),
                // 预约时间段。
                "上午"
        );
    }
}
