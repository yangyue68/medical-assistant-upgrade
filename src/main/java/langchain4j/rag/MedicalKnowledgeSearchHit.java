// 声明当前类所在的包。
package langchain4j.rag;

// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义一次医学知识检索命中的结果项。
public class MedicalKnowledgeSearchHit {

    // 命中片段 ID。
    private String chunkId;

    // 命中文档 ID。
    private String documentId;

    // 命中片段在原始文档中的序号。
    private Integer chunkIndex;

    // 命中文档标题。
    private String title;

    // 命中文档片段。
    private String snippet;

    // 数据来源名称。
    private String sourceName;

    // 数据来源 URL。
    private String sourceUrl;

    // 文档类型。
    private MedicalKnowledgeDocumentType documentType;

    // 命中分数，后续混合检索会把 BM25 分数、向量分数、重排分数融合到这里。
    private double score;

    // 关键词检索原始分数；只有混合检索或关键词检索需要解释分数时使用。
    private Double keywordScore;

    // 向量检索原始分数；只有混合检索或向量检索需要解释分数时使用。
    private Double vectorScore;

    // 重排模型的相关性分数；仅在启用在线 rerank 时存在。
    private Double rerankScore;

    // 命中来源说明，例如 KEYWORD、VECTOR、KEYWORD+VECTOR。
    private String retrievalSource;

    // 根据知识文档创建命中项。
    public static MedicalKnowledgeSearchHit from(MedicalKnowledgeDocument document, String snippet, double score) {
        // 创建命中项。
        MedicalKnowledgeSearchHit hit = new MedicalKnowledgeSearchHit();
        // 设置文档 ID。
        hit.setDocumentId(document.getDocumentId());
        // 设置标题。
        hit.setTitle(document.getTitle());
        // 设置片段。
        hit.setSnippet(snippet);
        // 设置来源名称。
        hit.setSourceName(document.getSourceName());
        // 设置来源 URL。
        hit.setSourceUrl(document.getSourceUrl());
        // 设置文档类型。
        hit.setDocumentType(document.getDocumentType());
        // 设置检索分数。
        hit.setScore(score);
        // 设置命中来源。
        //虽然在合理写了DOCUMENT，但是实际被调用时，会写成KEYWORD、VECTOR、KEYWORD+VECTOR。
        hit.setRetrievalSource("DOCUMENT");
        // 返回命中项。
        return hit;
    }

    // 根据知识片段创建命中项。
    public static MedicalKnowledgeSearchHit from(MedicalKnowledgeChunk chunk, String snippet, double score) {
        // 创建命中项。
        MedicalKnowledgeSearchHit hit = new MedicalKnowledgeSearchHit();
        // 设置片段 ID。
        hit.setChunkId(chunk.getChunkId());
        // 设置文档 ID。
        hit.setDocumentId(chunk.getDocumentId());
        // 设置片段序号。
        hit.setChunkIndex(chunk.getChunkIndex());
        // 设置标题。
        hit.setTitle(chunk.getTitle());
        // 设置片段。
        hit.setSnippet(snippet);
        // 设置来源名称。
        hit.setSourceName(chunk.getSourceName());
        // 设置来源 URL。
        hit.setSourceUrl(chunk.getSourceUrl());
        // 设置文档类型。
        hit.setDocumentType(chunk.getDocumentType());
        // 设置检索分数。
        hit.setScore(score);
        // 设置命中来源。
        //虽然在合理写了CHUNK，但是实际被调用时，会写成KEYWORD、VECTOR、KEYWORD+VECTOR。
        hit.setRetrievalSource("CHUNK");
        // 返回命中项。
        return hit;
    }
}
