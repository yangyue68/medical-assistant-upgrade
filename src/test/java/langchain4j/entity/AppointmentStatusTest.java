// 声明测试类所在的包。
package langchain4j.entity;

// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;

// 定义预约状态枚举的单元测试。
class AppointmentStatusTest {

    // 测试预约状态码与数据库约定保持一致。
    @Test
    // 方法名描述测试意图：状态码应该匹配数据库约定。
    void shouldKeepDatabaseStatusCodes() {
        // 验证有效预约状态码为 0。
        assertThat(AppointmentStatus.ACTIVE.code()).isEqualTo(0);
        // 验证已取消状态码为 1。
        assertThat(AppointmentStatus.CANCELLED.code()).isEqualTo(1);
        // 验证已完成状态码为 2。
        assertThat(AppointmentStatus.COMPLETED.code()).isEqualTo(2);
    }

    // 测试 matches 方法能正确判断状态码。
    @Test
    // 方法名描述测试意图：matches 应该识别当前状态。
    void shouldMatchStatusCode() {
        // 验证 ACTIVE 能匹配 0。
        assertThat(AppointmentStatus.ACTIVE.matches(0)).isTrue();
        // 验证 ACTIVE 不匹配 1。
        assertThat(AppointmentStatus.ACTIVE.matches(1)).isFalse();
        // 验证 ACTIVE 不匹配 null。
        assertThat(AppointmentStatus.ACTIVE.matches(null)).isFalse();
    }
}
