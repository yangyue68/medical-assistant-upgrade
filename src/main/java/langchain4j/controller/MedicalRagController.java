// 声明当前类所在的包，controller 表示它负责处理 HTTP 请求。
package langchain4j.controller;

// ApiResponse 是项目统一的非流式接口返回结构。
import langchain4j.common.ApiResponse;
// MedicalKnowledgeSearchRequest 是 RAG 检索请求。
import langchain4j.rag.MedicalKnowledgeSearchRequest;
// MedicalRagAnswerRequest 是 RAG 回答生成请求。
import langchain4j.rag.application.MedicalRagAnswerRequest;
// MedicalRagAnswerResponse 是 RAG 回答生成响应。
import langchain4j.rag.application.MedicalRagAnswerResponse;
// MedicalRagContext 是 RAG 应用服务返回的 prompt-ready 上下文。
import langchain4j.rag.application.MedicalRagContext;
// MedicalRagService 是 RAG 应用编排服务。
import langchain4j.rag.application.MedicalRagService;
// ChineseRagEvaluationSamples 提供项目内置中文 RAG 评测样例。
import langchain4j.rag.evaluation.ChineseRagEvaluationSamples;
// RagEvaluationRunner 是 RAG 批量评测执行器。
import langchain4j.rag.evaluation.RagEvaluationRunner;
// RagEvaluationSummary 是 RAG 批量评测汇总结果。
import langchain4j.rag.evaluation.RagEvaluationSummary;
// RuleBasedRagAnswerEvaluator 是本地规则评测器。
import langchain4j.rag.evaluation.RuleBasedRagAnswerEvaluator;
// SensitiveDataMasker 用来在日志中隐藏敏感信息。
import langchain4j.util.SensitiveDataMasker;
// Operation 用来给 Knife4j/OpenAPI 文档补充接口说明。
import io.swagger.v3.oas.annotations.Operation;
// Tag 用来给 Knife4j/OpenAPI 文档中的接口分组。
import io.swagger.v3.oas.annotations.tags.Tag;
// Logger 是 SLF4J 的日志接口。
import org.slf4j.Logger;
// LoggerFactory 用来创建当前类对应的日志对象。
import org.slf4j.LoggerFactory;
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

// 给 OpenAPI 文档添加接口分组名称。
@Tag(name = "RAG 医学知识检索")
// 标记当前类为 REST Controller。
@RestController
// 给当前类下所有接口加上 /xiaozhi/rag 前缀。
@RequestMapping("/xiaozhi/rag")
// 定义 RAG 医学知识检索 HTTP 接口控制器。
public class MedicalRagController {

    // 创建当前类的日志对象。
    private static final Logger log = LoggerFactory.getLogger(MedicalRagController.class);

    // 保存 RAG 应用服务依赖。
    private final MedicalRagService medicalRagService;

    // 构造器注入 RAG 应用服务。
    public MedicalRagController(
            // Spring 会自动注入 MedicalRagService Bean。
            MedicalRagService medicalRagService
    ) {
        // 保存 RAG 应用服务。
        this.medicalRagService = medicalRagService;
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "预览 RAG 检索上下文")
    // 声明 POST /xiaozhi/rag/context 接口。
    @PostMapping("/context")
    // context 方法返回检索命中、引用和 prompt-ready 上下文，方便前端和调试使用。
    public ApiResponse<MedicalRagContext> context(@RequestBody MedicalKnowledgeSearchRequest request) {
        // 记录 RAG 请求日志，用户问题先做脱敏。
        log.info(
                // 日志模板记录检索模式、topK、语言和脱敏后的 query。
                "RAG context request. mode={}, topK={}, language={}, query={}",
                // 请求为空时模式记为 null。
                request == null ? null : request.getRetrievalMode(),
                // 请求为空时 topK 记为 null。
                request == null ? null : request.getTopK(),
                // 请求为空时 language 记为 null。
                request == null ? null : request.getLanguage(),
                // 请求为空时 query 记为 null，否则脱敏。
                request == null ? null : SensitiveDataMasker.mask(request.getQuery())
        );
        // 委托 RAG 应用服务准备上下文。
        MedicalRagContext context = medicalRagService.prepareContext(request);
        // 返回统一成功响应。
        return ApiResponse.success(context);
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "生成带引用的 RAG 回答")
    // 声明 POST /xiaozhi/rag/answer 接口。
    @PostMapping("/answer")
    // answer 方法返回基于 RAG 上下文生成的回答和引用。
    public ApiResponse<MedicalRagAnswerResponse> answer(@RequestBody MedicalRagAnswerRequest request) {
        // 记录 RAG 回答请求日志，用户问题先做脱敏。
        log.info(
                // 日志模板记录检索模式、topK、语言和脱敏后的 query。
                "RAG answer request. mode={}, topK={}, language={}, query={}",
                // 请求为空时模式记为 null。
                request == null || request.getSearchRequest() == null ? null : request.getSearchRequest().getRetrievalMode(),
                // 请求为空时 topK 记为 null。
                request == null || request.getSearchRequest() == null ? null : request.getSearchRequest().getTopK(),
                // 请求为空时 language 记为 null。
                request == null || request.getSearchRequest() == null ? null : request.getSearchRequest().getLanguage(),
                // 请求为空时 query 记为 null，否则脱敏。
                request == null || request.getSearchRequest() == null ? null : SensitiveDataMasker.mask(request.getSearchRequest().getQuery())
        );
        // 委托 RAG 应用服务生成回答。
        MedicalRagAnswerResponse response = medicalRagService.answer(request);
        // 返回统一成功响应。
        return ApiResponse.success(response);
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "生成 RAG 样例评测报告")
    // 声明 GET /xiaozhi/rag/evaluation/sample 接口。
    @GetMapping("/evaluation/sample")
    // sampleEvaluation 方法返回项目内置中文样例套件的评测摘要。
    public ApiResponse<RagEvaluationSummary> sampleEvaluation() {
        // 创建本地规则评测器。
        RuleBasedRagAnswerEvaluator evaluator = new RuleBasedRagAnswerEvaluator();
        // 创建批量评测执行器。
        RagEvaluationRunner runner = new RagEvaluationRunner(evaluator);
        // 创建中文基础样例套件。
        ChineseRagEvaluationSamples samples = new ChineseRagEvaluationSamples();
        // 执行样例评测。
        RagEvaluationSummary summary = runner.run(samples.basicMedicalSuite());
        // 返回统一成功响应。
        return ApiResponse.success(summary);
    }
}
