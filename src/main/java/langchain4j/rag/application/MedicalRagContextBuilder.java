// 声明当前类所在的包，application 表示它属于 RAG 应用编排层。
package langchain4j.rag.application;

// MedicalKnowledgeCitation 表示回答引用来源。
import langchain4j.rag.MedicalKnowledgeCitation;
// MedicalKnowledgeSearchHit 表示检索命中。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// MedicalKnowledgeSearchResult 表示检索结果。
import langchain4j.rag.MedicalKnowledgeSearchResult;
// ArrayList 用来构建可变引用列表。
import java.util.ArrayList;
// List 表示一组命中或引用。
import java.util.List;

// 定义 RAG 上下文构建器，负责把检索结果转换成模型可读上下文。
public class MedicalRagContextBuilder {

    // 默认上下文最大字符数，避免一次塞入过多检索内容。
    private static final int DEFAULT_MAX_CONTEXT_CHARACTERS = 3000;

    // 默认医疗安全提示。
    private static final String DEFAULT_SAFETY_INSTRUCTION =
            "你是医疗健康助手，只能提供科普和就医建议，不能替代医生诊断、开药或给出确定治疗方案。"
                    + " 如果用户描述胸痛、呼吸困难、意识障碍、大出血、严重过敏、疑似卒中等急症，应建议立即拨打急救电话或前往急诊。";

    // 上下文最大字符数。
    private final int maxContextCharacters;

    // 医疗安全提示。
    private final String safetyInstruction;

    // 使用默认参数创建上下文构建器。
    public MedicalRagContextBuilder() {
        // 委托给完整构造器。
        this(DEFAULT_MAX_CONTEXT_CHARACTERS, DEFAULT_SAFETY_INSTRUCTION);
    }

    // 使用自定义参数创建上下文构建器。
    public MedicalRagContextBuilder(int maxContextCharacters, String safetyInstruction) {
        // 如果上下文长度太小，就无法提供有效证据。
        if (maxContextCharacters < 500) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("maxContextCharacters must be at least 500.");
        }
        // 保存上下文最大字符数。
        this.maxContextCharacters = maxContextCharacters;
        // 保存安全提示；如果调用方传空，就使用默认提示。
        this.safetyInstruction = isBlank(safetyInstruction) ? DEFAULT_SAFETY_INSTRUCTION : safetyInstruction.trim();
    }

    // 根据检索结果构建 RAG 上下文。
    public MedicalRagContext build(MedicalKnowledgeSearchResult searchResult) {
        // 如果检索结果为空，说明调用方没有执行检索。
        if (searchResult == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("searchResult must not be null.");
        }
        // 读取命中列表。
        List<MedicalKnowledgeSearchHit> hits = safeHits(searchResult.getHits());
        // 读取引用列表。
        List<MedicalKnowledgeCitation> citations = safeCitations(searchResult.getCitations());
        // 构建上下文文本。
        String contextText = buildContextText(hits, citations);
        // 返回 RAG 上下文对象。
        return MedicalRagContext.of(
                // 设置用户问题。
                searchResult.getQuery(),
                // 设置实际检索模式。
                searchResult.getRetrievalMode(),
                // 设置上下文文本。
                contextText,
                // 设置安全提示。
                safetyInstruction,
                // 设置命中列表。
                hits,
                // 设置引用列表。
                citations
        );
    }

    // 构建 prompt 上下文文本。
    private String buildContextText(List<MedicalKnowledgeSearchHit> hits, List<MedicalKnowledgeCitation> citations) {
        // 如果没有命中，就返回明确的空上下文提示。
        if (hits.isEmpty()) {
            // 返回空上下文提示。
            return "未检索到可引用的医学知识片段。回答时请说明资料不足，并建议用户补充信息或咨询专业医生。";
        }
        // 创建文本构建器。
        StringBuilder builder = new StringBuilder();
        // 添加安全提示标题。
        builder.append("【医疗安全边界】\n");
        // 添加安全提示内容。
        builder.append(safetyInstruction).append("\n\n");
        // 添加知识片段标题。
        builder.append("【可引用医学知识片段】\n");
        // 遍历命中列表。
        for (int i = 0; i < hits.size(); i++) {
            // 读取当前命中。
            MedicalKnowledgeSearchHit hit = hits.get(i);
            // 计算引用编号。
            int citationNumber = citationNumberFor(hit, citations);
            // 构建当前片段文本。
            String block = buildHitBlock(i + 1, citationNumber, hit);
            // 如果加入该片段会超过最大上下文长度，就停止追加。
            if (builder.length() + block.length() > maxContextCharacters) {
                // 添加截断提示。
                builder.append("\n【上下文截断】部分检索片段因上下文预算限制未加入。\n");
                // 结束循环。
                break;
            }
            // 追加当前片段。
            builder.append(block);
        }
        // 返回最终上下文文本。
        return builder.toString().trim();
    }

    // 构建单条命中的上下文块。
    private String buildHitBlock(int hitNumber, int citationNumber, MedicalKnowledgeSearchHit hit) {
        // 创建片段文本构建器。
        StringBuilder builder = new StringBuilder();
        // 添加片段编号和引用编号。
        //实际上此处的hitNumber和citationNumber必然相等，从前端的“RAG医学问答”的“生成回答”功能可以验证这一点
        builder.append("[片段 ").append(hitNumber).append(" / 引用 ").append(citationNumber).append("]\n");
        // 添加标题。
        builder.append("标题：").append(nullToEmpty(hit.getTitle())).append("\n");
        // 添加来源。
        builder.append("来源：").append(nullToEmpty(hit.getSourceName())).append("\n");
        // 添加 URL。
        builder.append("URL：").append(nullToEmpty(hit.getSourceUrl())).append("\n");
        // 添加召回来源。
        builder.append("召回方式：").append(nullToEmpty(hit.getRetrievalSource())).append("\n");
        // 添加正文片段。
        builder.append("内容：").append(nullToEmpty(hit.getSnippet())).append("\n\n");
        // 返回片段文本。
        return builder.toString();
    }

    // 查找命中对应的引用编号。
    private int citationNumberFor(MedicalKnowledgeSearchHit hit, List<MedicalKnowledgeCitation> citations) {
        // 遍历引用列表。
        for (MedicalKnowledgeCitation citation : citations) {
            // 如果引用 chunkId 和命中 chunkId 一致，就返回引用编号。
            if (citation.getChunkId() != null && citation.getChunkId().equals(hit.getChunkId())) {
                // 返回引用编号。
                return citation.getCitationNumber();
            }
        }
        // 没找到时返回 0，表示没有可用引用编号，因为引用编号是从1开始递增的。
        return 0;
    }

    // 安全读取命中列表。
    private List<MedicalKnowledgeSearchHit> safeHits(List<MedicalKnowledgeSearchHit> hits) {
        // 如果命中列表为空，返回空列表。
        if (hits == null || hits.isEmpty()) {
            // 返回不可变空列表。
            return List.of();
        }
        // 返回新列表，避免外部修改影响上下文对象。
        return new ArrayList<>(hits);
    }

    // 安全读取引用列表。
    private List<MedicalKnowledgeCitation> safeCitations(List<MedicalKnowledgeCitation> citations) {
        // 如果引用列表为空，返回空列表。
        if (citations == null || citations.isEmpty()) {
            // 返回不可变空列表。
            return List.of();
        }
        // 返回新列表，避免外部修改影响上下文对象。
        return new ArrayList<>(citations);
    }

    // 判断文本是否为空白。
    private boolean isBlank(String text) {
        // 返回 null 或 trim 后为空的判断结果。
        return text == null || text.trim().isEmpty();
    }

    // 把 null 转换为空字符串。
    private String nullToEmpty(String text) {
        // 如果文本为空，返回空字符串。
        if (text == null) {
            // 返回空文本。
            return "";
        }
        // 返回原文本。
        return text;
    }
}
