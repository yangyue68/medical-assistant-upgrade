// 声明当前类所在的包，store 表示它负责知识片段存取。
package langchain4j.rag.store;

// MedicalKnowledgeChunk 是需要保存和读取的知识片段。
import langchain4j.rag.MedicalKnowledgeChunk;
// ArrayList 用来创建快照列表。
import java.util.ArrayList;
// LinkedHashMap 既能按 chunkId 去重，又能保留插入顺序。
import java.util.LinkedHashMap;
// List 表示一组知识片段。
import java.util.List;
// Map 表示 chunkId 到 chunk 的映射。
import java.util.Map;
// Optional 表示可能存在也可能不存在的查询结果。
import java.util.Optional;

// 定义内存版医学知识 chunk 存储。
public class InMemoryMedicalKnowledgeChunkStore implements MedicalKnowledgeChunkStore {

    // 用 chunkId 作为 key 保存知识片段。
    private final Map<String, MedicalKnowledgeChunk> chunksById = new LinkedHashMap<>();

    // 批量保存知识片段。
    @Override
    public synchronized void saveAll(List<MedicalKnowledgeChunk> chunks) {
        // 如果传入列表为空，直接返回。
        if (chunks == null || chunks.isEmpty()) {
            // 结束保存流程。
            return;
        }
        // 遍历待保存片段。
        for (MedicalKnowledgeChunk chunk : chunks) {
            // 如果片段为空，就跳过。
            if (chunk == null) {
                // 继续处理下一个片段。
                continue;
            }
            // 如果片段 ID 为空，就拒绝保存，避免后续无法追溯。
            if (chunk.getChunkId() == null || chunk.getChunkId().trim().isEmpty()) {
                // 抛出非法参数异常。
                throw new IllegalArgumentException("chunkId must not be blank.");
            }
            // 按 chunkId 保存；重复 ID 会覆盖旧值，实现简单幂等。
            chunksById.put(chunk.getChunkId(), chunk);
        }
    }

    // 查询所有知识片段。
    @Override
    public synchronized List<MedicalKnowledgeChunk> findAll() {
        // 返回当前存储的快照，避免外部修改内部 Map。
        return new ArrayList<>(chunksById.values());
    }

    // 根据 chunkId 查询知识片段。
    @Override
    public synchronized Optional<MedicalKnowledgeChunk> findByChunkId(String chunkId) {
        // 如果 chunkId 为空，直接返回空结果。
        if (chunkId == null || chunkId.trim().isEmpty()) {
            // 返回空 Optional。
            return Optional.empty();
        }
        // 从 Map 中查询并包装成 Optional。
        return Optional.ofNullable(chunksById.get(chunkId));
    }

    // 返回当前片段数量。
    @Override
    public synchronized int count() {
        // 返回 Map 中的元素数量。
        return chunksById.size();
    }

    // 清空当前存储。
    @Override
    public synchronized void clear() {
        // 清空 Map。
        chunksById.clear();
    }
}
