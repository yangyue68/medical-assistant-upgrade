// 声明当前类所在的包，application 表示它属于 RAG 应用编排层。
package langchain4j.rag.application;

// MedicalKnowledgeSearchRequest 是底层知识检索请求。
import langchain4j.rag.MedicalKnowledgeSearchRequest;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义 RAG 生成回答请求对象。
public class MedicalRagAnswerRequest {

    // 检索请求，包含 query、topK、retrievalMode、language 等字段。
    private MedicalKnowledgeSearchRequest searchRequest;

    // 是否在响应中返回 prompt，默认不返回，避免前端无意展示过长内部提示词。
    private Boolean includePrompt;

    // 返回规整后的 includePrompt。
    public boolean includePrompt() {
        // 只有显式传 true 时才返回 prompt。
        return Boolean.TRUE.equals(includePrompt);
    }
}
