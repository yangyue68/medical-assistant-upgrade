// 声明当前接口所在的包，store 表示它负责知识片段存取边界。
package langchain4j.rag.store;

// MedicalKnowledgeChunk 是需要保存和读取的知识片段。
import langchain4j.rag.MedicalKnowledgeChunk;
// List 表示一组知识片段。
import java.util.List;
// Optional 表示可能存在也可能不存在的查询结果。
import java.util.Optional;

// 定义医学知识 chunk 存储接口，为内存、文件、数据库、OpenSearch 等实现预留统一边界。
public interface MedicalKnowledgeChunkStore {

    // 批量保存知识片段。
    void saveAll(List<MedicalKnowledgeChunk> chunks);

    // 查询所有知识片段。
    List<MedicalKnowledgeChunk> findAll();

    // 根据 chunkId 查询单个知识片段。
    Optional<MedicalKnowledgeChunk> findByChunkId(String chunkId);

    // 返回当前存储中的知识片段数量。
    int count();

    // 清空当前存储，主要用于测试和重新构建索引。
    void clear();
}
