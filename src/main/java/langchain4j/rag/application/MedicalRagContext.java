// 声明当前类所在的包，application 表示它属于 RAG 应用编排层。
package langchain4j.rag.application;

// MedicalKnowledgeCitation 表示回答可展示的引用来源。
import langchain4j.rag.MedicalKnowledgeCitation;
// MedicalKnowledgeSearchHit 表示检索命中片段。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// RetrievalMode 表示实际使用的检索模式。
import langchain4j.rag.RetrievalMode;
// List 表示一组检索命中或引用。
import java.util.List;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义 RAG 上下文对象，表示一次检索后可交给模型使用的完整上下文材料。
public class MedicalRagContext {

    // 用户原始问题。
    private String query;

    // 实际使用的检索模式。
    private RetrievalMode retrievalMode;

    // 可拼接进模型 prompt 的医学知识上下文。
    private String contextText;

    // 医疗安全提示，提醒模型不能替代医生诊断。
    private String safetyInstruction;

    // 检索命中列表，用于调试、评估和前端展示。
    private List<MedicalKnowledgeSearchHit> hits;

    // 引用列表，用于回答时展示来源。
    private List<MedicalKnowledgeCitation> citations;

    // 创建 RAG 上下文对象。
    public static MedicalRagContext of(
            // 用户原始问题。
            String query,
            // 实际检索模式。
            RetrievalMode retrievalMode,
            // prompt 上下文文本。
            String contextText,
            // 医疗安全提示。
            String safetyInstruction,
            // 检索命中列表。
            List<MedicalKnowledgeSearchHit> hits,
            // 引用列表。
            List<MedicalKnowledgeCitation> citations
    ) {
        // 创建上下文对象。
        MedicalRagContext context = new MedicalRagContext();
        // 设置用户原始问题。
        context.setQuery(query);
        // 设置实际检索模式。
        context.setRetrievalMode(retrievalMode);
        // 设置上下文文本。
        context.setContextText(contextText);
        // 设置医疗安全提示。
        context.setSafetyInstruction(safetyInstruction);
        // 设置检索命中列表。
        context.setHits(hits);
        // 设置引用列表。
        context.setCitations(citations);
        // 返回上下文对象。
        return context;
    }
}
