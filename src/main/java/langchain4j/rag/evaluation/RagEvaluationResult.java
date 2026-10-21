// 声明当前类所在的包，evaluation 表示它属于 RAG 评测模块。
package langchain4j.rag.evaluation;

// List 表示一组评测发现。
import java.util.List;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义 RAG 回答评测结果。
public class RagEvaluationResult {

    // 评测用例名称。
    private String caseName;

    // 整体是否通过。
    private boolean passed;

    // 简单分数，范围为 0.0 到 1.0。
    private double score;

    // 每个评测维度的详细发现。
    private List<RagEvaluationFinding> findings;

    // 创建评测结果。
    public static RagEvaluationResult of(
            // 评测用例名称。
            String caseName,
            // 整体是否通过。
            boolean passed,
            // 评测分数。
            double score,
            // 详细发现。
            List<RagEvaluationFinding> findings
    ) {
        // 创建评测结果对象。
        RagEvaluationResult result = new RagEvaluationResult();
        // 设置用例名称。
        result.setCaseName(caseName);
        // 设置整体是否通过。
        result.setPassed(passed);
        // 设置分数。
        result.setScore(score);
        // 设置详细发现。
        result.setFindings(findings);
        // 返回评测结果。
        return result;
    }
}
