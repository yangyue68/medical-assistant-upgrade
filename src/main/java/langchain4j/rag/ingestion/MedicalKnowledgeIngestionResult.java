// 声明当前类所在的包，ingestion 表示它属于知识导入流程。
package langchain4j.rag.ingestion;

// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义医学知识导入结果，用于记录一次 ingestion 处理了多少文档和片段。
public class MedicalKnowledgeIngestionResult {

    // 数据来源名称，例如 MedlinePlus。
    private String sourceName;

    // 本次解析出的完整文档数量。
    private int documentCount;

    // 本次切分并保存的 chunk 数量。
    private int chunkCount;

    // 创建导入结果对象。
    public static MedicalKnowledgeIngestionResult of(
            // 数据来源名称。
            String sourceName,
            // 文档数量。
            int documentCount,
            // chunk 数量。
            int chunkCount
    ) {
        // 创建结果对象。
        MedicalKnowledgeIngestionResult result = new MedicalKnowledgeIngestionResult();
        // 设置数据来源名称。
        result.setSourceName(sourceName);
        // 设置文档数量。
        result.setDocumentCount(documentCount);
        // 设置 chunk 数量。
        result.setChunkCount(chunkCount);
        // 返回结果对象。
        return result;
    }
}
