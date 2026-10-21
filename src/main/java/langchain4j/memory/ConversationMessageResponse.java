// 声明当前类所在的包，memory 表示它属于会话记忆模块。
package langchain4j.memory;

// AiMessage 表示 LangChain4j 中的大模型回复消息。
import dev.langchain4j.data.message.AiMessage;
// ChatMessage 表示 LangChain4j 中所有聊天消息的统一接口。
import dev.langchain4j.data.message.ChatMessage;
// ChatMessageType 表示消息类型枚举。
import dev.langchain4j.data.message.ChatMessageType;
// Content 表示用户消息里可能出现的文本、图片等多模态内容。
import dev.langchain4j.data.message.Content;
// SystemMessage 表示系统提示词消息。
import dev.langchain4j.data.message.SystemMessage;
// TextContent 表示用户消息中的文本内容块。
import dev.langchain4j.data.message.TextContent;
// ToolExecutionResultMessage 表示工具执行结果消息。
import dev.langchain4j.data.message.ToolExecutionResultMessage;
// UserMessage 表示用户发送的消息。
import dev.langchain4j.data.message.UserMessage;
// StringUtils 提供字符串判空工具。
import org.springframework.util.StringUtils;

// 定义前端展示聊天历史时使用的响应 DTO。
public class ConversationMessageResponse {

    // role 表示前端气泡角色，当前主要使用 assistant 和 user。
    private final String role;

    // content 表示前端气泡展示的文本内容。
    private final String content;

    // ragContext 表示从 LangChain4j 增强用户消息中剥离出的 RAG 来源上下文。
    private final String ragContext;

    // timestamp 表示前端生成稳定 DOM ID 时使用的序号型时间戳。
    private final long timestamp;

    // 构造一个会话消息响应对象。
    public ConversationMessageResponse(
            // 前端消息角色。
            String role,
            // 前端消息内容。
            String content,
            // 前端使用的稳定序号。
            long timestamp
    ) {
        // 委托完整构造器。
        this(role, content, null, timestamp);
    }

    // 构造一个带 RAG 上下文的会话消息响应对象。
    public ConversationMessageResponse(
            // 前端消息角色。
            String role,
            // 前端消息内容。
            String content,
            // 从内部 prompt 中剥离的 RAG 上下文。
            String ragContext,
            // 前端使用的稳定序号。
            long timestamp
    ) {
        // 保存前端消息角色。
        this.role = role;
        // 保存前端消息内容。
        this.content = content;
        // 保存 RAG 上下文。
        this.ragContext = ragContext;
        // 保存前端使用的稳定序号。
        this.timestamp = timestamp;
    }

    // 把 LangChain4j 原生消息转换成前端响应对象。
    public static ConversationMessageResponse from(
            // LangChain4j 原生聊天消息。
            ChatMessage message,
            // 当前消息在历史列表中的顺序。
            int index
    ) {
        // 根据消息类型转换前端角色。
        String role = roleOf(message);
        // 根据消息具体类型提取可展示文本。
        String rawContent = contentOf(message);
        // 提取用户原始问题，避免把 RAG 增强 prompt 当作用户消息展示。
        String content = displayContentOf(message, rawContent);
        // 提取 RAG 来源上下文，前端会挂到后续助手消息下折叠展示。
        String ragContext = ragContextOf(message, rawContent);
        // 使用序号生成稳定 timestamp，避免每次刷新历史时 DOM ID 完全变化。
        long timestamp = index + 1L;
        // 返回前端响应对象。
        return new ConversationMessageResponse(role, content, ragContext, timestamp);
    }

    // 判断 LangChain4j 原生消息是否适合返回给前端展示。
    public static boolean isDisplayable(ChatMessage message) {
        // 空消息不展示。
        if (message == null) {
            return false;
        }
        // 系统提示词永远不返回前端。
        if (message instanceof SystemMessage) {
            return false;
        }
        // 工具结果属于内部编排细节，不直接展示。
        if (message instanceof ToolExecutionResultMessage) {
            return false;
        }
        // 只展示用户和 AI 的自然语言消息。
        if (!(message instanceof UserMessage) && !(message instanceof AiMessage)) {
            return false;
        }
        // 防御性过滤：即使系统提示词被错误写成普通消息，也不能泄露到前端。
        String content = contentOf(message);
        return !looksLikeInternalPrompt(content);
    }

    // 根据 LangChain4j 消息类型映射前端角色。
    private static String roleOf(ChatMessage message) {
        // 空消息兜底为助手消息，避免前端无法渲染。
        if (message == null) {
            // 返回助手角色。
            return "assistant";
        }
        // 用户消息映射为 user。
        if (ChatMessageType.USER.equals(message.type())) {
            // 返回用户角色。
            return "user";
        }
        // 其他系统、AI、工具消息统一映射为 assistant，避免泄露内部实现细节。
        return "assistant";
    }

    // 从原始消息中提取最终可展示内容。
    private static String displayContentOf(ChatMessage message, String rawContent) {
        // 只有用户消息可能被 LangChain4j RAG 增强为完整 prompt。
        if (message instanceof UserMessage) {
            // 查找增强 prompt 标记。
            int markerIndex = ragMarkerIndex(rawContent);
            // 如果存在标记，只展示标记前的原始用户问题。
            if (markerIndex >= 0) {
                return safe(rawContent.substring(0, markerIndex)).trim();
            }
        }
        // 其他消息直接展示原始文本。
        return safe(rawContent);
    }

    // 从用户增强 prompt 中提取 RAG 上下文。
    private static String ragContextOf(ChatMessage message, String rawContent) {
        // 只有用户消息中才会携带 LangChain4j 注入的 RAG 上下文。
        if (!(message instanceof UserMessage)) {
            return null;
        }
        // 查找增强 prompt 标记。
        int markerIndex = ragMarkerIndex(rawContent);
        // 没有标记时没有 RAG 上下文。
        if (markerIndex < 0) {
            return null;
        }
        // 截取标记之后的上下文。
        String context = rawContent.substring(markerIndex);
        // 去掉框架注入的英文说明，只保留来源信息本身。
        context = context.replaceFirst("(?is)^\\s*Answer using the following information:\\s*", "");
        // 返回清理后的上下文。
        return StringUtils.hasText(context) ? context.trim() : null;
    }

    // 查找 LangChain4j RAG 增强 prompt 的起点。
    private static int ragMarkerIndex(String content) {
        // 空内容不包含标记。
        if (content == null) {
            return -1;
        }
        // 当前 LangChain4j 默认英文标记。
        return content.indexOf("Answer using the following information:");
    }

    // 判断内容是否像内部系统提示词或完整模型 prompt。
    private static boolean looksLikeInternalPrompt(String content) {
        // 空内容交给上层按空白过滤。
        if (!StringUtils.hasText(content)) {
            return false;
        }
        // 规整文本。
        String normalized = content.trim();
        // 系统提示词标题或核心身份说明都不应出现在前端。
        return normalized.startsWith("# 医疗小智系统提示词")
                || normalized.contains("你是“医疗小智”")
                || normalized.contains("你不是医生，不能替代医生诊断");
    }

    // 从 LangChain4j 消息对象中提取文本内容。
    private static String contentOf(ChatMessage message) {
        // 空消息返回空文本。
        if (message == null) {
            // 返回空字符串。
            return "";
        }
        // AI 消息直接读取 text。
        if (message instanceof AiMessage aiMessage) {
            // 返回 AI 回复文本，空值兜底为空字符串。
            return safe(aiMessage.text());
        }
        // 用户消息可能是单文本，也可能是多模态内容列表。
        if (message instanceof UserMessage userMessage) {
            // 返回用户消息中的文本部分。
            return userText(userMessage);
        }
        // 系统消息读取 text。
        if (message instanceof SystemMessage systemMessage) {
            // 返回系统消息文本。
            return safe(systemMessage.text());
        }
        // 工具执行结果读取 text。
        if (message instanceof ToolExecutionResultMessage toolMessage) {
            // 返回工具执行结果文本。
            return safe(toolMessage.text());
        }
        // 未知消息类型使用 toString 兜底，保证接口不因为新类型崩溃。
        return safe(message.toString());
    }

    // 提取用户消息中的文本内容。
    private static String userText(UserMessage userMessage) {
        // 单文本用户消息直接返回 singleText。
        if (userMessage.hasSingleText()) {
            // 返回单文本内容。
            return safe(userMessage.singleText());
        }
        // 创建字符串构造器，用来拼接多个文本块。
        StringBuilder builder = new StringBuilder();
        // 遍历用户消息中的多模态内容块。
        for (Content content : userMessage.contents()) {
            // 当前内容块是文本时才加入前端聊天历史。
            if (content instanceof TextContent textContent) {
                // 如果前面已有文本，就加一个换行分隔。
                if (builder.length() > 0) {
                    // 追加换行。
                    builder.append('\n');
                }
                // 追加文本块内容。
                builder.append(textContent.text());
            }
        }
        // 返回拼接后的文本。
        return builder.toString();
    }

    // 把 null 字符串规整为空字符串。
    private static String safe(String value) {
        // value 为 null 时返回空字符串，否则返回原值。
        return value == null ? "" : value;
    }

    // 返回前端消息角色。
    public String getRole() {
        // 返回 role 字段。
        return role;
    }

    // 返回前端消息内容。
    public String getContent() {
        // 返回 content 字段。
        return content;
    }

    // 返回 RAG 上下文。
    public String getRagContext() {
        // 返回 ragContext 字段。
        return ragContext;
    }

    // 返回前端消息稳定序号。
    public long getTimestamp() {
        // 返回 timestamp 字段。
        return timestamp;
    }
}
