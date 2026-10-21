// 声明当前类所在的包。
package langchain4j.rag;

// LocalDate 表示数据下载或解析日期。
import java.time.LocalDate;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义医学知识片段对象，表示 RAG 检索时真正被召回的一小段文本。
public class MedicalKnowledgeChunk {

    // 片段唯一 ID，可以由document ID+"#chunk-"+chunkIndex组成，例如 medlineplus:6308#chunk-0。
    private String chunkId;

    // 原始文档 ID，用于把片段追溯回完整文档。
    private String documentId;

    // 片段在原始文档中的序号，从 0 开始。
    private int chunkIndex;

    // 原始文档标题，用于检索加权和回答引用。
    private String title;

    // 片段正文，是后续关键词检索、向量化和拼接 prompt 的核心内容。
    private String text;

    // 文档语言，例如 English、Chinese。
    private String language;

    // 数据来源名称，例如 MedlinePlus、CDC、国家卫健委。
    private String sourceName;

    // 原始来源 URL，用于回答时展示引用。
    private String sourceUrl;

    // 文档类型，用于按健康主题、指南、检查说明等维度过滤。
    private MedicalKnowledgeDocumentType documentType;

    // 关联科室，后续可用于分诊场景的元数据过滤。
    private String department;

    // 关联疾病或主题，后续可用于医学实体过滤和聚合展示。
    private String diseaseOrTopic;

    // 原始来源创建日期字符串。
    private String sourceCreatedAt;

    // 原始数据集生成时间字符串。
    private String sourceGeneratedAt;

    // 本项目下载或解析该片段的日期。
    private LocalDate retrievedAt;

    // 根据原始文档和切分结果创建片段。
    public static MedicalKnowledgeChunk from(
            // 原始医学知识文档。
            MedicalKnowledgeDocument document,
            // 片段序号。
            int chunkIndex,
            // 片段正文。
            String text
    ) {
        // 创建片段对象。
        MedicalKnowledgeChunk chunk = new MedicalKnowledgeChunk();
        // 设置片段 ID。
        chunk.setChunkId(document.getDocumentId() + "#chunk-" + chunkIndex);
        // 设置原始文档 ID。
        chunk.setDocumentId(document.getDocumentId());
        // 设置片段序号。
        chunk.setChunkIndex(chunkIndex);
        // 设置标题。
        chunk.setTitle(document.getTitle());
        // 设置片段正文。
        chunk.setText(text);
        // 设置语言。
        chunk.setLanguage(document.getLanguage());
        // 设置来源名称。
        chunk.setSourceName(document.getSourceName());
        // 设置来源 URL。
        chunk.setSourceUrl(document.getSourceUrl());
        // 设置文档类型。
        chunk.setDocumentType(document.getDocumentType());
        // 设置关联科室。
        chunk.setDepartment(document.getDepartment());
        // 设置关联疾病或主题。
        chunk.setDiseaseOrTopic(document.getDiseaseOrTopic());
        // 设置来源创建日期。
        chunk.setSourceCreatedAt(document.getSourceCreatedAt());
        // 设置来源数据集生成时间。
        chunk.setSourceGeneratedAt(document.getSourceGeneratedAt());
        // 设置本项目获取日期。
        chunk.setRetrievedAt(document.getRetrievedAt());
        // 返回片段对象。
        return chunk;
    }
}
