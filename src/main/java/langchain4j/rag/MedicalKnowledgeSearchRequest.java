// 声明当前类所在的包。
package langchain4j.rag;

// ArrayList 用来创建可变错误列表。
import java.util.ArrayList;
// List 表示一组文档类型或校验错误。
import java.util.List;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义医学知识检索请求对象，后续由 RAG Controller、Agent 或服务层使用。
public class MedicalKnowledgeSearchRequest {

    // 默认返回条数。
    private static final int DEFAULT_TOP_K = 5;

    // 最大返回条数，避免一次检索塞太多内容进模型上下文。
    private static final int MAX_TOP_K = 20;

    // 用户检索问题。
    private String query;

    // 期望返回条数。
    private Integer topK;

    // 检索模式，默认后续会走 HYBRID。
    private RetrievalMode retrievalMode;

    // 语言过滤条件，例如 English、Chinese。
    private String language;

    // 科室过滤条件。
    private String department;

    // 文档类型过滤条件。
    private List<MedicalKnowledgeDocumentType> documentTypes;

    // 返回规整后的 topK。
    public int normalizedTopK() {
        // 如果调用方没有传 topK，就使用默认值。
        if (topK == null) {
            // 返回默认 topK。
            return DEFAULT_TOP_K;
        }
        // 如果 topK 小于 1，就规整成 1。
        if (topK < 1) {
            // 返回最小值。
            return 1;
        }
        // 如果 topK 大于最大值，就规整成最大值。
        if (topK > MAX_TOP_K) {
            // 返回最大值。
            return MAX_TOP_K;
        }
        // 返回调用方传入的合法 topK。
        return topK;
    }

    // 返回规整后的检索模式。
    public RetrievalMode normalizedRetrievalMode() {
        // 如果调用方没有指定检索模式，就默认使用混合检索。
        if (retrievalMode == null) {
            // 返回 HYBRID。
            return RetrievalMode.HYBRID;
        }
        // 返回调用方指定的检索模式。
        return retrievalMode;
    }

    // 校验检索请求。
    public List<String> validate() {
        // 创建错误列表。
        List<String> errors = new ArrayList<>();
        // 如果查询为空，就记录错误。
        if (query == null || query.trim().isEmpty()) {
            // 添加查询缺失错误。
            errors.add("检索问题不能为空。");
        }
        // 返回错误列表；空列表表示校验通过。
        return errors;
    }
}
