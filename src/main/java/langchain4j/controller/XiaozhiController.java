// 声明当前类所在的包，controller 表示它负责处理 HTTP 请求。
package langchain4j.controller;

// ApiResponse 是项目统一的非流式接口返回结构。
import langchain4j.common.ApiResponse;
// XiaoZhiAgent 是 LangChain4j 生成的 AI 服务接口。
import langchain4j.assistant.XiaoZhiAgent;
// ChatForm 是前端发送聊天请求时使用的请求体对象。
import langchain4j.bean.ChatForm;
// StartupConfigValidator 用来检查启动配置是否完整。
import langchain4j.config.StartupConfigValidator;
// ChatMemoryPolicy 用来判断上下文超限并生成对应的用户提示。
import langchain4j.memory.ChatMemoryPolicy;
// ConversationMemoryService 用来处理新对话、清空记录等记忆管理动作。
import langchain4j.memory.ConversationMemoryService;
// SensitiveDataMasker 用来在写日志前隐藏身份证、手机号、API Key 等敏感信息。
import langchain4j.util.SensitiveDataMasker;
import langchain4j.auth.AccountContext;
// Operation 用来给 Knife4j/OpenAPI 文档补充接口说明。
import io.swagger.v3.oas.annotations.Operation;
// Tag 用来给 Knife4j/OpenAPI 文档中的接口分组。
import io.swagger.v3.oas.annotations.tags.Tag;
// List 表示 readiness 接口返回的一组配置告警。
import java.util.List;
// Logger 是 SLF4J 的日志接口。
import org.slf4j.Logger;
// LoggerFactory 用来创建当前类对应的日志对象。
import org.slf4j.LoggerFactory;
// StringUtils 提供字符串判空工具。
import org.springframework.util.StringUtils;
// GetMapping 表示处理 HTTP GET 请求。
import org.springframework.web.bind.annotation.GetMapping;
// PostMapping 表示处理 HTTP POST 请求。
import org.springframework.web.bind.annotation.PostMapping;
// RequestBody 表示从 HTTP 请求体中读取 JSON 参数。
import org.springframework.web.bind.annotation.RequestBody;
// RequestMapping 表示给当前 Controller 的所有接口统一加路径前缀。
import org.springframework.web.bind.annotation.RequestMapping;
// RestController 表示当前类的返回值会直接写入 HTTP 响应体。
import org.springframework.web.bind.annotation.RestController;
// Flux 是 Reactor 的流式数据类型，用于聊天接口逐段返回模型输出。
import reactor.core.publisher.Flux;

// 给 OpenAPI 文档添加接口分组名称。
@Tag(name = "智能医疗助手")
// 标记当前类为 REST Controller。
@RestController
// 给当前类下所有接口加上 /xiaozhi 前缀。
@RequestMapping("/xiaozhi")
// 定义医疗小智 HTTP 接口控制器。
public class XiaozhiController {

    // 创建当前类的日志对象，替代 System.out.println。
    private static final Logger log = LoggerFactory.getLogger(XiaozhiController.class);

    // 保存 AI Agent 依赖，用于处理用户聊天请求。
    private final XiaoZhiAgent xiaoZhiAgent;

    // 保存对话记忆服务依赖，用于执行“新对话/清空对话”等命令。
    private final ConversationMemoryService conversationMemoryService;

    // 保存启动配置校验器依赖，用于 readiness 接口。
    private final StartupConfigValidator startupConfigValidator;

    // 保存聊天记忆策略依赖，用于上下文超限判断和降级文案。
    private final ChatMemoryPolicy chatMemoryPolicy;

    // 构造器注入依赖，让依赖关系更明确，也方便单元测试直接 new Controller。
    public XiaozhiController(
            // Spring 会自动注入 XiaoZhiAgent Bean。
            XiaoZhiAgent xiaoZhiAgent,
            // Spring 会自动注入 ConversationMemoryService Bean。
            ConversationMemoryService conversationMemoryService,
            // Spring 会自动注入 StartupConfigValidator Bean。
            StartupConfigValidator startupConfigValidator,
            // Spring 会自动注入 ChatMemoryPolicy Bean。
            ChatMemoryPolicy chatMemoryPolicy
    ) {
        // 保存 AI Agent 到成员变量。
        this.xiaoZhiAgent = xiaoZhiAgent;
        // 保存对话记忆服务到成员变量。
        this.conversationMemoryService = conversationMemoryService;
        // 保存启动配置校验器到成员变量。
        this.startupConfigValidator = startupConfigValidator;
        // 保存聊天记忆策略到成员变量。
        this.chatMemoryPolicy = chatMemoryPolicy;
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "医疗咨询流式对话")
    // 声明 POST /xiaozhi/chat 接口，并指定返回文本流。
    @PostMapping(value = "/chat", produces = "text/plain;charset=utf-8")
    // chat 方法接收前端请求，并返回 Flux<String> 让前端逐段接收模型输出。
    public Flux<String> chat(@RequestBody ChatForm chatForm) {
        // 校验请求体不能为空，否则后面读取字段会空指针。
        if (chatForm == null) {
            // 对流式接口来说，直接返回一段友好提示比抛异常更适合前端展示。
            return Flux.just("❌ 请求体不能为空。");
        }

        // 把前端传来的 memoryId 字符串转换成 Long；无法转换时会生成新的会话 ID。
        Long memoryId = parseMemoryId(chatForm.getMemoryId());
        // 读取用户消息文本。
        String message = chatForm.getMessage();

        // 如果用户没有输入有效内容，就直接返回提示，避免调用大模型浪费 token。
        if (!StringUtils.hasText(message)) {
            // 返回一段流式文本，提醒用户输入问题。
            return Flux.just("请输入您的问题，我会尽力帮助您。");
        }

        // 判断用户是否输入了新对话命令。
        if (conversationMemoryService.isNewConversationCommand(message)) {
            // 委托记忆服务生成新的会话 ID，旧 memoryId 对应的历史不会被删除。
            Long newMemoryId = conversationMemoryService.createNewConversationId();
            // 返回新对话提示，并把新的会话 ID 告诉调用方。
            return Flux.just(conversationMemoryService.newConversationMessage(newMemoryId));
        }

        // 判断用户是否输入了清空会话命令。
        if (conversationMemoryService.isClearMemoryCommand(message)) {
            // 委托记忆服务清空当前 memoryId 对应的聊天记忆。
            conversationMemoryService.clearConversation(memoryId);
            // 返回记忆服务统一维护的清空成功提示。
            return Flux.just(conversationMemoryService.clearConversationMessage());
        }

        // 记录请求日志，但用户消息必须先脱敏。
        log.info("Received chat request. memoryId={}, message={}", memoryId, SensitiveDataMasker.mask(message));

        // 刷新会话更新时间，让侧边栏排序能体现真实活跃会话。
        Long userId = AccountContext.currentUserId();
        if (userId != null) {
            conversationMemoryService.touchConversation(userId, memoryId);
        } else {
            conversationMemoryService.touchConversation(memoryId);
        }

        // 调用 AI Agent 处理聊天请求，并返回流式响应。
        Flux<String> agentResponse = userId == null
                ? xiaoZhiAgent.chat(memoryId, message)
                : xiaoZhiAgent.chat(userId, memoryId, message);
        return agentResponse
                // 捕获模型调用、工具调用等异步链路中的异常。
                .onErrorResume(throwable -> {
                    // 读取异常消息，后续根据消息内容判断工具调用错误。
                    String errorMsg = throwable.getMessage();

                    // 如果异常包含 context_length，通常表示对话历史超过模型上下文长度。
                    if (chatMemoryPolicy.isContextLengthExceeded(throwable)) {
                        // 记录 warn 日志，说明是可预期的上下文超限问题。
                        log.warn("LLM context length exceeded. memoryId={}", memoryId);
                        // 返回策略类统一生成的降级提示，不再声称已经清空记忆。
                        return Flux.just(chatMemoryPolicy.contextLengthFallbackMessage());
                    }

                    // 如果异常包含 ToolExecutor，通常表示工具调用执行失败。
                    if (errorMsg != null && errorMsg.contains("ToolExecutor")) {
                        // 返回工具调用失败的友好提示。
                        return Flux.just(
                                "❌ 服务出现技术问题，工具调用异常，请稍后重试。\n" +
                                        "如问题持续，请回复「新对话」重新开始。"
                        );
                    }

                    // 对未知异常记录完整堆栈，方便工程排查。
                    log.error("Chat request failed. memoryId={}", memoryId, throwable);
                    // 返回统一兜底文案给用户。
                    return Flux.just("❌ 系统繁忙，请稍后重试。");
                });
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "启动配置检查")
    // 声明 GET /xiaozhi/readiness 接口。
    @GetMapping("/readiness")
    // readiness 方法返回当前关键配置检查结果。
    public ApiResponse<List<String>> readiness() {
        // 执行配置检查，拿到所有告警。
        List<String> warnings = startupConfigValidator.validate();
        // 把告警列表放进统一响应结构；空列表表示当前没有发现配置问题。
        return ApiResponse.success(warnings);
    }

    // 把前端传来的 memoryId 字符串转换成 Long。
    Long parseMemoryId(String memoryIdStr) {
        // 如果前端没传 memoryId，就让会话服务创建新的会话 ID。
        if (!StringUtils.hasText(memoryIdStr)) {
            // 返回统一 ID 生成器创建的会话 ID。
            return conversationMemoryService.createNewConversationId();
        }
        // 尝试把字符串解析成 Long。
        try {
            // 解析成功就返回用户传来的会话 ID。
            return Long.parseLong(memoryIdStr);
        } catch (NumberFormatException e) {
            // 如果解析失败，就记录 warn 日志，说明前端传了非法 memoryId。
            log.warn("Invalid memoryId received: {}", SensitiveDataMasker.mask(memoryIdStr));
            // 返回统一 ID 生成器创建的会话 ID，避免接口因为会话 ID 格式问题直接不可用。
            return conversationMemoryService.createNewConversationId();
        }
    }
}
