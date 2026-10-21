// 声明测试类所在的包。
package langchain4j.memory;

// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;

// 定义 ChatMemoryPolicy 的单元测试。
class ChatMemoryPolicyTest {

    // 测试最大窗口小于最小窗口时会被自动规整。
    @Test
    // 方法名描述测试意图：最大窗口不能小于最小窗口。
    void shouldNormalizeMaxWindowMessages() {
        // 创建策略对象，故意传入 max=2、min=4。
        //在ChatMemoryPolicy构造函数里，最大值会被规整
        ChatMemoryPolicy policy = new ChatMemoryPolicy(2, 4);

        // 验证最小窗口被保存为 4。
        assertThat(policy.minWindowMessages()).isEqualTo(4);
        // 验证最大窗口被规整为 4，而不是保留错误的 2。
        assertThat(policy.maxWindowMessages()).isEqualTo(4);
    }

    // 测试异常消息中包含 context_length 时能被识别。
    @Test
    // 方法名描述测试意图：识别常见上下文超限错误。
    void shouldDetectContextLengthError() {
        // 创建默认策略对象。
        ChatMemoryPolicy policy = new ChatMemoryPolicy(20, 4);
        // 创建一个模拟模型上下文超限异常。
        RuntimeException exception = new RuntimeException("context_length exceeded");

        // 验证策略能识别该异常。
        assertThat(policy.isContextLengthExceeded(exception)).isTrue();
    }

    // 测试被包装在 cause 里的上下文超限异常也能识别。
    @Test
    // 方法名描述测试意图：识别异常链中的上下文超限错误。
    void shouldDetectContextLengthErrorInCauseChain() {
        // 创建默认策略对象。
        ChatMemoryPolicy policy = new ChatMemoryPolicy(20, 4);
        // 创建底层上下文超限异常。
        RuntimeException cause = new RuntimeException("maximum context length is 8192 tokens");
        // 创建外层包装异常。
        RuntimeException wrapper = new RuntimeException("model call failed", cause);

        // 验证策略会沿着 cause 链继续检查。
        assertThat(policy.isContextLengthExceeded(wrapper)).isTrue();
    }

    // 测试上下文超限提示不再声称已经清空记忆。
    @Test
    // 方法名描述测试意图：降级文案要诚实，不误导用户。
    void shouldNotClaimMemoryWasClearedInFallbackMessage() {
        // 创建默认策略对象。
        ChatMemoryPolicy policy = new ChatMemoryPolicy(20, 4);

        // 获取上下文超限降级提示。
        String message = policy.contextLengthFallbackMessage();

        // 验证提示明确表达这是刚才请求失败后的兜底，而不是模型提前预判。
        assertThat(message).contains("刚才这次请求");
        // 验证提示说明了无法携带全部历史上下文。
        assertThat(message).contains("无法继续携带全部历史上下文");
        // 验证提示没有说“已清空记忆”。
        assertThat(message).doesNotContain("已清空记忆");
        // 验证提示引导用户补充关键背景。
        assertThat(message).contains("补充关键背景");
    }
}
