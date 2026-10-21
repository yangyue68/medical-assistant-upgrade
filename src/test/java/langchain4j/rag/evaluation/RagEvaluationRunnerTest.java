// 声明测试类所在的包。
package langchain4j.rag.evaluation;

// MedicalKnowledgeCitation 表示回答引用来源。
import langchain4j.rag.MedicalKnowledgeCitation;
// RetrievalMode 表示实际检索模式。
import langchain4j.rag.RetrievalMode;
// MedicalRagAnswerResponse 是被评测的 RAG 回答响应。
import langchain4j.rag.application.MedicalRagAnswerResponse;
// List 表示一组评测用例或引用。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// AssertJ 异常断言工具。
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 定义 RAG 批量评测执行器测试。
class RagEvaluationRunnerTest {

    // 测试批量评测执行器可以汇总全部通过的套件。
    @Test
    // 方法名描述测试意图：多条评测用例全部通过时，汇总结果应该整体通过。
    void shouldSummarizeAllPassedSuite() {
        // 创建批量评测执行器。
        RagEvaluationRunner runner = new RagEvaluationRunner(new RuleBasedRagAnswerEvaluator());
        // 创建全通过评测套件。
        RagEvaluationSuite suite = RagEvaluationSuite.of(
                // 套件名称。
                "all-passed-suite",
                // 评测用例列表。
                List.of(
                        // 第一条通过用例。
                        passingCase("case-1", 1),
                        // 第二条通过用例。
                        passingCase("case-2", 2)
                )
        );

        // 执行批量评测。
        RagEvaluationSummary summary = runner.run(suite);

        // 验证整体通过。
        assertThat(summary.isPassed()).isTrue();
        // 验证总用例数量。
        assertThat(summary.getTotalCases()).isEqualTo(2);
        // 验证通过用例数量。
        assertThat(summary.getPassedCases()).isEqualTo(2);
        // 验证失败用例数量。
        assertThat(summary.getFailedCases()).isZero();
        // 验证平均分满分。
        assertThat(summary.getAverageScore()).isEqualTo(1.0);
        // 验证详细结果数量。
        assertThat(summary.getResults()).hasSize(2);
    }

    // 测试中文样例套件会识别故意缺引用的失败样例。
    @Test
    // 方法名描述测试意图：中文评测样例集中应该同时包含通过样例和失败样例。
    void shouldRunChineseSampleSuiteAndExposeMissingCitationCase() {
        // 创建批量评测执行器。
        RagEvaluationRunner runner = new RagEvaluationRunner(new RuleBasedRagAnswerEvaluator());
        // 创建中文样例套件。
        RagEvaluationSuite suite = new ChineseRagEvaluationSamples().basicMedicalSuite();

        // 执行批量评测。
        RagEvaluationSummary summary = runner.run(suite);

        // 验证套件名称是中文基础评测套件。
        assertThat(summary.getSuiteName()).isEqualTo("中文医疗 RAG 基础评测套件");
        // 验证总用例数量。
        assertThat(summary.getTotalCases()).isEqualTo(4);
        // 验证通过用例数量。
        assertThat(summary.getPassedCases()).isEqualTo(3);
        // 验证失败用例数量。
        assertThat(summary.getFailedCases()).isEqualTo(1);
        // 验证整体失败，因为其中包含一个故意失败样例。
        assertThat(summary.isPassed()).isFalse();
        // 验证失败样例名称可定位。
        assertThat(summary.getResults()).anyMatch(result -> "缺少引用失败样例".equals(result.getCaseName()) && !result.isPassed());
    }

    // 测试空套件会被拒绝。
    @Test
    // 方法名描述测试意图：批量评测必须至少包含一条用例。
    void shouldRejectEmptySuite() {
        // 创建批量评测执行器。
        RagEvaluationRunner runner = new RagEvaluationRunner(new RuleBasedRagAnswerEvaluator());
        // 创建空评测套件。
        RagEvaluationSuite suite = RagEvaluationSuite.of("empty-suite", List.of());

        // 验证空套件会抛出异常。
        assertThatThrownBy(() -> runner.run(suite))
                // 验证异常类型。
                .isInstanceOf(IllegalArgumentException.class)
                // 验证异常信息。
                .hasMessageContaining("cases");
    }

    // 测试空单条评测器会被拒绝。
    @Test
    // 方法名描述测试意图：批量执行器必须有明确的单条评测器。
    void shouldRejectNullEvaluator() {
        // 验证构造器拒绝空评测器。
        assertThatThrownBy(() -> new RagEvaluationRunner(null))
                // 验证异常类型。
                .isInstanceOf(IllegalArgumentException.class)
                // 验证异常信息。
                .hasMessageContaining("evaluator");
    }

    // 创建通过用例。
    private static RagEvaluationCase passingCase(String caseName, int citationNumber) {
        // 创建回答响应。
        MedicalRagAnswerResponse response = MedicalRagAnswerResponse.of(
                // 用户问题。
                "测试问题",
                // 检索模式。
                RetrievalMode.HYBRID,
                // 回答文本。
                "以下内容仅供健康科普和就医参考，不能替代医生诊断。测试回答 [" + citationNumber + "]。",
                // 引用列表。
                List.of(citation(citationNumber)),
                // prompt 不参与规则评测。
                null
        );
        // 返回评测用例。
        return RagEvaluationCase.of(caseName, response, true, false, true);
    }

    // 创建引用对象。
    private static MedicalKnowledgeCitation citation(int citationNumber) {
        // 创建引用对象。
        MedicalKnowledgeCitation citation = new MedicalKnowledgeCitation();
        // 设置引用编号。
        citation.setCitationNumber(citationNumber);
        // 设置 chunkId。
        citation.setChunkId("test:doc#chunk-" + citationNumber);
        // 设置标题。
        citation.setTitle("测试文档");
        // 设置来源名称。
        citation.setSourceName("测试来源");
        // 设置来源 URL。
        citation.setSourceUrl("https://example.org/test");
        // 返回引用对象。
        return citation;
    }
}
