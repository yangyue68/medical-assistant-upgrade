// 声明当前类所在的包，ingestion 表示它负责知识数据导入。
package langchain4j.rag.ingestion;

// MedicalKnowledgeChunk 是切分后的可检索知识片段。
import langchain4j.rag.MedicalKnowledgeChunk;
// MedicalKnowledgeDocument 是解析出来的完整医学知识文档。
import langchain4j.rag.MedicalKnowledgeDocument;
// MedicalKnowledgeChunkStore 是 chunk 存储边界。
import langchain4j.rag.store.MedicalKnowledgeChunkStore;
// List 表示一组文档或 chunk。
import java.util.List;

// 定义医学知识导入服务，把“解析、切分、保存”串成稳定流水线。
public class MedicalKnowledgeIngestionService {

    // 医学知识文档切分器。
    private final MedicalKnowledgeChunker chunker;

    // 医学知识 chunk 存储边界。
    private final MedicalKnowledgeChunkStore chunkStore;

    // 创建医学知识导入服务。
    public MedicalKnowledgeIngestionService(
            // 医学知识文档切分器。
            MedicalKnowledgeChunker chunker,
            // 医学知识 chunk 存储边界。
            MedicalKnowledgeChunkStore chunkStore
    ) {
        // 如果切分器为空，说明服务无法把文档转换成可检索片段。
        if (chunker == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("chunker must not be null.");
        }
        // 如果 chunk 存储为空，说明导入结果无法保存。
        if (chunkStore == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("chunkStore must not be null.");
        }
        // 保存文档切分器。
        this.chunker = chunker;
        // 保存 chunk 存储。
        this.chunkStore = chunkStore;
    }

    // 导入已经解析成统一模型的医学知识文档。
    public MedicalKnowledgeIngestionResult ingestDocuments(String sourceName, List<MedicalKnowledgeDocument> documents) {
        // 如果来源名称为空，导入结果无法追溯。
        if (sourceName == null || sourceName.trim().isEmpty()) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("sourceName must not be blank.");
        }
        // 如果文档列表为空，说明调用方传参错误。
        if (documents == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("documents must not be null.");
        }
        // 调用切分器把完整文档转换成可检索 chunk。
        List<MedicalKnowledgeChunk> chunks = chunker.splitAll(documents);
        // 把 chunk 保存到存储边界中。
        chunkStore.saveAll(chunks);
        // 返回本次导入统计结果。
        return MedicalKnowledgeIngestionResult.of(sourceName.trim(), documents.size(), chunks.size());
    }
}
