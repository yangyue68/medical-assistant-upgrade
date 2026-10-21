// 声明测试类所在的包。
package langchain4j.rag.evaluation;

// MedicalKnowledgeCitation 表示回答引用来源。
import langchain4j.rag.MedicalKnowledgeCitation;
// RetrievalMode 表示实际检索模式。
import langchain4j.rag.RetrievalMode;
// MedicalRagAnswerResponse 是被评测的 RAG 回答响应。
import langchain4j.rag.application.MedicalRagAnswerResponse;
// List 表示一组引用或发现。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// AssertJ 异常断言工具。
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 定义本地规则 RAG 回答评测器测试。
class RuleBasedRagAnswerEvaluatorTest {

    // 测试带引用和安全边界的回答可以通过评测。
    @Test
    // 方法名描述测试意图：正常 RAG 回答应该通过引用覆盖和医疗安全边界评测。
    void shouldPassWhenAnswerHasCitationAndSafetyBoundary() {
        // 创建评测器。
        RuleBasedRagAnswerEvaluator evaluator = new RuleBasedRagAnswerEvaluator();
        // 创建带引用和安全边界的回答。
        MedicalRagAnswerResponse response = answerResponse(
                // 回答文本。
                "以下内容仅供健康科普和就医参考，不能替代医生诊断。糖尿病常见表现包括口渴、多尿和乏力 [1]。",
                // 引用列表。
                List.of(citation(1))
        );
        // 创建评测用例。
        RagEvaluationCase evaluationCase = RagEvaluationCase.of("diabetes-with-citation", response, true, false, true);

        // 执行评测。
        RagEvaluationResult result = evaluator.evaluate(evaluationCase);

        // 验证整体通过。
        assertThat(result.isPassed()).isTrue();
        // 验证分数为满分。
        assertThat(result.getScore()).isEqualTo(1.0);
        // 验证包含引用覆盖发现。
        assertThat(result.getFindings()).anyMatch(finding -> finding.getCriterion() == RagEvaluationCriterion.CITATION_COVERAGE && finding.isPassed());
        // 验证包含医疗安全边界发现。
        assertThat(result.getFindings()).anyMatch(finding -> finding.getCriterion() == RagEvaluationCriterion.MEDICAL_SAFETY_BOUNDARY && finding.isPassed());
    }

    // 测试资料不足回答可以通过拒答评测。
    @Test
    // 方法名描述测试意图：没有证据时回答应该明确说明资料不足。
    void shouldPassWhenInsufficientEvidenceAnswerRefusesToFabricate() {
        // 创建评测器。
        RuleBasedRagAnswerEvaluator evaluator = new RuleBasedRagAnswerEvaluator();
        // 创建资料不足回答。
        MedicalRagAnswerResponse response = answerResponse(
                // 回答文本。
                "目前没有检索到足够可靠的医学知识片段，不能基于资料给出明确回答。请补充信息或咨询专业医生。",
                // 引用列表。
                List.of()
        );
        // 创建评测用例。
        RagEvaluationCase evaluationCase = RagEvaluationCase.of("insufficient-evidence", response, false, true, true);

        // 执行评测。
        RagEvaluationResult result = evaluator.evaluate(evaluationCase);

        // 验证整体通过。
        assertThat(result.isPassed()).isTrue();
        // 验证包含资料不足拒答发现。
        assertThat(result.getFindings()).anyMatch(finding -> finding.getCriterion() == RagEvaluationCriterion.INSUFFICIENT_EVIDENCE_REFUSAL && finding.isPassed());
        // 验证包含医疗安全边界发现。
        assertThat(result.getFindings()).anyMatch(finding -> finding.getCriterion() == RagEvaluationCriterion.MEDICAL_SAFETY_BOUNDARY && finding.isPassed());
    }

    // 测试缺少引用和安全边界时会失败。
    @Test
    // 方法名描述测试意图：评测器应该能发现回答不可追溯且缺少医疗安全提示。
    void shouldFailWhenCitationAndSafetyBoundaryAreMissing() {
        // 创建评测器。
        RuleBasedRagAnswerEvaluator evaluator = new RuleBasedRagAnswerEvaluator();
        // 创建缺少引用标记和安全边界的回答。
        MedicalRagAnswerResponse response = answerResponse(
                // 回答文本。
                "糖尿病早期表现包括口渴、多尿和乏力。",
                // 引用列表。
                List.of(citation(1))
        );
        // 创建评测用例。
        RagEvaluationCase evaluationCase = RagEvaluationCase.of("missing-citation-and-safety", response, true, false, true);

        // 执行评测。
        RagEvaluationResult result = evaluator.evaluate(evaluationCase);

        // 验证整体失败。
        assertThat(result.isPassed()).isFalse();
        // 验证分数小于满分。
        assertThat(result.getScore()).isLessThan(1.0);
        // 验证引用覆盖失败。
        assertThat(result.getFindings()).anyMatch(finding -> finding.getCriterion() == RagEvaluationCriterion.CITATION_COVERAGE && !finding.isPassed());
        // 验证医疗安全边界失败。
        assertThat(result.getFindings()).anyMatch(finding -> finding.getCriterion() == RagEvaluationCriterion.MEDICAL_SAFETY_BOUNDARY && !finding.isPassed());
    }

    // 测试空评测用例会被拒绝。
    @Test
    // 方法名描述测试意图：评测器不接受空输入。
    void shouldRejectNullEvaluationCase() {
        // 创建评测器。
        RuleBasedRagAnswerEvaluator evaluator = new RuleBasedRagAnswerEvaluator();

        // 验证空用例会抛出异常。
        assertThatThrownBy(() -> evaluator.evaluate(null))
                // 验证异常类型。
                .isInstanceOf(IllegalArgumentException.class)
                // 验证异常信息。
                .hasMessageContaining("evaluation case");
    }

    // 创建测试回答响应。
    private static MedicalRagAnswerResponse answerResponse(String answer, List<MedicalKnowledgeCitation> citations) {
        // 返回回答响应。
        return MedicalRagAnswerResponse.of(
                // 用户问题。
                "糖尿病早期有哪些症状",
                // 检索模式。
                RetrievalMode.HYBRID,
                // 回答文本。
                answer,
                // 引用列表。
                citations,
                // prompt 不参与本次评测。
                null
        );
    }

    // 创建测试引用。
    private static MedicalKnowledgeCitation citation(int citationNumber) {
        // 创建引用对象。
        MedicalKnowledgeCitation citation = new MedicalKnowledgeCitation();
        // 设置引用编号。
        citation.setCitationNumber(citationNumber);
        // 设置 chunkId。
        citation.setChunkId("demo:diabetes#chunk-0");
        // 设置标题。
        citation.setTitle("糖尿病健康科普");
        // 设置来源名称。
        citation.setSourceName("项目内置演示知识");
        // 设置来源 URL。
        citation.setSourceUrl("internal://demo/diabetes");
        // 返回引用。
        return citation;
    }
}
