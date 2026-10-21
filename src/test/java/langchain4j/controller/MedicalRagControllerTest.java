// 声明测试类所在的包。
package langchain4j.controller;

// ApiResponse 是统一接口响应结构。
import langchain4j.common.ApiResponse;
// MedicalKnowledgeSearchRequest 是 RAG 检索请求。
import langchain4j.rag.MedicalKnowledgeSearchRequest;
// RetrievalMode 是检索模式枚举。
import langchain4j.rag.RetrievalMode;
// MedicalRagAnswerRequest 是 RAG 回答生成请求。
import langchain4j.rag.application.MedicalRagAnswerRequest;
// MedicalRagAnswerResponse 是 RAG 回答生成响应。
import langchain4j.rag.application.MedicalRagAnswerResponse;
// MedicalRagContext 是 RAG 接口返回的上下文对象。
import langchain4j.rag.application.MedicalRagContext;
// MedicalRagService 是 RAG 应用服务。
import langchain4j.rag.application.MedicalRagService;
// RagEvaluationSummary 是 RAG 评测汇总结果。
import langchain4j.rag.evaluation.RagEvaluationSummary;
// List 表示一组命中或引用。
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

// 定义 RAG Controller 的单元测试。
class MedicalRagControllerTest {

    // 测试 RAG context 接口会委托应用服务并返回统一响应。
    @Test
    // 方法名描述测试意图：Controller 应该返回 RAG 上下文预览结果。
    void shouldReturnRagContextPreview() {
        // 创建 RAG 应用服务 mock。
        MedicalRagService medicalRagService = mock(MedicalRagService.class);
        // 创建被测 Controller。
        MedicalRagController controller = new MedicalRagController(medicalRagService);
        // 创建检索请求。
        MedicalKnowledgeSearchRequest request = request();
        // 创建 RAG 上下文。
        MedicalRagContext context = MedicalRagContext.of(
                // 用户问题。
                "糖尿病早期症状",
                // 检索模式。
                RetrievalMode.HYBRID,
                // 上下文文本。
                "【可引用医学知识片段】糖尿病可能出现口渴、多尿等表现。",
                // 安全提示。
                "不能替代医生诊断。",
                // 命中列表。
                List.of(),
                // 引用列表。
                List.of()
        );
        // 指定应用服务返回上下文。
        when(medicalRagService.prepareContext(eq(request))).thenReturn(context);

        // 调用 Controller 接口方法。
        ApiResponse<MedicalRagContext> response = controller.context(request);

        // 验证统一响应码为成功。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证响应 data 是应用服务返回的上下文。
        assertThat(response.getData()).isEqualTo(context);
        // 验证上下文检索模式正确。
        assertThat(response.getData().getRetrievalMode()).isEqualTo(RetrievalMode.HYBRID);
        // 验证 Controller 委托了 RAG 应用服务。
        verify(medicalRagService).prepareContext(request);
    }

    // 测试 RAG answer 接口会委托应用服务并返回统一响应。
    @Test
    // 方法名描述测试意图：Controller 应该返回带引用的 RAG 回答。
    void shouldReturnRagAnswer() {
        // 创建 RAG 应用服务 mock。
        MedicalRagService medicalRagService = mock(MedicalRagService.class);
        // 创建被测 Controller。
        MedicalRagController controller = new MedicalRagController(medicalRagService);
        // 创建回答请求。
        MedicalRagAnswerRequest request = new MedicalRagAnswerRequest();
        // 设置内部检索请求。
        request.setSearchRequest(request());
        // 创建回答响应。
        MedicalRagAnswerResponse answerResponse = MedicalRagAnswerResponse.of(
                // 用户问题。
                "糖尿病早期症状",
                // 检索模式。
                RetrievalMode.HYBRID,
                // 回答文本。
                "糖尿病可能出现口渴、多尿等表现。[1]",
                // 引用列表。
                List.of(),
                // prompt 不返回。
                null
        );
        // 指定应用服务返回回答。
        when(medicalRagService.answer(eq(request))).thenReturn(answerResponse);

        // 调用 Controller 接口方法。
        ApiResponse<MedicalRagAnswerResponse> response = controller.answer(request);

        // 验证统一响应码为成功。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证响应 data 是应用服务返回的回答。
        assertThat(response.getData()).isEqualTo(answerResponse);
        // 验证回答内容正确。
        assertThat(response.getData().getAnswer()).contains("糖尿病");
        // 验证 Controller 委托了 RAG 应用服务。
        verify(medicalRagService).answer(request);
    }

    // 测试 RAG 样例评测接口会返回汇总报告。
    @Test
    // 方法名描述测试意图：前端演示页应该能获取 RAG 评测通过数、失败数和平均分。
    void shouldReturnSampleEvaluationReport() {
        // 创建 RAG 应用服务 mock。
        MedicalRagService medicalRagService = mock(MedicalRagService.class);
        // 创建被测 Controller。
        MedicalRagController controller = new MedicalRagController(medicalRagService);

        // 调用样例评测报告接口。
        ApiResponse<RagEvaluationSummary> response = controller.sampleEvaluation();

        // 验证统一响应码为成功。
        assertThat(response.getCode()).isEqualTo(0);
        // 验证报告名称正确。
        assertThat(response.getData().getSuiteName()).isEqualTo("中文医疗 RAG 基础评测套件");
        // 验证报告包含 4 条内置样例。
        assertThat(response.getData().getTotalCases()).isEqualTo(4);
        // 验证报告暴露故意失败的引用样例。
        assertThat(response.getData().getFailedCases()).isEqualTo(1);
    }

    // 创建测试检索请求。
    private static MedicalKnowledgeSearchRequest request() {
        // 创建请求对象。
        MedicalKnowledgeSearchRequest request = new MedicalKnowledgeSearchRequest();
        // 设置查询问题。
        request.setQuery("糖尿病早期症状");
        // 设置检索模式。
        request.setRetrievalMode(RetrievalMode.HYBRID);
        // 设置返回数量。
        request.setTopK(3);
        // 设置语言。
        request.setLanguage("Chinese");
        // 返回请求对象。
        return request;
    }
}
