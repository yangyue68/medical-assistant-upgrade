// 声明当前类所在的包。
package langchain4j.rag;

// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义医学知识引用对象，用于把检索命中转换成回答可展示的来源引用。
public class MedicalKnowledgeCitation {
    //这个类的作用是：在一次MedicalKnowledgeSearchResult里，一批MedicalKnowledgeCitation和一批MedicalKnowledgeSearchHit
    //是一一对应的，相当于下面的citationNumber和chunkId其实是一一对应的。
    //所以citationNumber不是全局唯一的，而是在每一次MedicalKnowledgeSearchResult里，一批MedicalKnowledgeCitation
    //的citationNumber都从1开始递增

    // 引用编号，例如 1、2、3。
    private int citationNumber;

    // 引用对应的 chunkId，方便追溯到具体知识片段。
    private String chunkId;

    // 引用对应的文档标题。
    private String title;

    // 引用来源名称，例如 MedlinePlus。
    private String sourceName;

    // 引用来源 URL。
    private String sourceUrl;

    // 根据检索命中创建引用。
    public static MedicalKnowledgeCitation from(
            // 引用编号。
            int citationNumber,
            // 检索命中。
            MedicalKnowledgeSearchHit hit
    ) {
        // 创建引用对象。
        MedicalKnowledgeCitation citation = new MedicalKnowledgeCitation();
        // 设置引用编号。
        citation.setCitationNumber(citationNumber);
        // 设置 chunkId。
        citation.setChunkId(hit.getChunkId());
        // 设置标题。
        citation.setTitle(hit.getTitle());
        // 设置来源名称。
        citation.setSourceName(hit.getSourceName());
        // 设置来源 URL。
        citation.setSourceUrl(hit.getSourceUrl());
        // 返回引用对象。
        return citation;
    }
}
