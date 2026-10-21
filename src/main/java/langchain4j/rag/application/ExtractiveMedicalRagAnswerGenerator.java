// 声明当前类所在的包，application 表示它属于 RAG 应用编排层。
package langchain4j.rag.application;

// MedicalKnowledgeCitation 表示回答引用来源。
import langchain4j.rag.MedicalKnowledgeCitation;
// MedicalKnowledgeSearchHit 表示检索命中。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// List 表示一组命中或引用。
import java.util.List;

// 定义本地抽取式 RAG 回答生成器。
public class ExtractiveMedicalRagAnswerGenerator implements MedicalRagAnswerGenerator {

    // 根据 prompt 和 RAG 上下文生成回答。
    @Override
    public String generate(String prompt, MedicalRagContext context) {
        // 如果上下文为空，说明调用方错误。
        if (context == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("context must not be null.");
        }
        // 如果没有命中，就返回资料不足回答。
        if (context.getHits() == null || context.getHits().isEmpty()) {
            // 返回资料不足提示。
            return "目前没有检索到足够可靠的医学知识片段，不能基于资料给出明确回答。"
                    + " 请补充症状、持续时间、年龄、基础疾病等信息，必要时咨询专业医生。";
        }
        // 创建回答构建器。
        StringBuilder builder = new StringBuilder();
        // 添加安全边界提醒。
        builder.append("以下内容仅供健康科普和就医参考，不能替代医生诊断。\n\n");
        // 添加简短回答标题。
        builder.append("根据已检索到的资料：\n");
        // 读取最多两条命中，避免抽取式回答过长。
        int limit = Math.min(context.getHits().size(), 2);
        // 遍历前几条命中。
        for (int i = 0; i < limit; i++) {
            // 读取当前命中。
            MedicalKnowledgeSearchHit hit = context.getHits().get(i);
            // 查找引用编号。
            int citationNumber = citationNumberFor(hit, context.getCitations());
            // 添加一条回答要点。
            builder.append(i + 1)
                    // 添加序号后的空格。
                    .append(". ")
                    // 添加摘要片段。
                    .append(compactSnippet(hit.getSnippet()))
                    // 添加引用编号。
                    .append(" [")
                    // 添加引用编号数字。
                    .append(citationNumber)
                    // 关闭引用编号。
                    .append("]\n");
        }
        // 添加就医建议。
        builder.append("\n如果症状明显、持续加重，或伴随胸痛、呼吸困难、意识异常等急症表现，应及时就医或前往急诊。");
        // 返回回答文本。
        return builder.toString();
    }

    // 查找命中对应的引用编号。
    private int citationNumberFor(MedicalKnowledgeSearchHit hit, List<MedicalKnowledgeCitation> citations) {
        // 如果引用列表为空，返回 0。
        if (citations == null || citations.isEmpty()) {
            // 返回 0 表示未知引用。
            return 0;
        }
        // 遍历引用列表。
        for (MedicalKnowledgeCitation citation : citations) {
            // 如果 citation 的 chunkId 和命中 chunkId 一致，就返回编号。
            if (citation.getChunkId() != null && citation.getChunkId().equals(hit.getChunkId())) {
                // 返回引用编号。
                return citation.getCitationNumber();
            }
        }
        // 没找到时返回 0。
        return 0;
    }

    // 压缩片段文本，避免回答过长。
    private String compactSnippet(String snippet) {
        // 如果片段为空，返回资料片段为空。
        if (snippet == null || snippet.trim().isEmpty()) {
            // 返回默认文本。
            return "检索到的资料片段为空。";
        }
        // 压缩连续空白。
        String compacted = snippet.replaceAll("\\s+", " ").trim();
        // 如果长度不超过 180，直接返回。
        if (compacted.length() <= 180) {
            // 返回原片段。
            return compacted;
        }
        // 超过长度就截断并加省略号。
        return compacted.substring(0, 180) + "...";
    }
}
