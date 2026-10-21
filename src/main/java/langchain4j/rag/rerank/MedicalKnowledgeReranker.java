// 声明当前类所在的包，rerank 表示它属于检索结果重排层。
package langchain4j.rag.rerank;

// MedicalKnowledgeSearchHit 是一次医学知识检索命中的结果对象。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// List 表示候选命中列表和重排后的命中列表。
import java.util.List;

// 定义医学知识重排器接口，隐藏 DashScope 或空实现等具体策略。
public interface MedicalKnowledgeReranker {

    // 返回当前重排器是否真正启用，方便服务层决定候选池大小。
    boolean isEnabled();

    // 根据用户 query 对召回候选进行二次排序，并最多返回 topK 条。
    List<MedicalKnowledgeSearchHit> rerank(String query, List<MedicalKnowledgeSearchHit> candidates, int topK);
}
