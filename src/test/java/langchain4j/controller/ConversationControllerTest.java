// 声明测试类所在的包。
package langchain4j.controller;

// ApiResponse 是统一接口响应结构。
import langchain4j.common.ApiResponse;
// ConversationMemoryService 是会话管理 Controller 依赖的服务。
import langchain4j.memory.ConversationMemoryService;
// ConversationMessageResponse 表示会话历史消息响应。
import langchain4j.memory.ConversationMessageResponse;
// ConversationSession 表示会话元数据。
import langchain4j.memory.ConversationSession;
// CreateConversationRequest 表示创建会话请求。
import langchain4j.memory.CreateConversationRequest;
// UpdateConversationTitleRequest 表示更新会话标题请求。
import langchain4j.memory.UpdateConversationTitleRequest;
// Instant 用来构造测试时间。
import java.time.Instant;
// List 表示一组会话元数据。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// Mockito 的 eq 用来匹配指定参数。
import static org.mockito.ArgumentMatchers.eq;
// Mockito 的 mock 用来创建模拟对象。
import static org.mockito.Mockito.mock;
// Mockito 的 verify 用来验证方法调用。
import static org.mockito.Mockito.verify;
// Mockito 的 when 用来指定 mock 方法返回值。
import static org.mockito.Mockito.when;

// 定义会话管理 Controller 的单元测试。
class ConversationControllerTest {

    // 测试创建会话接口会返回会话元数据。
    @Test
    // 方法名描述测试意图：前端点击新对话按钮后应该拿到新的 memoryId。
    void shouldCreateConversation() {
        // 创建会话记忆服务 mock。
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        // 创建被测 Controller。
        ConversationController controller = new ConversationController(memoryService);
        // 创建请求对象。
        CreateConversationRequest request = new CreateConversationRequest();
        // 设置会话标题。
        request.setTitle("糖尿病咨询");
        // 创建服务层返回的会话。
        ConversationSession session = session(1001L, "糖尿病咨询");
        // 指定服务层创建会话结果。
        when(memoryService.createConversation(eq("糖尿病咨询"))).thenReturn(session);

        // 调用创建接口。
        ApiResponse<ConversationSession> response = controller.create(request);

        // 验证统一响应码为成功。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证响应中包含新会话 ID。
        assertThat(response.getData().getMemoryId()).isEqualTo(1001L);
        // 验证响应中包含会话标题。
        assertThat(response.getData().getTitle()).isEqualTo("糖尿病咨询");
        // 验证 Controller 委托服务创建会话。
        verify(memoryService).createConversation("糖尿病咨询");
    }

    // 测试会话列表接口会返回所有会话。
    @Test
    // 方法名描述测试意图：前端侧边栏应该可以加载已有会话。
    void shouldListConversations() {
        // 创建会话记忆服务 mock。
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        // 创建被测 Controller。
        ConversationController controller = new ConversationController(memoryService);
        // 创建会话列表。
        List<ConversationSession> sessions = List.of(session(1001L, "糖尿病咨询"), session(1002L, "头痛咨询"));
        // 指定服务层返回会话列表。
        when(memoryService.listConversations()).thenReturn(sessions);

        // 调用列表接口。
        ApiResponse<List<ConversationSession>> response = controller.list();

        // 验证统一响应码为成功。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证返回两个会话。
        assertThat(response.getData()).hasSize(2);
        // 验证第一个会话标题。
        assertThat(response.getData().get(0).getTitle()).isEqualTo("糖尿病咨询");
        // 验证 Controller 委托服务查询列表。
        verify(memoryService).listConversations();
    }

    // 测试查询会话消息接口会返回历史消息。
    @Test
    // 方法名描述测试意图：切换会话时前端应该能从后端恢复历史。
    void shouldListConversationMessages() {
        // 创建会话记忆服务 mock。
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        // 创建被测 Controller。
        ConversationController controller = new ConversationController(memoryService);
        // 创建历史消息列表。
        List<ConversationMessageResponse> messages = List.of(new ConversationMessageResponse("user", "头痛怎么办", 1L));
        // 指定服务层返回历史消息。
        when(memoryService.listConversationMessages(eq(1001L))).thenReturn(messages);

        // 调用消息查询接口。
        ApiResponse<List<ConversationMessageResponse>> response = controller.messages(1001L);

        // 验证统一响应码为成功。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证返回历史消息。
        assertThat(response.getData()).containsExactlyElementsOf(messages);
        // 验证 Controller 委托服务查询消息。
        verify(memoryService).listConversationMessages(1001L);
    }

    // 测试更新会话标题接口会返回更新后的会话。
    @Test
    // 方法名描述测试意图：前端根据第一条用户消息生成标题后应该能持久化。
    void shouldUpdateConversationTitle() {
        // 创建会话记忆服务 mock。
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        // 创建被测 Controller。
        ConversationController controller = new ConversationController(memoryService);
        // 创建请求对象。
        UpdateConversationTitleRequest request = new UpdateConversationTitleRequest();
        // 设置新标题。
        request.setTitle("头痛咨询");
        // 创建更新后的会话。
        ConversationSession session = session(1001L, "头痛咨询");
        // 指定服务层更新结果。
        when(memoryService.updateConversationTitle(eq(1001L), eq("头痛咨询"))).thenReturn(session);

        // 调用标题更新接口。
        ApiResponse<ConversationSession> response = controller.updateTitle(1001L, request);

        // 验证统一响应码为成功。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证标题已更新。
        assertThat(response.getData().getTitle()).isEqualTo("头痛咨询");
        // 验证 Controller 委托服务更新标题。
        verify(memoryService).updateConversationTitle(1001L, "头痛咨询");
    }

    // 测试删除会话接口会删除会话。
    @Test
    // 方法名描述测试意图：前端点击删除按钮后应该删除会话和历史记忆。
    void shouldDeleteConversation() {
        // 创建会话记忆服务 mock。
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        // 创建被测 Controller。
        ConversationController controller = new ConversationController(memoryService);

        // 调用删除接口。
        ApiResponse<Boolean> response = controller.delete(1001L);

        // 验证统一响应码为成功。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证返回删除成功标记。
        assertThat(response.getData()).isTrue();
        // 验证 Controller 委托服务删除会话。
        verify(memoryService).deleteConversation(1001L);
    }

    // 测试清空会话消息接口只清空聊天记忆。
    @Test
    // 方法名描述测试意图：清空对话按钮不应该删除会话窗口。
    void shouldClearConversationMessagesWithoutDeletingSession() {
        // 创建会话记忆服务 mock。
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        // 创建被测 Controller。
        ConversationController controller = new ConversationController(memoryService);

        // 调用清空消息接口。
        ApiResponse<Boolean> response = controller.clearMessages(1001L);

        // 验证统一响应码为成功。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证返回清空成功标记。
        assertThat(response.getData()).isTrue();
        // 验证 Controller 只委托服务清空聊天记忆。
        verify(memoryService).clearConversation(1001L);
    }

    // 创建测试会话。
    private static ConversationSession session(Long memoryId, String title) {
        // 读取固定时间。
        Instant now = Instant.parse("2026-07-27T00:00:00Z");
        // 返回会话元数据。
        return ConversationSession.of(memoryId, title, now, now);
    }
}
