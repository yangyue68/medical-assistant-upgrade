// 声明当前类所在的包，evaluation 表示它属于 RAG 评测模块。
package langchain4j.rag.evaluation;

// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义单条 RAG 评测发现。
public class RagEvaluationFinding {

    // 当前发现对应的评测维度。
    private RagEvaluationCriterion criterion;

    // 当前维度是否通过。
    private boolean passed;

    // 当前维度的人类可读说明。
    private String message;

    // 创建评测发现。
    public static RagEvaluationFinding of(
            // 评测维度。
            RagEvaluationCriterion criterion,
            // 是否通过。
            boolean passed,
            // 说明文本。
            String message
    ) {
        // 创建发现对象。
        RagEvaluationFinding finding = new RagEvaluationFinding();
        // 设置评测维度。
        finding.setCriterion(criterion);
        // 设置是否通过。
        finding.setPassed(passed);
        // 设置说明文本。
        finding.setMessage(message);
        // 返回发现对象。
        return finding;
    }
}
