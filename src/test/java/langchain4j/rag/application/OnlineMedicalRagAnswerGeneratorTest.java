// 声明测试类所在的包。
package langchain4j.rag.application;

// RetrievalMode 表示检索模式。
import langchain4j.rag.RetrievalMode;
// ChatLanguageModel 是 LangChain4j 的聊天模型抽象。
import dev.langchain4j.model.chat.ChatLanguageModel;
// List 表示一组命中或引用。
import java.util.List;
// AtomicReference 用来记录 fake 模型收到的 prompt。
import java.util.concurrent.atomic.AtomicReference;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// AssertJ 异常断言工具。
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 定义在线 RAG 回答生成器测试。
class OnlineMedicalRagAnswerGeneratorTest {

    // 测试在线生成器会把 prompt 交给聊天模型。
    @Test
    // 方法名描述测试意图：在线生成器本身只负责模型调用，不负责重新检索。
    void shouldGenerateAnswerByCallingChatModelWithPrompt() {
        // 创建一个引用，用来记录 fake 模型收到的 prompt。
        AtomicReference<String> receivedPrompt = new AtomicReference<>();
        // 创建 fake 聊天模型。
        ChatLanguageModel chatLanguageModel = new ChatLanguageModel() {
            // 覆盖 chat(String) 方法，避免测试访问真实在线模型。
            @Override
            public String chat(String userMessage) {
                // 记录模型收到的 prompt。
                receivedPrompt.set(userMessage);
                // 返回 fake 模型回答。
                return "模型生成的带引用回答 [1] ";
            }
        };
        // 创建在线 RAG 回答生成器。
        OnlineMedicalRagAnswerGenerator generator = new OnlineMedicalRagAnswerGenerator(chatLanguageModel);

        // 调用生成器生成回答。
        String answer = generator.generate("测试 prompt", context());

        // 验证返回值来自模型，并且已经去掉首尾空白。
        assertThat(answer).isEqualTo("模型生成的带引用回答 [1]");
        // 验证模型收到的输入就是 prompt。
        assertThat(receivedPrompt.get()).isEqualTo("测试 prompt");
    }

    // 测试空模型返回会被拒绝。
    @Test
    // 方法名描述测试意图：在线模型返回空内容时不能当作有效医学回答。
    void shouldRejectBlankModelAnswer() {
        // 创建始终返回空白文本的 fake 模型。
        ChatLanguageModel chatLanguageModel = new ChatLanguageModel() {
            // 覆盖 chat(String) 方法。
            @Override
            public String chat(String userMessage) {
                // 返回空白文本。
                return " ";
            }
        };
        // 创建在线 RAG 回答生成器。
        OnlineMedicalRagAnswerGenerator generator = new OnlineMedicalRagAnswerGenerator(chatLanguageModel);

        // 验证空回答会抛出非法状态异常。
        assertThatThrownBy(() -> generator.generate("测试 prompt", context()))
                // 验证异常类型。
                .isInstanceOf(IllegalStateException.class)
                // 验证异常信息。
                .hasMessageContaining("blank answer");
    }

    // 测试空模型依赖会被拒绝。
    @Test
    // 方法名描述测试意图：在线生成器必须显式注入模型。
    void shouldRejectNullChatModel() {
        // 验证构造器拒绝空模型。
        assertThatThrownBy(() -> new OnlineMedicalRagAnswerGenerator(null))
                // 验证异常类型。
                .isInstanceOf(IllegalArgumentException.class)
                // 验证异常信息。
                .hasMessageContaining("chatLanguageModel");
    }

    // 创建测试 RAG 上下文。
    private static MedicalRagContext context() {
        // 返回一个最小上下文，在线生成器只需要确认上下文存在。
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
