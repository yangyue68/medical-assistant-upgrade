// 声明当前类所在的包。
package langchain4j.rag;

// ArrayList 用来收集引用对象。
import java.util.ArrayList;
// LinkedHashSet 用来按出现顺序去重引用 key。
import java.util.LinkedHashSet;
// List 表示一组检索命中或引用。
import java.util.List;
// Set 表示去重后的引用 key 集合。
import java.util.Set;

// 定义医学知识引用构建器。
public class MedicalKnowledgeCitationBuilder {

    // 根据检索命中列表构建引用列表。
    public List<MedicalKnowledgeCitation> build(List<MedicalKnowledgeSearchHit> hits) {
        // 如果命中列表为空，返回空引用列表。
        if (hits == null || hits.isEmpty()) {
            // 返回不可变空列表。
            return List.of();
        }
        // 创建引用列表。
        List<MedicalKnowledgeCitation> citations = new ArrayList<>();
        // 创建去重 key 集合。
        Set<String> seenKeys = new LinkedHashSet<>();
        // 遍历检索命中。
        for (MedicalKnowledgeSearchHit hit : hits) {
            // 如果命中为空，跳过。
            if (hit == null) {
                // 继续处理下一个命中。
                continue;
            }
            // 构建引用去重 key。
            String citationKey = citationKey(hit);
            // 如果引用 key 已经出现过，就跳过。
            if (seenKeys.contains(citationKey)) {
                // 继续处理下一个命中。
                continue;
            }
            // 记录已经出现过的引用 key。
            seenKeys.add(citationKey);
            // 创建引用并加入结果列表。
            citations.add(MedicalKnowledgeCitation.from(citations.size() + 1, hit));
        }
        // 返回引用列表。
        return citations;
    }

    // 构建引用去重 key。
    private String citationKey(MedicalKnowledgeSearchHit hit) {
        // 如果 chunkId 存在，优先用 chunkId 去重，因为它最精确。
        if (hit.getChunkId() != null && !hit.getChunkId().trim().isEmpty()) {
            // 返回 chunkId。
            return "chunk:" + hit.getChunkId();
        }
        // 如果来源 URL 存在，就用 URL 去重。
        if (hit.getSourceUrl() != null && !hit.getSourceUrl().trim().isEmpty()) {
            // 返回来源 URL。
            return "url:" + hit.getSourceUrl();
        }
        // 最后退回文档 ID。
        return "document:" + hit.getDocumentId();
    }
}
