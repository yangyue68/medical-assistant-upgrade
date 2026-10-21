// 声明当前类所在的包，application 表示它属于 RAG 应用编排层。
package langchain4j.rag.application;

// ChatLanguageModel 是 LangChain4j 的非流式聊天模型抽象。
import dev.langchain4j.model.chat.ChatLanguageModel;

// 定义在线模型 RAG 回答生成器。
public class OnlineMedicalRagAnswerGenerator implements MedicalRagAnswerGenerator {

    // 保存在线聊天模型依赖。
    private final ChatLanguageModel chatLanguageModel;

    // 创建在线模型 RAG 回答生成器。
    public OnlineMedicalRagAnswerGenerator(
            // 注入 LangChain4j 聊天模型，当前运行时通常是 DashScope QwenChatModel。
            ChatLanguageModel chatLanguageModel
    ) {
        // 如果模型为空，说明配置没有提供在线模型 Bean。
        if (chatLanguageModel == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("chatLanguageModel must not be null.");
        }
        // 保存在线模型依赖。
        this.chatLanguageModel = chatLanguageModel;
    }

    // 根据 prompt 和 RAG 上下文调用在线模型生成回答。
    @Override
    public String generate(String prompt, MedicalRagContext context) {
        // 校验 prompt 不能为空。
        validatePrompt(prompt);
        // 校验上下文不能为空。
        validateContext(context);
        // 调用在线模型生成回答。
        //注意，prompt是MedicalRagPromptBuilder从MedicalRagContext组装出来的，prompt实际上已经包含context了
        String answer = chatLanguageModel.chat(prompt);
        // 校验模型返回不能是空文本。
        validateAnswer(answer);
        // 返回去掉首尾空白后的回答。
        return answer.trim();
    }

    // 校验 prompt。
    private void validatePrompt(String prompt) {
        // 如果 prompt 为空，说明调用方没有先构建模型输入。
        if (prompt == null || prompt.trim().isEmpty()) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("prompt must not be blank.");
        }
    }

    // 校验 RAG 上下文。
    private void validateContext(MedicalRagContext context) {
        // 如果上下文为空，说明调用方没有先完成检索和上下文构建。
        if (context == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("context must not be null.");
        }
    }

    // 校验模型回答。
    private void validateAnswer(String answer) {
        // 如果模型返回空文本，就不能把它当作有效医学回答。
        if (answer == null || answer.trim().isEmpty()) {
            // 抛出非法状态异常。
            throw new IllegalStateException("Online RAG answer generator returned blank answer.");
        }
    }
}
