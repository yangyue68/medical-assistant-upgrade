// 声明当前类所在的包，controller 表示它负责处理 HTTP 请求。
package langchain4j.controller;

// ApiResponse 是项目统一的非流式接口返回结构。
import langchain4j.common.ApiResponse;
// ConversationMemoryService 用来处理会话管理动作。
import langchain4j.memory.ConversationMemoryService;
// ConversationMessageResponse 表示前端展示聊天历史时使用的消息 DTO。
import langchain4j.memory.ConversationMessageResponse;
// ConversationSession 表示会话元数据。
import langchain4j.memory.ConversationSession;
// CreateConversationRequest 表示创建会话请求。
import langchain4j.memory.CreateConversationRequest;
// UpdateConversationTitleRequest 表示更新会话标题请求。
import langchain4j.memory.UpdateConversationTitleRequest;
import langchain4j.auth.AuthSession;
import jakarta.servlet.http.HttpSession;
// Operation 用来给 Knife4j/OpenAPI 文档补充接口说明。
import io.swagger.v3.oas.annotations.Operation;
// Tag 用来给 Knife4j/OpenAPI 文档中的接口分组。
import io.swagger.v3.oas.annotations.tags.Tag;
// List 表示一组会话元数据。
import java.util.List;
// DeleteMapping 表示处理 HTTP DELETE 请求。
import org.springframework.web.bind.annotation.DeleteMapping;
// GetMapping 表示处理 HTTP GET 请求。
import org.springframework.web.bind.annotation.GetMapping;
// PathVariable 表示从 URL 路径中读取变量。
import org.springframework.web.bind.annotation.PathVariable;
// PatchMapping 表示处理 HTTP PATCH 请求。
import org.springframework.web.bind.annotation.PatchMapping;
// PostMapping 表示处理 HTTP POST 请求。
import org.springframework.web.bind.annotation.PostMapping;
// RequestBody 表示从 HTTP 请求体中读取 JSON 参数。
import org.springframework.web.bind.annotation.RequestBody;
// RequestMapping 表示给当前 Controller 的所有接口统一加路径前缀。
import org.springframework.web.bind.annotation.RequestMapping;
// RestController 表示当前类的返回值会直接写入 HTTP 响应体。
import org.springframework.web.bind.annotation.RestController;

// 给 OpenAPI 文档添加接口分组名称。
@Tag(name = "会话管理")
// 标记当前类为 REST Controller。
@RestController
// 给当前类下所有接口加上 /xiaozhi/conversations 前缀。
@RequestMapping("/xiaozhi/conversations")
// 定义会话管理 HTTP 接口控制器。
public class ConversationController {

    // 保存会话记忆服务依赖。
    private final ConversationMemoryService conversationMemoryService;

    // 构造器注入会话记忆服务。
    public ConversationController(
            // Spring 会自动注入 ConversationMemoryService Bean。
            ConversationMemoryService conversationMemoryService
    ) {
        // 保存会话记忆服务。
        this.conversationMemoryService = conversationMemoryService;
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "创建新会话")
    // 声明 POST /xiaozhi/conversations 接口。
    @PostMapping
    // create 方法创建一个新的会话元数据。
    public ApiResponse<ConversationSession> create(@RequestBody(required = false) CreateConversationRequest request, HttpSession httpSession) {
        // 读取前端传入的标题；请求为空时标题也为空。
        String title = request == null ? null : request.getTitle();
        // 创建新会话。
        ConversationSession session = conversationMemoryService.createConversation(AuthSession.userId(httpSession), title);
        // 返回统一成功响应。
        return ApiResponse.success(session);
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "查询会话列表")
    // 声明 GET /xiaozhi/conversations 接口。
    @GetMapping
    // list 方法返回所有会话元数据。
    public ApiResponse<List<ConversationSession>> list(HttpSession httpSession) {
        // 查询会话列表。
        List<ConversationSession> sessions = conversationMemoryService.listConversations(AuthSession.userId(httpSession));
        // 返回统一成功响应。
        return ApiResponse.success(sessions);
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "查询会话消息")
    // 声明 GET /xiaozhi/conversations/{memoryId}/messages 接口。
    @GetMapping("/{memoryId}/messages")
    // messages 方法查询当前会话已经持久化的聊天历史。
    public ApiResponse<List<ConversationMessageResponse>> messages(@PathVariable("memoryId") Long memoryId, HttpSession httpSession) {
        // 查询会话消息列表。
        List<ConversationMessageResponse> messages = conversationMemoryService.listConversationMessages(AuthSession.userId(httpSession), memoryId);
        // 返回统一成功响应。
        return ApiResponse.success(messages);
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "更新会话标题")
    // 声明 PATCH /xiaozhi/conversations/{memoryId} 接口。
    @PatchMapping("/{memoryId}")
    // updateTitle 方法更新侧边栏展示的会话标题。
    public ApiResponse<ConversationSession> updateTitle(
            // 从 URL 路径中读取 memoryId。
            @PathVariable("memoryId") Long memoryId,
            // 从请求体中读取新标题。
            @RequestBody UpdateConversationTitleRequest request, HttpSession httpSession
    ) {
        // 请求体为空时标题按 null 处理，服务层会规整成默认标题。
        String title = request == null ? null : request.getTitle();
        // 更新会话标题。
        ConversationSession session = conversationMemoryService.updateConversationTitle(AuthSession.userId(httpSession), memoryId, title);
        // 返回更新后的会话元数据。
        return ApiResponse.success(session);
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "删除会话")
    // 声明 DELETE /xiaozhi/conversations/{memoryId} 接口。
    @DeleteMapping("/{memoryId}")
    // delete 方法删除会话元数据和对应聊天记忆。
    public ApiResponse<Boolean> delete(@PathVariable("memoryId") Long memoryId, HttpSession httpSession) {
        // 删除会话。
        conversationMemoryService.deleteConversation(AuthSession.userId(httpSession), memoryId);
        // 返回删除成功标记。
        return ApiResponse.success(Boolean.TRUE);
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "清空当前会话消息")
    // 声明 DELETE /xiaozhi/conversations/{memoryId}/messages 接口。
    @DeleteMapping("/{memoryId}/messages")
    // clearMessages 方法只删除当前会话聊天消息，不删除会话窗口元数据。
    public ApiResponse<Boolean> clearMessages(@PathVariable("memoryId") Long memoryId, HttpSession httpSession) {
        // 清空指定会话的聊天记忆。
        conversationMemoryService.clearConversation(AuthSession.userId(httpSession), memoryId);
        // 返回清空成功标记。
        return ApiResponse.success(Boolean.TRUE);
    }

    // Compatibility overloads keep existing standalone controller tests and integrations source-compatible.
    public ApiResponse<ConversationSession> create(CreateConversationRequest request) {
        return ApiResponse.success(conversationMemoryService.createConversation(request == null ? null : request.getTitle()));
    }

    public ApiResponse<List<ConversationSession>> list() {
        return ApiResponse.success(conversationMemoryService.listConversations());
    }

    public ApiResponse<List<ConversationMessageResponse>> messages(Long memoryId) {
        return ApiResponse.success(conversationMemoryService.listConversationMessages(memoryId));
    }

    public ApiResponse<ConversationSession> updateTitle(Long memoryId, UpdateConversationTitleRequest request) {
        return ApiResponse.success(conversationMemoryService.updateConversationTitle(memoryId,
                request == null ? null : request.getTitle()));
    }

    public ApiResponse<Boolean> delete(Long memoryId) {
        conversationMemoryService.deleteConversation(memoryId);
        return ApiResponse.success(Boolean.TRUE);
    }

    public ApiResponse<Boolean> clearMessages(Long memoryId) {
        conversationMemoryService.clearConversation(memoryId);
        return ApiResponse.success(Boolean.TRUE);
    }
}
