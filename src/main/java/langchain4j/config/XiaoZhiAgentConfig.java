// 声明当前类所在的包，config 表示它负责 Spring Bean 配置。
package langchain4j.config;

// ChatMemoryPolicy 是对话记忆窗口策略。
import langchain4j.memory.ChatMemoryPolicy;
import langchain4j.memory.OrderedWorkerChatMemory;
// MedicalKnowledgeRetriever 是项目统一医学知识检索接口。
import langchain4j.rag.MedicalKnowledgeRetriever;
// MedicalKnowledgeSearchHit 表示一条医学知识命中。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// MedicalKnowledgeSearchRequest 表示医学知识检索请求。
import langchain4j.rag.MedicalKnowledgeSearchRequest;
// RetrievalMode 表示关键词、向量或混合检索模式。
import langchain4j.rag.RetrievalMode;
// MongoChatMemoryStore 是自定义 MongoDB 聊天记忆存储。
import langchain4j.store.MongoChatMemoryStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
// ChatMemoryProvider 是 LangChain4j 用来按 memoryId 创建聊天记忆的接口。
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.ChatMemory;
// MessageWindowChatMemory 是 LangChain4j 提供的窗口型短期记忆实现。
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
// Content 是 LangChain4j Agent 可消费的 RAG 内容对象。
import dev.langchain4j.rag.content.Content;
// ContentRetriever 是 LangChain4j RAG 检索器接口。
import dev.langchain4j.rag.content.retriever.ContentRetriever;
// List 表示一组 Agent RAG 内容。
import java.util.List;
// Bean 注解表示把方法返回值注册为 Spring Bean。
import org.springframework.context.annotation.Bean;
// Configuration 注解表示这是 Spring 配置类。
import org.springframework.context.annotation.Configuration;

// 标记当前类为 Spring 配置类。
@Configuration
// 定义 XiaoZhi Agent 相关 Bean 的配置。
public class XiaoZhiAgentConfig {

    // 保存 MongoDB 聊天记忆存储依赖。
    private final MongoChatMemoryStore mongoChatMemoryStore;

    // 保存统一医学知识检索器依赖。
    private final MedicalKnowledgeRetriever medicalKnowledgeRetriever;

    // 保存聊天记忆策略依赖。
    private final ChatMemoryPolicy chatMemoryPolicy;

    private final ObjectMapper objectMapper;

    // 构造器注入所有依赖，让配置类的依赖关系更清晰。
    public XiaoZhiAgentConfig(
            // Spring 注入 MongoChatMemoryStore。
            MongoChatMemoryStore mongoChatMemoryStore,
            // Spring 注入新的 Elasticsearch 统一医学知识检索器。
            MedicalKnowledgeRetriever medicalKnowledgeRetriever,
            // Spring 注入聊天记忆策略。
            ChatMemoryPolicy chatMemoryPolicy,
            ObjectMapper objectMapper
    ) {
        // 保存 MongoDB 聊天记忆存储。
        this.mongoChatMemoryStore = mongoChatMemoryStore;
        // 保存医学知识检索器。
        this.medicalKnowledgeRetriever = medicalKnowledgeRetriever;
        // 保存聊天记忆策略。
        this.chatMemoryPolicy = chatMemoryPolicy;
        this.objectMapper = objectMapper;
    }

    // 注册 XiaoZhi Agent 使用的聊天记忆提供器。
    @Bean
    // ChatMemoryProvider 会根据不同 memoryId 创建彼此隔离的对话记忆。
    ChatMemoryProvider chatMemoryProviderXiaoZhi() {
        // 返回一个 lambda；LangChain4j 每次拿到 memoryId 时会调用它创建 ChatMemory。
        return memoryId -> MessageWindowChatMemory
                // 使用 builder 创建窗口型聊天记忆。
                .builder()
                // 设置当前聊天记忆的唯一 ID。
                .id(memoryId)
                // 设置短期窗口最多保留多少条消息。
                .maxMessages(chatMemoryPolicy.maxWindowMessages())
                // 设置底层持久化存储为 MongoDB。
                .chatMemoryStore(mongoChatMemoryStore)
                // 构建 MessageWindowChatMemory 实例。
                .build();
    }

    @Bean
    ChatMemoryProvider hospitalWorkerMemoryProvider() {
        return memoryId -> workerMemory(memoryId);
    }

    @Bean
    ChatMemoryProvider ragWorkerMemoryProvider() {
        return memoryId -> workerMemory(memoryId);
    }

    @Bean
    ChatMemoryProvider generalWorkerMemoryProvider() {
        return memoryId -> workerMemory(memoryId);
    }

    private ChatMemory workerMemory(Object memoryId) {
        return new OrderedWorkerChatMemory(memoryId, chatMemoryPolicy.maxWindowMessages(), mongoChatMemoryStore);
    }

    // 注册 XiaoZhi Agent 使用的 Elasticsearch RAG 内容检索器。
    @Bean
    // ContentRetriever 会在用户提问时从 Elasticsearch 混合检索相关知识片段。
    ContentRetriever contentRetrieverXiaozhiElasticsearch() {
        // 返回 LangChain4j 需要的内容检索 lambda。
        return query -> {
            // 创建项目内部医学知识检索请求。
            MedicalKnowledgeSearchRequest request = new MedicalKnowledgeSearchRequest();
            // 设置用户问题文本。
            request.setQuery(extractCurrentUserMessage(query.text()));
            // Agent 默认使用混合检索，底层由 Elasticsearch 融合关键词分数和向量相似度。
            request.setRetrievalMode(RetrievalMode.HYBRID);
            // 限制返回条数，避免注入过长上下文。
            request.setTopK(3);
            // 执行检索并转换为 LangChain4j Content。
            return medicalKnowledgeRetriever.search(request)
                    // 读取命中列表。
                    .getHits()
                    // 转成 stream 方便映射。
                    .stream()
                    // 把项目命中转换为 Agent 可消费文本。
                    .map(this::toContent)
                    // 收集为列表。
                    .toList();
        };
    }

    // 把项目检索命中转换成 LangChain4j RAG 内容。
    String extractCurrentUserMessage(String text) {
        if (text == null || !text.trim().startsWith("{")) {
            if (text != null && text.startsWith("当前用户消息：")) {
                String current = text.substring("当前用户消息：".length());
                int historyMarker = current.indexOf("\n主会话记忆（只读）：");
                return (historyMarker >= 0 ? current.substring(0, historyMarker) : current).trim();
            }
            return text;
        }
        try {
            JsonNode root = objectMapper.readTree(text);
            String currentMessage = root.path("conversation").path("currentUserMessage").asText();
            return currentMessage.isBlank() ? text : currentMessage;
        } catch (Exception ignored) {
            return text;
        }
    }

    private Content toContent(MedicalKnowledgeSearchHit hit) {
        // 创建包含标题、片段和来源的上下文文本。
        String text = "标题：" + nullToEmpty(hit.getTitle()) + "\n"
                + "内容：" + nullToEmpty(hit.getSnippet()) + "\n"
                + "来源：" + nullToEmpty(hit.getSourceName()) + " " + nullToEmpty(hit.getSourceUrl());
        // 返回 LangChain4j Content。
        return Content.from(text);
    }

    // 把 null 转为空字符串，避免 Agent 上下文出现 null 字样。
    private String nullToEmpty(String value) {
        // 如果值为空，返回空字符串。
        if (value == null) {
            // 返回空字符串。
            return "";
        }
        // 返回原值。
        return value;
    }
}
