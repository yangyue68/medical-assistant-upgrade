// 声明当前类所在的包，memory 表示它负责对话记忆相关策略。
package langchain4j.memory;

// Value 用来从 application.properties 或环境变量中读取配置。
import org.springframework.beans.factory.annotation.Value;
// Component 表示把当前策略类注册成 Spring Bean。
import org.springframework.stereotype.Component;

// 标记为 Spring 组件，让 Controller 和 Agent 配置类都可以注入它。
@Component
// 定义聊天记忆策略类，集中管理上下文窗口和上下文超限处理规则。
public class ChatMemoryPolicy {

    // 保存短期记忆最多保留多少条消息。
    private final int maxWindowMessages;

    // 保存短期记忆最少允许配置多少条消息，避免配置太小导致对话完全不连贯。
    private final int minWindowMessages;

    // 构造方法注入配置值，让这个类创建后就是不可变策略对象。
    public ChatMemoryPolicy(
            // 读取最大短期记忆消息数；默认值是 20。
            @Value("${xiaozhi.chat-memory.max-window-messages:20}") int maxWindowMessages,
            // 读取最小短期记忆消息数；默认值是 4。
            @Value("${xiaozhi.chat-memory.min-window-messages:4}") int minWindowMessages
    ) {
        // 对最小窗口做兜底，保证它至少是 1。
        this.minWindowMessages = Math.max(1, minWindowMessages);
        // 对最大窗口做规整，避免它小于最小窗口。
        this.maxWindowMessages = Math.max(this.minWindowMessages, maxWindowMessages);
    }

    // 返回实际用于 MessageWindowChatMemory 的短期记忆窗口大小。
    public int maxWindowMessages() {
        // 返回构造方法中规整后的最大消息数。
        return maxWindowMessages;
    }

    // 返回最小窗口大小，主要给测试、配置检查或后续管理接口使用。
    public int minWindowMessages() {
        // 返回构造方法中规整后的最小消息数。
        return minWindowMessages;
    }

    // 判断某个异常是否属于模型上下文长度超限。
    public boolean isContextLengthExceeded(Throwable throwable) {
        // 如果异常对象为空，就不可能判断为上下文超限。
        if (throwable == null) {
            // 返回 false 表示不是上下文超限。
            return false;
        }
        // 从异常链中逐层向上检查，因为真实错误可能被包装在 RuntimeException 里面。
        Throwable current = throwable;
        // 只要 current 不为空，就继续检查当前异常节点。
        while (current != null) {
            // 读取当前异常的消息文本。
            String message = current.getMessage();
            // 如果消息命中常见上下文超限关键词，就认为是上下文超限。
            if (containsContextLengthSignal(message)) {
                // 返回 true 表示命中上下文长度超限。
                return true;
            }
            // 移动到下一层 cause，继续检查被包装的原始异常。
            current = current.getCause();
        }
        // 整条异常链都没有命中关键词，返回 false。
        return false;
    }

    // 构造上下文超限异常已经发生后返回给用户的降级提示文案。
    public String contextLengthFallbackMessage() {
        // 返回诚实的用户提示：不假装模型提前感知，只说明刚才请求因上下文过长失败。
        return "⚠️ 刚才这次请求因为对话历史过长，无法继续携带全部历史上下文。\n\n"
                // 说明当前工程版本的处理策略是后续优先保留最近对话，而不是删除所有记忆。
                + "接下来我会优先保留最近几轮对话继续为您服务；如果问题依赖很早之前的信息，"
                // 提醒用户补充关键背景，降低体验损失。
                + "请您简单补充关键背景，例如症状、年龄、过敏史或预约信息。";
    }

    // 判断错误消息中是否包含上下文长度超限的典型信号。
    private boolean containsContextLengthSignal(String message) {
        // 如果消息为空，就无法命中任何关键词。
        if (message == null || message.isEmpty()) {
            // 返回 false 表示没有命中。
            return false;
        }
        // 转成小写，避免不同模型或 SDK 使用不同大小写导致匹配失败。
        String normalizedMessage = message.toLowerCase();
        // 匹配 OpenAI/兼容接口常见的 context_length 错误。
        return normalizedMessage.contains("context_length")
                // 匹配部分模型服务会返回的 maximum context length 文案。
                || normalizedMessage.contains("maximum context length")
                // 匹配中文错误里可能出现的“上下文长度”描述。
                || normalizedMessage.contains("上下文长度")
                // 匹配中文错误里可能出现的“超出上下文”描述。
                || normalizedMessage.contains("超出上下文")
                // 匹配通用 token 超限描述。
                || normalizedMessage.contains("token limit");
    }
}
