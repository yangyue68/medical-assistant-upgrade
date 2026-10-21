// 声明当前类所在的包。
package langchain4j.rag;

// List 表示一组检索命中项。
import java.util.List;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义医学知识检索响应对象。
public class MedicalKnowledgeSearchResult {

    // 原始查询问题。
    private String query;

    // 实际使用的检索模式。
    private RetrievalMode retrievalMode;

    // 检索命中列表。
    private List<MedicalKnowledgeSearchHit> hits;

    // 检索命中对应的引用列表。
    private List<MedicalKnowledgeCitation> citations;

    // 创建检索结果对象。
    public static MedicalKnowledgeSearchResult of(
            // 原始查询问题。
            String query,
            // 实际使用的检索模式。
            RetrievalMode retrievalMode,
            // 命中列表。
            List<MedicalKnowledgeSearchHit> hits
    ) {
        // 创建结果对象。
        MedicalKnowledgeSearchResult result = new MedicalKnowledgeSearchResult();
        // 设置查询问题。
        result.setQuery(query);
        // 设置检索模式。
        result.setRetrievalMode(retrievalMode);
        // 设置命中列表。
        result.setHits(hits);
        // 根据命中列表构建引用列表。
        result.setCitations(new MedicalKnowledgeCitationBuilder().build(hits));
        // 返回结果对象。
        return result;
    }
}
