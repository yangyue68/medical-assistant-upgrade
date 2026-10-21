// 声明当前类所在的包，evaluation 表示它属于 RAG 评测模块。
package langchain4j.rag.evaluation;

// List 表示一组评测用例。
import java.util.List;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义 RAG 批量评测套件。
public class RagEvaluationSuite {

    // 评测套件名称，方便区分中文基础评测、急症安全评测、引用忠实度评测等不同集合。
    private String suiteName;

    // 当前套件中的评测用例列表。
    private List<RagEvaluationCase> cases;

    // 创建评测套件。
    public static RagEvaluationSuite of(
            // 评测套件名称。
            String suiteName,
            // 评测用例列表。
            List<RagEvaluationCase> cases
    ) {
        // 创建评测套件对象。
        RagEvaluationSuite suite = new RagEvaluationSuite();
        // 设置评测套件名称。
        suite.setSuiteName(suiteName);
        // 设置评测用例列表。
        suite.setCases(cases);
        // 返回评测套件。
        return suite;
    }
}
