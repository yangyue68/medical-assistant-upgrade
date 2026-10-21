// 声明当前类所在的包，memory 表示它负责对话记忆相关能力。
package langchain4j.memory;

// MongoChatMemoryStore 是当前项目保存 LangChain4j 聊天记忆的 MongoDB 适配器。
import langchain4j.store.MongoChatMemoryStore;
// ChatMessage 是 LangChain4j 聊天记忆中的原生消息对象。
import dev.langchain4j.data.message.ChatMessage;
// Instant 表示会话创建和更新时间。
import java.time.Instant;
// List 表示一组会话元数据。
import java.util.List;
// AtomicLong 用来在单个应用进程内生成单调递增的会话 ID。
import java.util.concurrent.atomic.AtomicLong;
// Logger 是 SLF4J 的日志接口。
import org.slf4j.Logger;
// LoggerFactory 用来创建当前类对应的日志对象。
import org.slf4j.LoggerFactory;
// Service 表示把当前类注册成 Spring 服务组件。
import org.springframework.stereotype.Service;

// 标记当前类为 Spring Service，让 Controller 可以通过构造器注入它。
@Service
// 定义对话记忆服务，集中承接“新对话、清空对话、未来摘要记忆”等业务动作。
public class ConversationMemoryService {

    // 创建当前类的日志对象，用于记录记忆管理动作。
    private static final Logger log = LoggerFactory.getLogger(ConversationMemoryService.class);

    // 保存进程内会话 ID 序列，避免同一毫秒内创建多个会话时生成重复 ID。
    private final AtomicLong conversationIdSequence = new AtomicLong(System.currentTimeMillis());

    // 保存底层聊天记忆存储依赖，当前实现是 MongoDB。
    private final MongoChatMemoryStore memoryStore;

    // 保存会话元数据存储依赖。
    private final ConversationSessionStore sessionStore;

    // 构造器注入 MongoChatMemoryStore，让依赖显式、不可变、方便单元测试。
    public ConversationMemoryService(
            // Spring 会自动注入 MongoChatMemoryStore Bean。
            MongoChatMemoryStore memoryStore,
            // Spring 会自动注入 ConversationSessionStore Bean。
            ConversationSessionStore sessionStore
    ) {
        // 把构造参数保存到成员变量。
        this.memoryStore = memoryStore;
        // 保存会话元数据存储。
        this.sessionStore = sessionStore;
    }

    // 判断用户输入是否是开启新对话命令。
    public boolean isNewConversationCommand(String message) {
        // 只有“新对话”表示创建新会话窗口，不表示删除旧会话历史。
        return "新对话".equals(message);
    }

    // 判断用户输入是否是清空当前会话历史的命令。
    public boolean isClearMemoryCommand(String message) {
        // 兼容“清空记录”和“清空对话”两种删除历史的用户表达。
        return "清空记录".equals(message) || "清空对话".equals(message);
    }

    // 创建一个新的对话 ID。
    public Long createNewConversationId() {
        // 读取当前毫秒时间戳，保证生成的 ID 大体具备时间递增特征。
        long now = System.currentTimeMillis();
        // 在当前进程内生成严格递增的 ID，避免高频点击“新对话”时发生同毫秒碰撞。
        return conversationIdSequence.updateAndGet(current -> Math.max(current + 1, now));
    }

    // 创建新的会话元数据。
    public ConversationSession createConversation(String title) {
        // 创建新的业务会话 ID。
        Long memoryId = createNewConversationId();
        // 获取当前时间，作为创建和更新时间。
        Instant now = Instant.now();
        // 创建会话元数据对象。
        ConversationSession session = ConversationSession.of(memoryId, normalizeTitle(title), now, now);
        // 保存会话元数据。
        return sessionStore.save(session);
    }

    public ConversationSession createConversation(Long userId, String title) {
        if (userId == null) throw new IllegalArgumentException("userId must not be null.");
        Long memoryId = createNewConversationId();
        Instant now = Instant.now();
        return sessionStore.save(ConversationSession.of(userId, memoryId, normalizeTitle(title), now, now));
    }

    // 查询所有会话元数据。
    public List<ConversationSession> listConversations() {
        // 委托会话存储查询列表。
        return sessionStore.findAll();
    }

    public List<ConversationSession> listConversations(Long userId) {
        if (userId == null) throw new IllegalArgumentException("userId must not be null.");
        return sessionStore.findAllByUserId(userId);
    }

    // 查询指定会话的聊天历史。
    public List<ConversationMessageResponse> listConversationMessages(Long memoryId) {
        // 校验 memoryId 不能为空。
        validateMemoryId(memoryId);
        // 从底层聊天记忆存储读取 LangChain4j 原生消息列表。
        List<ChatMessage> messages = memoryStore.getMessages(memoryId);
        // 创建前端响应列表。
        java.util.ArrayList<ConversationMessageResponse> responses = new java.util.ArrayList<>();
        // 遍历原生消息列表。
        for (int i = 0; i < messages.size(); i++) {
            // 系统提示词、工具结果和内部 prompt 不返回给前端。
            if (!ConversationMessageResponse.isDisplayable(messages.get(i))) {
                // 继续下一条。
                continue;
            }
            // 转换并添加当前消息响应。
            responses.add(ConversationMessageResponse.from(messages.get(i), i));
        }
        // 返回前端可直接渲染的消息列表。
        return responses;
    }

    public List<ConversationMessageResponse> listConversationMessages(Long userId, Long memoryId) {
        requireOwnedSession(userId, memoryId);
        return listConversationMessages(memoryId);
    }

    // 更新会话标题。
    public ConversationSession updateConversationTitle(Long memoryId, String title) {
        // 校验 memoryId 不能为空。
        validateMemoryId(memoryId);
        // 查询已有会话元数据。
        ConversationSession session = sessionStore.findByMemoryId(memoryId)
                // 找不到会话时抛出异常，避免静默创建脏数据。
                .orElseThrow(() -> new IllegalArgumentException("Conversation session does not exist."));
        // 设置规整后的标题。
        session.setTitle(normalizeTitle(title));
        // 更新时间刷新为当前时间。
        session.setUpdatedAt(Instant.now());
        // 保存并返回更新后的会话元数据。
        return sessionStore.save(session);
    }

    public ConversationSession updateConversationTitle(Long userId, Long memoryId, String title) {
        requireOwnedSession(userId, memoryId);
        return updateConversationTitle(memoryId, title);
    }

    // 刷新会话更新时间。
    public void touchConversation(Long memoryId) {
        // 校验 memoryId 不能为空。
        validateMemoryId(memoryId);
        // 查询已有会话元数据；旧客户端可能没有先创建会话，所以这里不强制存在。
        sessionStore.findByMemoryId(memoryId).ifPresent(session -> {
            // 把更新时间刷新为当前时间。
            session.setUpdatedAt(Instant.now());
            // 保存更新后的会话元数据。
            sessionStore.save(session);
        });
    }

    public void touchConversation(Long userId, Long memoryId) {
        requireOwnedSession(userId, memoryId);
        touchConversation(memoryId);
    }

    // 删除会话元数据和对应聊天记忆。
    public void deleteConversation(Long memoryId) {
        // 校验 memoryId 不能为空。
        validateMemoryId(memoryId);
        // 删除聊天记忆。
        clearConversation(memoryId);
        // 删除会话元数据。
        sessionStore.deleteByMemoryId(memoryId);
    }

    public void deleteConversation(Long userId, Long memoryId) {
        requireOwnedSession(userId, memoryId);
        deleteConversation(memoryId);
    }

    // 返回开启新对话后展示给用户的统一提示文案。
    public String newConversationMessage(Long newMemoryId) {
        // 提示用户旧对话仍然保留，避免把“新对话”误解成“清空历史”。
        return "✅ 已开启新对话，原对话历史已保留。\n\n"
                // 把新的会话 ID 返回给前端或调试用户，当前纯文本流接口先用这种方式承载。
                + "新的会话ID：" + newMemoryId + "\n\n"
                // 引导用户在新会话里提出新的咨询问题。
                + "请直接输入新的咨询问题。";
    }

    // 清空指定 memoryId 对应的对话记忆。
    public void clearConversation(Long memoryId) {
        // 校验 memoryId 不能为空。
        validateMemoryId(memoryId);
        // 记录清空动作，方便后续排查用户会话状态变化。
        log.info("Clearing conversation memory. memoryId={}", memoryId);
        // 调用底层存储删除该会话的全部聊天消息。
        memoryStore.deleteMessages(memoryId);
        // Multi-agent worker memories share the collection but use namespaced ids.
        memoryStore.deleteMessages("conversation:" + memoryId + ":worker:hospital");
        memoryStore.deleteMessages("conversation:" + memoryId + ":worker:rag");
        memoryStore.deleteMessages("conversation:" + memoryId + ":worker:general");
        memoryStore.deleteMultiAgentState(memoryId);
    }

    public void clearConversation(Long userId, Long memoryId) {
        requireOwnedSession(userId, memoryId);
        clearConversation(memoryId);
    }

    private void requireOwnedSession(Long userId, Long memoryId) {
        if (userId == null || memoryId == null
                || sessionStore.findByMemoryIdAndUserId(memoryId, userId).isEmpty()) {
            throw new IllegalArgumentException("Conversation does not belong to the current user.");
        }
    }

    // 返回清空成功后展示给用户的统一提示文案。
    public String clearConversationMessage() {
        // 统一文案避免 Controller、工具或未来 Agent 各自拼接不同提示。
        return "✅ 已清空当前对话记录，该会话历史已从数据库删除。请问有什么可以帮您？";
    }

    // 预留未来摘要记忆入口，当前第 5 天只做接口占位，不调用在线模型。
    public boolean summaryMemoryEnabled() {
        // 当前版本尚未接入摘要记忆，所以明确返回 false。
        return false;
    }

    // 规整会话标题。
    private String normalizeTitle(String title) {
        // 如果前端没有传标题，就使用默认标题。
        if (title == null || title.trim().isEmpty()) {
            // 返回默认标题。
            return "新对话";
        }
        // 去掉标题首尾空白。
        String normalized = title.trim();
        // 如果标题长度不超过 30 个字符，就直接返回。
        if (normalized.length() <= 30) {
            // 返回规整后的标题。
            return normalized;
        }
        // 标题过长时截断，避免前端侧边栏展示失控。
        return normalized.substring(0, 30);
    }

    // 校验 memoryId。
    private void validateMemoryId(Long memoryId) {
        // 如果 memoryId 为空，说明调用方没有指定会话。
        if (memoryId == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("memoryId must not be null.");
        }
    }
}
