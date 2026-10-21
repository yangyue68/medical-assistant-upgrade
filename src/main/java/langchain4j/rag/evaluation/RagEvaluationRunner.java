// 声明当前类所在的包，evaluation 表示它属于 RAG 评测模块。
package langchain4j.rag.evaluation;

// ArrayList 用来收集多条评测结果。
import java.util.ArrayList;
// List 表示一组评测用例或评测结果。
import java.util.List;

// 定义 RAG 批量评测执行器。
public class RagEvaluationRunner {

    // 保存单条回答评测器。
    private final RuleBasedRagAnswerEvaluator evaluator;

    // 创建批量评测执行器。
    public RagEvaluationRunner(
            // 注入单条回答评测器。
            RuleBasedRagAnswerEvaluator evaluator
    ) {
        // 如果评测器为空，说明调用方没有提供评测规则。
        if (evaluator == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("evaluator must not be null.");
        }
        // 保存单条评测器。
        this.evaluator = evaluator;
    }

    // 执行一整套 RAG 评测用例。
    public RagEvaluationSummary run(RagEvaluationSuite suite) {
        // 校验评测套件。
        validateSuite(suite);
        // 创建结果列表。
        List<RagEvaluationResult> results = new ArrayList<>();
        // 遍历套件中的每条用例。
        for (RagEvaluationCase evaluationCase : suite.getCases()) {
            // 执行单条用例评测。
            RagEvaluationResult result = evaluator.evaluate(evaluationCase);
            // 收集单条评测结果。
            results.add(result);
        }
        // 计算总用例数量。
        int totalCases = results.size();
        // 计算通过用例数量。
        int passedCases = countPassedCases(results);
        // 计算失败用例数量。
        int failedCases = totalCases - passedCases;
        // 计算平均分。
        double averageScore = averageScore(results);
        // 判断整套评测是否全部通过。
        boolean passed = failedCases == 0;
        // 返回批量评测汇总。
        return RagEvaluationSummary.of(suite.getSuiteName(), passed, totalCases, passedCases, failedCases, averageScore, results);
    }

    // 校验评测套件。
    private void validateSuite(RagEvaluationSuite suite) {
        // 如果套件为空，说明调用方错误。
        if (suite == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("RAG evaluation suite must not be null.");
        }
        // 如果用例列表为空，说明没有可执行的评测。
        if (suite.getCases() == null || suite.getCases().isEmpty()) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("RAG evaluation suite cases must not be empty.");
        }
    }

    // 统计通过用例数量。
    private int countPassedCases(List<RagEvaluationResult> results) {
        // 初始化计数器。
        int count = 0;
        // 遍历结果列表。
        for (RagEvaluationResult result : results) {
            // 如果当前结果通过，就计数加一。
            if (result.isPassed()) {
                // 通过数量加一。
                count++;
            }
        }
        // 返回通过用例数量。
        return count;
    }

    // 计算平均分。
    private double averageScore(List<RagEvaluationResult> results) {
        // 初始化总分。
        double totalScore = 0.0;
        // 遍历结果列表。
        for (RagEvaluationResult result : results) {
            // 累加当前用例分数。
            totalScore += result.getScore();
        }
        // 返回平均分。
        return totalScore / results.size();
    }
}
