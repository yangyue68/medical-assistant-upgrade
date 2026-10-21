// 声明测试类所在的包；和被测 Controller 同包，方便测试包可见方法。
package langchain4j.controller;

// ApiResponse 是 readiness 接口返回的统一响应结构。
import langchain4j.common.ApiResponse;
// XiaoZhiAgent 是 Controller 依赖的 AI Agent，这里会用 Mockito 模拟。
import langchain4j.assistant.XiaoZhiAgent;
// ChatForm 是聊天接口请求体。
import langchain4j.bean.ChatForm;
// StartupConfigValidator 是 readiness 接口依赖的配置检查器。
import langchain4j.config.StartupConfigValidator;
// ChatMemoryPolicy 是 Controller 用来处理上下文超限的策略类。
import langchain4j.memory.ChatMemoryPolicy;
// ConversationMemoryService 是 Controller 用来处理新对话、清空记录的记忆服务。
import langchain4j.memory.ConversationMemoryService;
// List 用来构造 readiness 的告警列表。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;
// Flux 用来模拟 AI Agent 的流式响应。
import reactor.core.publisher.Flux;
// StepVerifier 用来验证 Flux 里发出的数据是否符合预期。
import reactor.test.StepVerifier;

// AssertJ 断言工具，让测试表达更接近自然语言。
import static org.assertj.core.api.Assertions.assertThat;
// Mockito 的 eq 用于校验 mock 方法参数。
import static org.mockito.ArgumentMatchers.eq;
// Mockito 的 mock 用于创建模拟对象。
import static org.mockito.Mockito.mock;
// Mockito 的 verify 用于验证某个 mock 方法是否被调用。
import static org.mockito.Mockito.verify;
// Mockito 的 verifyNoInteractions 用于验证某个 mock 没有被调用。
import static org.mockito.Mockito.verifyNoInteractions;
// Mockito 的 verifyNoMoreInteractions 用于验证指定 mock 没有额外调用。
import static org.mockito.Mockito.verifyNoMoreInteractions;
// Mockito 的 when 用于指定 mock 方法的返回值。
import static org.mockito.Mockito.when;

// 定义 XiaozhiController 的单元测试类。
class XiaozhiControllerTest {

    // 测试普通聊天请求会被转发给 XiaoZhiAgent。
    @Test
    // 方法名描述测试意图：应该把聊天请求委托给 Agent。
    void shouldDelegateChatRequestToAgent() {
        // 创建 XiaoZhiAgent 的 mock，避免测试中真的调用在线大模型。
        XiaoZhiAgent agent = mock(XiaoZhiAgent.class);
        // 创建 ConversationMemoryService 的 mock，当前测试不需要真实 MongoDB。
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        // 创建 StartupConfigValidator 的 mock，当前测试不关心 readiness。
        StartupConfigValidator validator = mock(StartupConfigValidator.class);
        // 使用构造器注入创建 Controller，这也是为什么要从字段注入改成构造器注入。
        XiaozhiController controller = new XiaozhiController(agent, memoryService, validator, testMemoryPolicy());
        // 创建聊天请求对象。
        ChatForm chatForm = new ChatForm();
        // 设置会话 ID。
        chatForm.setMemoryId("123");
        // 设置用户消息。
        chatForm.setMessage("你好");
        // 指定当 Agent 收到 memoryId=123 和 message=你好 时，返回 ok。
        when(agent.chat(eq(123L), eq("你好"))).thenReturn(Flux.just("ok"));
        // 指定普通消息不是新对话命令。
        when(memoryService.isNewConversationCommand(eq("你好"))).thenReturn(false);
        // 指定普通消息不是清空记忆命令。
        when(memoryService.isClearMemoryCommand(eq("你好"))).thenReturn(false);

        // 执行 Controller 聊天方法，并验证 Flux 输出。
        StepVerifier.create(controller.chat(chatForm))
                // 期望流里只发出一个 ok。
                .expectNext("ok")
                // 期望 Flux 正常结束。
                .verifyComplete();

        // 验证 Controller 确实调用了 Agent。
        verify(agent).chat(123L, "你好");
        // 验证 Controller 会先询问记忆服务当前消息是否为新对话命令。
        verify(memoryService).isNewConversationCommand("你好");
        // 验证 Controller 会询问记忆服务当前消息是否为清空命令。
        verify(memoryService).isClearMemoryCommand("你好");
        // 验证普通聊天会刷新会话更新时间。
        verify(memoryService).touchConversation(123L);
        // 验证普通聊天不会真正清空记忆。
        verifyNoMoreInteractions(memoryService);
    }

    // 测试“新对话”命令只开启新会话，不清空当前会话记忆。
    @Test
    // 方法名描述测试意图：新对话应该保留旧历史。
    void shouldStartNewConversationWithoutClearingMemory() {
        // 创建 XiaoZhiAgent 的 mock。
        XiaoZhiAgent agent = mock(XiaoZhiAgent.class);
        // 创建 ConversationMemoryService 的 mock。
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        // 创建 StartupConfigValidator 的 mock。
        StartupConfigValidator validator = mock(StartupConfigValidator.class);
        // 创建 Controller。
        XiaozhiController controller = new XiaozhiController(agent, memoryService, validator, testMemoryPolicy());
        // 创建聊天请求对象。
        ChatForm chatForm = new ChatForm();
        // 设置会话 ID。
        chatForm.setMemoryId("456");
        // 设置新对话命令。
        chatForm.setMessage("新对话");
        // 指定“新对话”会被记忆服务识别为新会话命令。
        when(memoryService.isNewConversationCommand(eq("新对话"))).thenReturn(true);
        // 指定记忆服务生成的新会话 ID。
        when(memoryService.createNewConversationId()).thenReturn(9001L);
        // 指定开启新对话后的统一提示文案。
        when(memoryService.newConversationMessage(eq(9001L))).thenReturn("✅ 已开启新对话，原对话历史已保留。新的会话ID：9001");

        // 执行 Controller 聊天方法，并验证返回新对话提示。
        StepVerifier.create(controller.chat(chatForm))
                // 期望响应里包含“已开启新对话”。
                .expectNextMatches(text -> text.contains("已开启新对话")
                        // 期望响应里说明原对话历史已保留。
                        && text.contains("原对话历史已保留")
                        // 期望响应里包含新的会话 ID。
                        && text.contains("9001"))
                // 期望 Flux 正常结束。
                .verifyComplete();

        // 验证 Controller 询问了记忆服务是否为新对话命令。
        verify(memoryService).isNewConversationCommand("新对话");
        // 验证 Controller 委托记忆服务创建了新的会话 ID。
        verify(memoryService).createNewConversationId();
        // 验证 Controller 从记忆服务获取了统一新对话提示。
        verify(memoryService).newConversationMessage(9001L);
        // 验证“新对话”命令不会调用大模型。
        verifyNoInteractions(agent);
        // 验证“新对话”没有继续执行清空记忆分支。
        verifyNoMoreInteractions(memoryService);
    }

    // 测试“清空对话”命令才会删除当前会话记忆。
    @Test
    // 方法名描述测试意图：清空对话应该删除数据库历史。
    void shouldClearMemoryWhenUserClearsConversation() {
        // 创建 XiaoZhiAgent 的 mock。
        XiaoZhiAgent agent = mock(XiaoZhiAgent.class);
        // 创建 ConversationMemoryService 的 mock。
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        // 创建 StartupConfigValidator 的 mock。
        StartupConfigValidator validator = mock(StartupConfigValidator.class);
        // 创建 Controller。
        XiaozhiController controller = new XiaozhiController(agent, memoryService, validator, testMemoryPolicy());
        // 创建聊天请求对象。
        ChatForm chatForm = new ChatForm();
        // 设置会话 ID。
        chatForm.setMemoryId("456");
        // 设置清空对话命令。
        chatForm.setMessage("清空对话");
        // 指定“清空对话”不是新对话命令。
        when(memoryService.isNewConversationCommand(eq("清空对话"))).thenReturn(false);
        // 指定“清空对话”会被记忆服务识别为清空命令。
        when(memoryService.isClearMemoryCommand(eq("清空对话"))).thenReturn(true);
        // 指定清空成功后的统一提示文案。
        when(memoryService.clearConversationMessage()).thenReturn("✅ 已清空当前对话记录，该会话历史已从数据库删除。");

        // 执行 Controller 聊天方法，并验证返回清空成功提示。
        StepVerifier.create(controller.chat(chatForm))
                // 期望响应里包含“已清空当前对话记录”。
                .expectNextMatches(text -> text.contains("已清空当前对话记录")
                        // 期望响应里明确说明数据库历史已删除。
                        && text.contains("数据库删除"))
                // 期望 Flux 正常结束。
                .verifyComplete();

        // 验证 Controller 询问了记忆服务是否为新对话命令。
        verify(memoryService).isNewConversationCommand("清空对话");
        // 验证 Controller 询问了记忆服务是否为清空命令。
        verify(memoryService).isClearMemoryCommand("清空对话");
        // 验证 Controller 委托记忆服务清空了 memoryId=456 的记忆。
        verify(memoryService).clearConversation(456L);
        // 验证 Controller 从记忆服务获取了统一清空提示。
        verify(memoryService).clearConversationMessage();
        // 验证清空命令不会调用大模型。
        verifyNoInteractions(agent);
    }

    // 测试 readiness 接口返回统一 ApiResponse。
    @Test
    // 方法名描述测试意图：应该返回配置检查告警。
    void shouldReturnReadinessWarnings() {
        // 创建 XiaoZhiAgent 的 mock。
        XiaoZhiAgent agent = mock(XiaoZhiAgent.class);
        // 创建 ConversationMemoryService 的 mock。
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        // 创建 StartupConfigValidator 的 mock。
        StartupConfigValidator validator = mock(StartupConfigValidator.class);
        // 指定配置检查器返回一个告警。
        when(validator.validate()).thenReturn(List.of("QWEN_API_KEY is missing."));
        // 创建 Controller。
        XiaozhiController controller = new XiaozhiController(agent, memoryService, validator, testMemoryPolicy());

        // 调用 readiness 接口。
        ApiResponse<List<String>> response = controller.readiness();

        // 验证统一响应码为 0。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证 data 中包含配置告警。
        assertThat(response.getData()).containsExactly("QWEN_API_KEY is missing.");
    }

    // 测试非法 memoryId 不会让接口失败。
    @Test
    // 方法名描述测试意图：非法 memoryId 应该兜底生成新 ID。
    void shouldFallbackWhenMemoryIdIsInvalid() {
        // 创建对话记忆服务 mock。
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        // 指定非法 memoryId 兜底时生成的新 ID。
        when(memoryService.createNewConversationId()).thenReturn(9002L);
        // 创建 Controller；这个测试只调用 parseMemoryId，所以依赖可以用 mock 占位。
        XiaozhiController controller = new XiaozhiController(
                // mock AI Agent。
                mock(XiaoZhiAgent.class),
                // 使用可验证的对话记忆服务 mock。
                memoryService,
                // mock 启动配置校验器。
                mock(StartupConfigValidator.class),
                // 使用真实 ChatMemoryPolicy，验证 memoryId 解析不依赖 Spring 容器。
                testMemoryPolicy()
        );

        // 调用 parseMemoryId，并传入非法字符串。
        Long memoryId = controller.parseMemoryId("abc");

        // 验证返回了服务层生成的新会话 ID，而不是抛异常。
        assertThat(memoryId).isEqualTo(9002L);
        // 验证 Controller 复用了服务层统一 ID 生成器。
        verify(memoryService).createNewConversationId();
    }

    // 测试模型上下文超限时不会清空 MongoDB 记忆。
    @Test
    // 方法名描述测试意图：上下文超限只给降级提示，不执行清空记忆。
    void shouldNotClearMemoryWhenContextLengthExceeded() {
        // 创建 XiaoZhiAgent 的 mock。
        XiaoZhiAgent agent = mock(XiaoZhiAgent.class);
        // 创建 ConversationMemoryService 的 mock。
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        // 创建 StartupConfigValidator 的 mock。
        StartupConfigValidator validator = mock(StartupConfigValidator.class);
        // 创建真实 ChatMemoryPolicy，让测试覆盖真实上下文超限判断。
        ChatMemoryPolicy memoryPolicy = testMemoryPolicy();
        // 创建 Controller。
        XiaozhiController controller = new XiaozhiController(agent, memoryService, validator, memoryPolicy);
        // 创建聊天请求对象。
        ChatForm chatForm = new ChatForm();
        // 设置会话 ID。
        chatForm.setMemoryId("789");
        // 设置用户消息。
        chatForm.setMessage("继续刚才的问题");
        // 指定 Agent 返回 context_length 异常。
        when(agent.chat(eq(789L), eq("继续刚才的问题")))
                // Flux.error 用来模拟响应式链路中的异步异常。
                .thenReturn(Flux.error(new RuntimeException("context_length exceeded")));
        // 指定普通追问不是新对话命令。
        when(memoryService.isNewConversationCommand(eq("继续刚才的问题"))).thenReturn(false);
        // 指定普通追问不是清空记忆命令。
        when(memoryService.isClearMemoryCommand(eq("继续刚才的问题"))).thenReturn(false);

        // 执行聊天方法并验证降级提示。
        StepVerifier.create(controller.chat(chatForm))
                // 验证提示中包含“无法继续携带全部历史上下文”。
                .expectNextMatches(text -> text.contains("无法继续携带全部历史上下文")
                        // 验证提示中不再出现误导性的“已清空记忆”。
                        && !text.contains("已清空记忆"))
                // 期望 Flux 正常结束。
                .verifyComplete();

        // 验证 Controller 会询问记忆服务当前消息是否为新对话命令。
        verify(memoryService).isNewConversationCommand("继续刚才的问题");
        // 验证 Controller 会询问记忆服务当前消息是否为清空命令。
        verify(memoryService).isClearMemoryCommand("继续刚才的问题");
        // 验证即使后续模型超限，收到用户消息时也会先刷新会话活跃时间。
        verify(memoryService).touchConversation(789L);
        // 验证上下文超限时没有清空聊天记忆。
        verifyNoMoreInteractions(memoryService);
        // 验证 Agent 只有预期的一次调用。
        verify(agent).chat(789L, "继续刚才的问题");
        // 验证 Agent 没有发生额外调用。
        verifyNoMoreInteractions(agent);
    }

    // 创建测试用 ChatMemoryPolicy。
    private static ChatMemoryPolicy testMemoryPolicy() {
        // 使用默认窗口 20 和最小窗口 4，与当前 application.properties 默认值一致。
        return new ChatMemoryPolicy(20, 4);
    }
}
