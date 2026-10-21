// 声明当前类所在的包，rerank 表示它属于检索结果重排层。
package langchain4j.rag.rerank;

// MedicalKnowledgeSearchHit 是一次医学知识检索命中的结果对象。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// List 表示候选列表。
import java.util.List;

// 空重排器，用于未配置真实 rerank 服务时保持流程可运行。
public class NoOpMedicalKnowledgeReranker implements MedicalKnowledgeReranker {

    // 空实现永远表示未启用真实重排。
    @Override
    public boolean isEnabled() {
        // 返回 false，让服务层按普通 topK 检索即可。
        return false;
    }

    // 不调用外部模型，只截取前 topK 条候选。
    @Override
    public List<MedicalKnowledgeSearchHit> rerank(String query, List<MedicalKnowledgeSearchHit> candidates, int topK) {
        // 候选为空时返回空列表，否则保留原排序并限制数量。
        return candidates == null ? List.of() : candidates.stream().limit(topK).toList();
    }
}
