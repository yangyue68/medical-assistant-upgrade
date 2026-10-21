// 声明当前类所在的包，store 表示它负责适配外部存储。
package langchain4j.store;

// ChatMessages 是 MongoDB 中保存聊天记忆的文档实体。
import langchain4j.bean.ChatMessages;
import langchain4j.multiagent.MultiAgentConversationState;
// ChatMessage 是 LangChain4j 对用户消息、AI 消息、工具消息的统一抽象。
import dev.langchain4j.data.message.ChatMessage;
// ChatMessageDeserializer 用来把 MongoDB 中的 JSON 反序列化成 ChatMessage 列表。
import dev.langchain4j.data.message.ChatMessageDeserializer;
// ChatMessageSerializer 用来把 ChatMessage 列表序列化成 JSON 存入 MongoDB。
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
// UserMessage 用来识别并清理旧版 Worker 交接请求。
import dev.langchain4j.data.message.UserMessage;
// ChatMemoryStore 是 LangChain4j 要求实现的聊天记忆存储接口。
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
// LinkedList 用来在没有历史消息时返回一个可变空列表。
import java.util.LinkedList;
// List 表示一组聊天消息。
import java.util.List;
// Logger 是 SLF4J 的日志接口。
import org.slf4j.Logger;
// LoggerFactory 用来创建当前类对应的日志对象。
import org.slf4j.LoggerFactory;
// MongoTemplate 是 Spring Data MongoDB 的核心操作对象。
import org.springframework.data.mongodb.core.MongoTemplate;
// Criteria 用来构造 MongoDB 查询条件。
import org.springframework.data.mongodb.core.query.Criteria;
// Query 用来封装 MongoDB 查询条件。
import org.springframework.data.mongodb.core.query.Query;
// Update 用来描述 MongoDB 更新操作。
import org.springframework.data.mongodb.core.query.Update;
// Component 表示把当前类注册成 Spring Bean。
import org.springframework.stereotype.Component;

// 标记当前类为 Spring 组件，让 LangChain4j 记忆配置可以注入它。
@Component
// 定义 MongoDB 聊天记忆存储，实现 LangChain4j 的 ChatMemoryStore 接口。
public class MongoChatMemoryStore implements ChatMemoryStore {

    // 创建当前类的日志对象，替代 System.out.println。
    private static final Logger log = LoggerFactory.getLogger(MongoChatMemoryStore.class);

    // 保存 MongoDB 操作对象，用于读写 chat_messages 集合。
    private final MongoTemplate mongoTemplate;

    // 构造器注入 MongoTemplate，让依赖不可变并便于单元测试。
    public MongoChatMemoryStore(
            // Spring 会自动注入 MongoTemplate Bean。
            MongoTemplate mongoTemplate
    ) {
        // 把构造参数保存到成员变量。
        this.mongoTemplate = mongoTemplate;
    }

    // 根据 memoryId 读取该会话的聊天记忆。
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // 记录调试日志，避免生产环境输出大量聊天内容。
        log.debug("Loading chat memory. memoryId={}", memoryId);
        // 创建按 memoryId 精确匹配的查询条件。
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        // 用查询条件构造 MongoDB 查询对象。
        Query query = new Query(criteria);
        // 从 MongoDB 中读取该 memoryId 对应的一条聊天记忆文档。
        ChatMessages one = mongoTemplate.findOne(query, ChatMessages.class);
        // 如果没有查到文档，说明该会话还没有历史消息。
        if (one == null) {
            // 返回空列表，让 LangChain4j 可以从空记忆开始对话。
            return new LinkedList<>();
        }
        // 把 JSON 字符串反序列化成 LangChain4j 可识别的 ChatMessage 列表。
        List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(one.getContent());
        // Worker 读取时移除旧系统提示和旧交接信封，兼容已经写入 MongoDB 的历史数据。
        return sanitizeWorkerMemory(memoryId, messages);
    }

    // 更新指定 memoryId 的完整聊天记忆。
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        // 记录更新动作，只记录数量，不记录原文，避免泄露用户隐私。
        log.debug("Updating chat memory. memoryId={}, messageCount={}", memoryId, list.size());
        // 创建按 memoryId 精确匹配的查询条件。
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        // 用查询条件构造 MongoDB 查询对象。
        Query query = new Query(criteria);
        // 创建 MongoDB 更新对象。
        Update update = new Update();
        // 写入 memoryId 字段，保证首次 upsert 新文档时也带有会话 ID。
        update.set("memoryId", memoryId);
        // 把完整消息列表序列化成 JSON 后写入 content 字段。
        update.set("content", ChatMessageSerializer.messagesToJson(messagesToPersist(memoryId, list)));
        // 使用 upsert：存在则更新，不存在则插入。
        mongoTemplate.upsert(query, update, ChatMessages.class);
        // 记录更新完成日志，方便排查记忆写入是否发生。
        log.debug("Chat memory updated. memoryId={}, messageCount={}", memoryId, list.size());
    }

    // 删除指定 memoryId 的聊天记忆。
    @Override
    public void deleteMessages(Object memoryId) {
        // 记录删除动作，但不记录任何聊天内容。
        log.info("Deleting chat memory. memoryId={}", memoryId);
        // 创建按 memoryId 精确匹配的查询条件。
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        // 用查询条件构造 MongoDB 查询对象。
        Query query = new Query(criteria);
        // 从 MongoDB 中删除该会话的聊天记忆文档。
        mongoTemplate.remove(query, ChatMessages.class);
    }

    public void deleteMultiAgentState(Long conversationId) {
        Query query = new Query(Criteria.where("_id").is(conversationId));
        mongoTemplate.remove(query, MultiAgentConversationState.class);
    }

    private List<ChatMessage> messagesToPersist(Object memoryId, List<ChatMessage> messages) {
        return sanitizeWorkerMemory(memoryId, messages);
    }

    private List<ChatMessage> sanitizeWorkerMemory(Object memoryId, List<ChatMessage> messages) {
        if (!(memoryId instanceof String id) || !id.contains(":worker:") || messages == null) {
            return messages == null ? List.of() : messages;
        }
        return messages.stream()
                .filter(message -> !(message instanceof SystemMessage))
                .filter(message -> !(message instanceof UserMessage user) || !isLegacyWorkerInput(user.singleText()))
                .toList();
    }

    private boolean isLegacyWorkerInput(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.trim();
        return normalized.contains("主会话记忆（只读）：")
                || (normalized.startsWith("{")
                && normalized.contains("\"conversation\"")
                && normalized.contains("\"instructions\""));
    }
}
