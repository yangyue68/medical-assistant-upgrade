// 声明测试类所在的包。
package langchain4j.controller;

// InputStream 用来读取 classpath 下的静态首页。
import java.io.InputStream;
// StandardCharsets 提供 UTF-8 编码常量。
import java.nio.charset.StandardCharsets;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;

// 定义静态首页的轻量回归测试。
class StaticIndexPageTest {

    // 测试首页已经接入后端会话管理 API。
    @Test
    // 方法名描述测试意图：防止前端会话侧边栏退回纯本地 localStorage 方案。
    void shouldUseBackendConversationApisInIndexPage() throws Exception {
        // 从 classpath 读取 Spring Boot 静态首页。
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("static/index.html");
        // 验证静态首页资源存在。
        assertThat(inputStream).isNotNull();
        // 把首页内容按 UTF-8 读取为字符串。
        String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        // 验证首页会调用会话管理接口。
        assertThat(html).contains("/xiaozhi/conversations");
        // 验证首页仍会调用聊天接口。
        assertThat(html).contains("/xiaozhi/chat");
        // 验证首页会调用预约挂号结构化表单接口。
        assertThat(html).contains("/xiaozhi/appointments");
        // 验证首页会调用预约号源查询接口。
        assertThat(html).contains("/xiaozhi/appointments/schedules");
        // 验证首页会调用本人预约查询接口。
        assertThat(html).contains("/xiaozhi/appointments/search");
        // 验证首页会调用取消预约接口。
        assertThat(html).contains("/cancel");
        // 验证首页会调用 RAG 回答接口。
        assertThat(html).contains("/xiaozhi/rag/answer");
        // 验证首页会调用 RAG 上下文调试接口。
        assertThat(html).contains("/xiaozhi/rag/context");
        // 验证首页会调用 RAG 样例评测报告接口。
        assertThat(html).contains("/xiaozhi/rag/evaluation/sample");
        assertThat(html).contains("/xiaozhi/medical-files");
        assertThat(html).contains("openMedicalFileModal");
        assertThat(html).contains("medicalFileInput");
        // 验证首页会调用清空当前会话消息接口。
        assertThat(html).contains("/messages");
        // 验证首页包含后端会话加载函数。
        assertThat(html).contains("loadConversationSessions");
        // 验证消息缓存版本已升级，旧版内部 prompt 缓存不会继续参与渲染。
        assertThat(html).contains("xiaozhi_message_cache_v4");
        // 验证页面初始化时会主动删除旧版消息缓存。
        assertThat(html).contains("clearLegacyMessageCaches");
        // 验证刷新或切换会话后仍会补齐固定欢迎语。
        assertThat(html).contains("withWelcomeMessage");
        // 验证首页包含后端新建会话函数。
        assertThat(html).contains("createNewSession");
        // 验证首页包含后端删除会话函数。
        assertThat(html).contains("deleteSession");
        // 验证首页包含预约表单提交函数。
        assertThat(html).contains("submitAppointmentForm");
        // 验证首页包含自定义预约日期校验函数。
        assertThat(html).contains("validateAppointmentDate");
        // 验证首页包含预约日期错误提示节点。
        assertThat(html).contains("appointmentDateError");
        // 验证预约日期缺失时前端直接提示，而不是提交到后端后再失败。
        assertThat(html).contains("请选择预约日期。");
        // 验证第 31 天补齐了统一面板状态渲染函数。
        assertThat(html).contains("renderPanelState");
        // 验证第 31 天补齐了按钮忙碌态工具函数。
        assertThat(html).contains("setButtonBusy");
        // 验证工具面板包含加载态样式。
        assertThat(html).contains(".result-panel.loading");
        // 验证工具面板包含错误态样式。
        assertThat(html).contains(".result-panel.error");
        // 验证聊天发送时会显示临时思考状态。
        assertThat(html).contains("正在思考");
        // 验证 RAG 回答有明确加载状态文案。
        assertThat(html).contains("正在生成 RAG 回答");
        // 验证预约查询有明确加载状态文案。
        assertThat(html).contains("正在查询预约");
        // 验证预约号源为空时有明确空状态。
        assertThat(html).contains("未来 30 天暂无可预约号源");
        // 验证首页包含预约号源加载函数。
        assertThat(html).contains("loadAppointmentSchedules");
        // 验证首页包含查询本人预约函数。
        assertThat(html).contains("searchAppointments");
        // 验证首页包含取消查询结果预约函数。
        assertThat(html).contains("cancelSearchedAppointment");
        // 验证首页包含 RAG 回答提交函数。
        assertThat(html).contains("submitRagAnswer");
        // 验证首页包含 RAG 上下文预览函数。
        assertThat(html).contains("previewRagContext");
        // 验证首页包含 RAG 评测报告函数。
        assertThat(html).contains("loadRagEvaluationReport");
        // 验证首页包含日期控件渲染函数，避免回退到浏览器原生日期占位。
        assertThat(html).contains("renderDateOptions");
        // 验证首页包含预约日历面板渲染函数。
        assertThat(html).contains("renderAppointmentCalendar");
        // 验证首页包含预约时间段联动渲染函数。
        assertThat(html).contains("renderTimeSlotOptions");
        // 验证预约时间不再默认选中上午，避免下午号源日期被误置灰。
        assertThat(html).contains("<option value=\"\">请选择时间</option>");
        // 验证首页包含打开预约日历函数。
        assertThat(html).contains("openAppointmentCalendar");
        // 验证首页包含日历日期格子容器。
        assertThat(html).contains("appointmentCalendarDays");
        // 验证首页包含中文日期格式化函数。
        assertThat(html).contains("formatChineseDate");
        // 验证首页不再使用原生 date 输入框，避免出现 yyyy/mm/日 这类混杂占位。
        assertThat(html).doesNotContain("type=\"date\"");
        // 验证首页会校验当前选择必须来自真实号源。
        assertThat(html).contains("selectedAppointmentSchedule");
        // 验证首页包含清空当前会话函数。
        assertThat(html).contains("clearCurrentConversation");
        // 验证首页包含会话历史加载函数。
        assertThat(html).contains("loadConversationMessages");
        // 验证首页会把后端 RAG 来源挂到助手消息下折叠展示。
        assertThat(html).contains("attachRagContextToAssistant");
        // 验证切换会话期间不会先渲染旧缓存中的内部 prompt。
        assertThat(html).contains("切换期间只显示固定欢迎语");
        // 验证首页包含助手消息 RAG 来源折叠块。
        assertThat(html).contains("message-rag-sources");
        // 验证用户需要点击才展开回答来源。
        assertThat(html).contains("查看回答来源");
        // 验证首页包含会话标题持久化函数。
        assertThat(html).contains("persistSessionTitleFromMessage");
        // 验证首页包含用户清空前的二次确认文案。
        assertThat(html).contains("清空后聊天历史将丢失，是否确定清空？");
    }
}
