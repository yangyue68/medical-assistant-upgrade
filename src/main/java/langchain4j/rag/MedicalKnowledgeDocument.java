// 声明当前类所在的包。
package langchain4j.rag;

// LocalDate 表示数据下载或入库日期。
import java.time.LocalDate;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义医学知识文档对象，表示进入 RAG 知识库的一篇可追溯文档。
public class MedicalKnowledgeDocument {

    // 文档唯一 ID，例如 medlineplus:6308。
    private String documentId;

    // 文档标题。
    private String title;

    // 文档正文或摘要文本。
    private String text;

    // 文档语言，例如 English、Chinese。
    private String language;

    // 数据来源名称，例如 MedlinePlus、CDC、国家卫健委。
    private String sourceName;

    // 原始文档 URL，用于回答时引用来源。
    private String sourceUrl;

    // 文档类型。
    private MedicalKnowledgeDocumentType documentType;

    // 关联科室；当前种子数据可能为空，后续可通过规则或模型补充。
    private String department;

    // 关联疾病或主题；当前可以先等于标题。
    private String diseaseOrTopic;

    // 原始来源的创建日期字符串；不同数据源格式可能不同，所以先保留字符串。
    private String sourceCreatedAt;

    // 原始数据集生成时间字符串。
    private String sourceGeneratedAt;

    // 本项目下载或解析该文档的日期。
    private LocalDate retrievedAt;
}
