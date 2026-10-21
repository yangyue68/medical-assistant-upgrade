// 声明当前类所在的包，evaluation 表示它属于 RAG 评测模块。
package langchain4j.rag.evaluation;

// List 表示一组单条评测结果。
import java.util.List;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义 RAG 批量评测汇总结果。
public class RagEvaluationSummary {

    // 评测套件名称。
    private String suiteName;

    // 整体是否通过。
    private boolean passed;

    // 总用例数量。
    private int totalCases;

    // 通过用例数量。
    private int passedCases;

    // 失败用例数量。
    private int failedCases;

    // 平均分，范围为 0.0 到 1.0。
    private double averageScore;

    // 每条用例的详细评测结果。
    private List<RagEvaluationResult> results;

    // 创建批量评测汇总结果。
    public static RagEvaluationSummary of(
            // 评测套件名称。
            String suiteName,
            // 整体是否通过。
            boolean passed,
            // 总用例数量。
            int totalCases,
            // 通过用例数量。
            int passedCases,
            // 失败用例数量。
            int failedCases,
            // 平均分。
            double averageScore,
            // 详细结果列表。
            List<RagEvaluationResult> results
    ) {
        // 创建汇总对象。
        RagEvaluationSummary summary = new RagEvaluationSummary();
        // 设置评测套件名称。
        summary.setSuiteName(suiteName);
        // 设置整体是否通过。
        summary.setPassed(passed);
        // 设置总用例数量。
        summary.setTotalCases(totalCases);
        // 设置通过用例数量。
        summary.setPassedCases(passedCases);
        // 设置失败用例数量。
        summary.setFailedCases(failedCases);
        // 设置平均分。
        summary.setAverageScore(averageScore);
        // 设置详细结果。
        summary.setResults(results);
        // 返回汇总对象。
        return summary;
    }
}
