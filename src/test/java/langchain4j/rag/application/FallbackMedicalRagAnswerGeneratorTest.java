// 声明测试类所在的包。
package langchain4j.rag.application;

// RetrievalMode 表示检索模式。
import langchain4j.rag.RetrievalMode;
// List 表示一组命中或引用。
import java.util.List;
// AtomicBoolean 用来记录某个生成器是否被调用。
import java.util.concurrent.atomic.AtomicBoolean;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// AssertJ 异常断言工具。
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 定义带兜底能力的 RAG 回答生成器测试。
class FallbackMedicalRagAnswerGeneratorTest {

    // 测试优先生成器成功时不会调用兜底生成器。
    @Test
    // 方法名描述测试意图：正常在线生成成功时应该直接返回在线结果。
    void shouldReturnPrimaryAnswerWhenPrimarySucceeds() {
        // 创建标记，用来记录兜底生成器是否被调用。
        AtomicBoolean fallbackCalled = new AtomicBoolean(false);
        // 创建优先生成器。
        MedicalRagAnswerGenerator primary = (prompt, context) -> "online answer";
        // 创建兜底生成器。
        MedicalRagAnswerGenerator fallback = (prompt, context) -> {
            // 记录兜底生成器被调用。
            fallbackCalled.set(true);
            // 返回兜底回答。
            return "fallback answer";
        };
        // 创建带兜底能力的生成器。
        FallbackMedicalRagAnswerGenerator generator = new FallbackMedicalRagAnswerGenerator(primary, fallback);

        // 生成回答。
        String answer = generator.generate("prompt", context());

        // 验证返回优先生成器的回答。
        assertThat(answer).isEqualTo("online answer");
        // 验证兜底生成器没有被调用。
        assertThat(fallbackCalled.get()).isFalse();
    }

    // 测试优先生成器失败时会调用兜底生成器。
    @Test
    // 方法名描述测试意图：在线模型异常不应该让 RAG answer 接口直接不可用。
    void shouldReturnFallbackAnswerWhenPrimaryFails() {
        // 创建优先生成器，模拟在线模型异常。
        MedicalRagAnswerGenerator primary = (prompt, context) -> {
            // 抛出运行时异常。
            throw new IllegalStateException("online failed");
        };
        // 创建兜底生成器。
        MedicalRagAnswerGenerator fallback = (prompt, context) -> "fallback answer";
        // 创建带兜底能力的生成器。
        FallbackMedicalRagAnswerGenerator generator = new FallbackMedicalRagAnswerGenerator(primary, fallback);

        // 生成回答。
        String answer = generator.generate("prompt", context());

        // 验证返回兜底回答。
        assertThat(answer).isEqualTo("fallback answer");
    }

    // 测试空优先生成器会被拒绝。
    @Test
    // 方法名描述测试意图：构造兜底链路时依赖必须完整。
    void shouldRejectNullPrimaryGenerator() {
        // 验证空优先生成器会抛异常。
        assertThatThrownBy(() -> new FallbackMedicalRagAnswerGenerator(null, (prompt, context) -> "fallback"))
                // 验证异常类型。
                .isInstanceOf(IllegalArgumentException.class)
                // 验证异常信息。
                .hasMessageContaining("primaryGenerator");
    }

    // 创建测试 RAG 上下文。
    private static MedicalRagContext context() {
        // 返回一个最小上下文。
        return MedicalRagContext.of(
                // 用户问题。
                "测试问题",
                // 检索模式。
                RetrievalMode.HYBRID,
                // 上下文文本。
                "测试上下文",
                // 安全提示。
                "不能替代医生诊断",
                // 命中列表。
                List.of(),
                // 引用列表。
                List.of()
        );
    }
}
