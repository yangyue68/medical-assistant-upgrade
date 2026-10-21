// 声明当前类所在的包，application 表示它属于 RAG 应用编排层。
package langchain4j.rag.application;

// MedicalKnowledgeCitation 表示回答引用来源。
import langchain4j.rag.MedicalKnowledgeCitation;
// RetrievalMode 表示实际检索模式。
import langchain4j.rag.RetrievalMode;
// List 表示一组引用。
import java.util.List;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义 RAG 生成回答响应对象。
public class MedicalRagAnswerResponse {

    // 用户原始问题。
    private String query;

    // 实际检索模式。
    private RetrievalMode retrievalMode;

    // 最终回答文本。
    private String answer;

    // 回答引用列表。
    private List<MedicalKnowledgeCitation> citations;

    // 实际组装出的 prompt；只有 includePrompt=true 时返回。
    private String prompt;

    // 创建 RAG 回答响应对象。
    public static MedicalRagAnswerResponse of(
            // 用户原始问题。
            String query,
            // 实际检索模式。
            RetrievalMode retrievalMode,
            // 最终回答文本。
            String answer,
            // 引用列表。
            List<MedicalKnowledgeCitation> citations,
            // prompt 文本。
            String prompt
    ) {
        // 创建响应对象。
        MedicalRagAnswerResponse response = new MedicalRagAnswerResponse();
        // 设置用户原始问题。
        response.setQuery(query);
        // 设置实际检索模式。
        response.setRetrievalMode(retrievalMode);
        // 设置最终回答。
        response.setAnswer(answer);
        // 设置引用列表。
        response.setCitations(citations);
        // 设置 prompt。
        response.setPrompt(prompt);
        // 返回响应对象。
        return response;
    }
}
