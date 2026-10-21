// 声明测试类所在的包。
package langchain4j.rag.application;

// MedicalKnowledgeSearchHit 表示检索命中。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// MedicalKnowledgeSearchResult 表示检索结果。
import langchain4j.rag.MedicalKnowledgeSearchResult;
// RetrievalMode 表示检索模式。
import langchain4j.rag.RetrievalMode;
// List 表示一组检索命中。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// AssertJ 异常断言工具。
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 定义 RAG 上下文构建器的单元测试。
class MedicalRagContextBuilderTest {

    // 测试检索结果会被转换成可给模型使用的上下文。
    @Test
    // 方法名描述测试意图：上下文应该包含安全提示、片段和引用信息。
    void shouldBuildPromptContextWithSafetyAndCitations() {
        // 创建上下文构建器。
        MedicalRagContextBuilder builder = new MedicalRagContextBuilder(1200, "安全提示：不能替代医生诊断。");
        // 创建检索结果。
        MedicalKnowledgeSearchResult searchResult = MedicalKnowledgeSearchResult.of(
                // 设置查询问题。
                "A1C 是什么？",
                // 设置检索模式。
                RetrievalMode.HYBRID,
                // 设置命中列表。
                List.of(hit("medlineplus:6308#chunk-0", "A1C", "A1C is a blood test."))
        );

        // 构建 RAG 上下文。
        MedicalRagContext context = builder.build(searchResult);

        // 验证原始问题被保留。
        assertThat(context.getQuery()).isEqualTo("A1C 是什么？");
        // 验证检索模式被保留。
        assertThat(context.getRetrievalMode()).isEqualTo(RetrievalMode.HYBRID);
        // 验证安全提示被保留。
        assertThat(context.getSafetyInstruction()).contains("不能替代医生诊断");
        // 验证上下文包含安全边界。
        assertThat(context.getContextText()).contains("【医疗安全边界】");
        // 验证上下文包含引用编号。
        assertThat(context.getContextText()).contains("[片段 1 / 引用 1]");
        // 验证上下文包含来源 URL。
        assertThat(context.getContextText()).contains("https://example.org/a1c");
        // 验证引用列表存在。
        assertThat(context.getCitations()).hasSize(1);
        // 验证引用标题正确。
        assertThat(context.getCitations().get(0).getTitle()).isEqualTo("A1C");
    }

    // 测试没有检索命中时会返回明确提示。
    @Test
    // 方法名描述测试意图：空命中不能让模型误以为有证据。
    void shouldBuildEmptyEvidenceContextWhenNoHits() {
        // 创建上下文构建器。
        MedicalRagContextBuilder builder = new MedicalRagContextBuilder();
        // 创建空命中检索结果。
        MedicalKnowledgeSearchResult searchResult = MedicalKnowledgeSearchResult.of("未知问题", RetrievalMode.HYBRID, List.of());

        // 构建上下文。
        MedicalRagContext context = builder.build(searchResult);

        // 验证上下文提示没有可引用片段。
        assertThat(context.getContextText()).contains("未检索到可引用的医学知识片段");
        // 验证引用列表为空。
        assertThat(context.getCitations()).isEmpty();
    }

    // 测试非法上下文长度会被拒绝。
    @Test
    // 方法名描述测试意图：上下文预算太小没有实际 RAG 价值。
    void shouldRejectTooSmallContextBudget() {
        // 验证创建构建器时抛出非法参数异常。
        assertThatThrownBy(() -> new MedicalRagContextBuilder(100, "安全提示"))
                // 验证异常类型。
                .isInstanceOf(IllegalArgumentException.class)
                // 验证异常信息。
                .hasMessageContaining("maxContextCharacters");
    }

    // 创建测试命中。
    private static MedicalKnowledgeSearchHit hit(String chunkId, String title, String snippet) {
        // 创建命中对象。
        MedicalKnowledgeSearchHit hit = new MedicalKnowledgeSearchHit();
        // 设置 chunkId。
        hit.setChunkId(chunkId);
        // 设置文档 ID。
        hit.setDocumentId(chunkId.split("#")[0]);
        // 设置片段序号。
        hit.setChunkIndex(0);
        // 设置标题。
        hit.setTitle(title);
        // 设置片段内容。
        hit.setSnippet(snippet);
        // 设置来源名称。
        hit.setSourceName("MedlinePlus");
        // 设置来源 URL。
        hit.setSourceUrl("https://example.org/a1c");
        // 设置召回方式。
        hit.setRetrievalSource("KEYWORD+VECTOR");
        // 设置分数。
        hit.setScore(0.03);
        // 返回命中对象。
        return hit;
    }
}
