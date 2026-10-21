// 声明测试类所在的包。
package langchain4j.memory;

// MongoChatMemoryStore 是 ConversationMemoryService 依赖的底层存储。
import langchain4j.store.MongoChatMemoryStore;
// AiMessage 表示测试中的助手消息。
import dev.langchain4j.data.message.AiMessage;
// SystemMessage 表示测试中的系统提示词消息。
import dev.langchain4j.data.message.SystemMessage;
// UserMessage 表示测试中的用户消息。
import dev.langchain4j.data.message.UserMessage;
// ArgumentCaptor 用来捕获传给 mock 的参数。
import org.mockito.ArgumentCaptor;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// Mockito 的 any 用来匹配任意指定类型参数。
import static org.mockito.ArgumentMatchers.any;
// Mockito 的 mock 用来创建模拟对象。
import static org.mockito.Mockito.mock;
// Mockito 的 verify 用来验证方法调用。
import static org.mockito.Mockito.verify;
// Mockito 的 when 用于指定 mock 方法的返回值。
import static org.mockito.Mockito.when;

// 定义 ConversationMemoryService 的单元测试。
class ConversationMemoryServiceTest {

    // 测试服务能识别“新对话”命令。
    @Test
    // 方法名描述测试意图：应该识别新对话命令。
    void shouldDetectNewConversationCommand() {
        // 创建底层存储 mock，当前测试不需要真实 MongoDB。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock，当前测试不需要真实 MongoDB。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 验证“新对话”会被识别为新会话命令。
        assertThat(service.isNewConversationCommand("新对话")).isTrue();
        // 验证“清空对话”不会被误判为新会话命令。
        assertThat(service.isNewConversationCommand("清空对话")).isFalse();
    }

    // 测试服务能识别“清空记录”和“清空对话”命令。
    @Test
    // 方法名描述测试意图：应该识别清空记忆命令。
    void shouldDetectClearMemoryCommand() {
        // 创建底层存储 mock，当前测试不需要真实 MongoDB。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock，当前测试不需要真实 MongoDB。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 验证“清空记录”会被识别为清空命令。
        assertThat(service.isClearMemoryCommand("清空记录")).isTrue();
        // 验证“清空对话”会被识别为清空命令。
        assertThat(service.isClearMemoryCommand("清空对话")).isTrue();
        // 验证“新对话”不会被误判为清空命令。
        assertThat(service.isClearMemoryCommand("新对话")).isFalse();
        // 验证普通医疗问题不会被误判为清空命令。
        assertThat(service.isClearMemoryCommand("我头疼应该挂什么科")).isFalse();
    }

    // 测试服务能创建新的会话 ID。
    @Test
    // 方法名描述测试意图：应该创建新会话 ID。
    void shouldCreateNewConversationId() {
        // 创建底层存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 创建新的会话 ID。
        Long memoryId = service.createNewConversationId();

        // 验证新会话 ID 是正数。
        assertThat(memoryId).isPositive();
    }

    // 测试连续创建的新会话 ID 应该递增。
    @Test
    // 方法名描述测试意图：避免高频创建会话时 ID 撞车。
    void shouldCreateIncreasingConversationIds() {
        // 创建底层存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 创建第一个会话 ID。
        Long firstMemoryId = service.createNewConversationId();
        // 创建第二个会话 ID。
        Long secondMemoryId = service.createNewConversationId();

        // 验证第二个会话 ID 大于第一个会话 ID。
        assertThat(secondMemoryId).isGreaterThan(firstMemoryId);
    }

    // 测试开启新对话提示会告诉用户旧历史已保留。
    @Test
    // 方法名描述测试意图：新对话提示不应该说清空历史。
    void shouldReturnNewConversationMessageWithoutClaimingHistoryDeleted() {
        // 创建底层存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 生成新对话提示。
        String message = service.newConversationMessage(2002L);

        // 验证提示说明开启了新对话。
        assertThat(message).contains("已开启新对话");
        // 验证提示说明原对话历史已保留。
        assertThat(message).contains("原对话历史已保留");
        // 验证提示包含新的会话 ID。
        assertThat(message).contains("2002");
        // 验证提示没有说数据库历史被删除。
        assertThat(message).doesNotContain("数据库删除");
    }

    // 测试清空对话时会委托底层存储删除指定 memoryId。
    @Test
    // 方法名描述测试意图：应该清空指定会话记忆。
    void shouldClearConversationByMemoryId() {
        // 创建底层存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 调用清空会话方法。
        service.clearConversation(1001L);

        // 验证底层存储删除了 memoryId=1001 的消息。
        verify(memoryStore).deleteMessages(1001L);
    }

    // 测试清空成功提示由服务层统一维护。
    @Test
    // 方法名描述测试意图：应该返回统一清空提示。
    void shouldReturnClearConversationMessage() {
        // 创建底层存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 验证提示中包含用户最关心的“已清空当前对话记录”。
        assertThat(service.clearConversationMessage()).contains("已清空当前对话记录");
        // 验证提示明确表达历史已从数据库删除。
        assertThat(service.clearConversationMessage()).contains("数据库删除");
    }

    // 测试摘要记忆开关当前默认关闭。
    @Test
    // 方法名描述测试意图：当前版本不应该启用摘要记忆。
    void shouldDisableSummaryMemoryForCurrentPhase() {
        // 创建底层存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 验证第 5 天阶段只预留摘要记忆入口，还没有真正启用。
        assertThat(service.summaryMemoryEnabled()).isFalse();
    }

    // 测试创建会话时会保存会话元数据。
    @Test
    // 方法名描述测试意图：新会话 API 需要有可展示的会话列表数据。
    void shouldCreateConversationSessionMetadata() {
        // 创建底层聊天记忆存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 指定 save 方法直接返回传入的会话对象。
        when(sessionStore.save(any(ConversationSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);
        // 创建参数捕获器。
        ArgumentCaptor<ConversationSession> captor = ArgumentCaptor.forClass(ConversationSession.class);

        // 创建会话。
        ConversationSession session = service.createConversation("糖尿病咨询");

        // 验证返回的会话 ID 是正数。
        assertThat(session.getMemoryId()).isPositive();
        // 验证标题来自请求。
        assertThat(session.getTitle()).isEqualTo("糖尿病咨询");
        // 验证创建时间不为空。
        assertThat(session.getCreatedAt()).isNotNull();
        // 验证更新时间不为空。
        assertThat(session.getUpdatedAt()).isNotNull();
        // 验证保存了会话元数据。
        verify(sessionStore).save(captor.capture());
        // 验证保存对象的标题正确。
        assertThat(captor.getValue().getTitle()).isEqualTo("糖尿病咨询");
    }

    // 测试空标题会使用默认标题。
    @Test
    // 方法名描述测试意图：前端不传标题时仍然能创建可展示会话。
    void shouldUseDefaultTitleWhenCreatingConversationWithoutTitle() {
        // 创建底层聊天记忆存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 指定 save 方法直接返回传入的会话对象。
        when(sessionStore.save(any(ConversationSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 创建没有标题的会话。
        ConversationSession session = service.createConversation(" ");

        // 验证使用默认标题。
        assertThat(session.getTitle()).isEqualTo("新对话");
    }

    // 测试查询会话列表会委托会话存储。
    @Test
    // 方法名描述测试意图：前端会话列表应该来自专门的会话元数据存储。
    void shouldListConversationSessions() {
        // 创建底层聊天记忆存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 创建一个会话。
        ConversationSession session = ConversationSession.of(1001L, "测试会话", java.time.Instant.now(), java.time.Instant.now());
        // 指定查询列表返回测试会话。
        when(sessionStore.findAll()).thenReturn(java.util.List.of(session));
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 查询会话列表。
        java.util.List<ConversationSession> sessions = service.listConversations();

        // 验证返回测试会话。
        assertThat(sessions).containsExactly(session);
        // 验证调用了会话存储。
        verify(sessionStore).findAll();
    }

    // 测试服务能把底层聊天记忆转换成前端消息响应。
    @Test
    // 方法名描述测试意图：前端切换会话后应该能恢复用户和助手消息。
    void shouldListConversationMessages() {
        // 创建底层聊天记忆存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 指定底层聊天记忆返回一问一答。
        when(memoryStore.getMessages(1001L)).thenReturn(java.util.List.of(UserMessage.from("头痛怎么办"), AiMessage.from("建议先观察症状。")));
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 查询前端消息响应。
        java.util.List<ConversationMessageResponse> messages = service.listConversationMessages(1001L);

        // 验证返回两条消息。
        assertThat(messages).hasSize(2);
        // 验证第一条是用户消息。
        assertThat(messages.get(0).getRole()).isEqualTo("user");
        // 验证第一条内容正确。
        assertThat(messages.get(0).getContent()).isEqualTo("头痛怎么办");
        // 验证第二条是助手消息。
        assertThat(messages.get(1).getRole()).isEqualTo("assistant");
        // 验证第二条内容正确。
        assertThat(messages.get(1).getContent()).isEqualTo("建议先观察症状。");
        // 验证调用了底层聊天记忆存储。
        verify(memoryStore).getMessages(1001L);
    }

    // 测试会话历史不会把系统提示词和 RAG 增强 prompt 原样返回给前端。
    @Test
    // 方法名描述测试意图：切换会话后前端只能看到用户问题、助手回答和折叠来源。
    void shouldSanitizeInternalPromptAndRagContextWhenListingConversationMessages() {
        // 创建底层聊天记忆存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 指定底层聊天记忆包含系统提示词、RAG 增强用户消息和助手回答。
        when(memoryStore.getMessages(1001L)).thenReturn(java.util.List.of(
                SystemMessage.from("# 医疗小智系统提示词\n你是“医疗小智”。"),
                UserMessage.from(ragAugmentedUserMessage()),
                AiMessage.from("神经内科主要诊治神经系统相关疾病。")
        ));
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 查询前端消息响应。
        java.util.List<ConversationMessageResponse> messages = service.listConversationMessages(1001L);

        // 验证系统提示词已被过滤，只剩一问一答。
        assertThat(messages).hasSize(2);
        // 验证用户消息只保留原始问题。
        assertThat(messages.get(0).getRole()).isEqualTo("user");
        assertThat(messages.get(0).getContent()).isEqualTo("介绍一下你们医院的神经内科");
        assertThat(messages.get(0).getRagContext()).contains("标题：北京协和医院神经科科室介绍");
        // 验证助手消息没有系统提示词内容。
        assertThat(messages.get(1).getRole()).isEqualTo("assistant");
        assertThat(messages.get(1).getContent()).contains("神经内科");
        assertThat(messages.get(1).getContent()).doesNotContain("医疗小智系统提示词");
    }

    // 测试服务能更新会话标题。
    @Test
    // 方法名描述测试意图：前端生成的会话标题应该保存到 MongoDB 元数据。
    void shouldUpdateConversationTitle() {
        // 创建底层聊天记忆存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 创建已有会话。
        ConversationSession existing = ConversationSession.of(1001L, "新对话", java.time.Instant.now(), java.time.Instant.now());
        // 指定按 memoryId 能查到已有会话。
        when(sessionStore.findByMemoryId(1001L)).thenReturn(java.util.Optional.of(existing));
        // 指定 save 方法直接返回传入对象。
        when(sessionStore.save(any(ConversationSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 更新会话标题。
        ConversationSession updated = service.updateConversationTitle(1001L, "头痛咨询");

        // 验证标题已更新。
        assertThat(updated.getTitle()).isEqualTo("头痛咨询");
        // 验证保存了会话元数据。
        verify(sessionStore).save(existing);
    }

    // 构造 LangChain4j RAG 增强后的用户消息样本。
    private static String ragAugmentedUserMessage() {
        // 返回包含原始问题和 RAG context 的增强 prompt。
        return """
                介绍一下你们医院的神经内科

                Answer using the following information:
                标题：北京协和医院神经科科室介绍
                内容：北京协和医院神经科成立于1921年。
                来源：北京协和医院 https://www.pumch.cn/department_shenjk.html
                """;
    }

    // 测试刷新会话更新时间。
    @Test
    // 方法名描述测试意图：发送新消息后侧边栏排序应该能按 updatedAt 变化。
    void shouldTouchConversationWhenSessionExists() {
        // 创建底层聊天记忆存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 创建已有会话。
        ConversationSession existing = ConversationSession.of(1001L, "头痛咨询", java.time.Instant.parse("2026-07-27T00:00:00Z"), java.time.Instant.parse("2026-07-27T00:00:00Z"));
        // 指定按 memoryId 能查到已有会话。
        when(sessionStore.findByMemoryId(1001L)).thenReturn(java.util.Optional.of(existing));
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 刷新会话更新时间。
        service.touchConversation(1001L);

        // 验证更新时间被刷新。
        assertThat(existing.getUpdatedAt()).isAfter(java.time.Instant.parse("2026-07-27T00:00:00Z"));
        // 验证保存了会话元数据。
        verify(sessionStore).save(existing);
    }

    // 测试删除会话时会同时删除聊天记忆和会话元数据。
    @Test
    // 方法名描述测试意图：删除按钮应该真正删除当前会话，而不是只从前端列表隐藏。
    void shouldDeleteConversationSessionAndMemory() {
        // 创建底层聊天记忆存储 mock。
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        // 创建会话元数据存储 mock。
        ConversationSessionStore sessionStore = mock(ConversationSessionStore.class);
        // 创建被测服务。
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessionStore);

        // 删除会话。
        service.deleteConversation(1001L);

        // 验证删除聊天记忆。
        verify(memoryStore).deleteMessages(1001L);
        // 验证删除会话元数据。
        verify(sessionStore).deleteByMemoryId(1001L);
    }
}
