// 声明测试类所在的包。
package langchain4j.rag.application;

// MedicalKnowledgeSearchHit 表示检索命中。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// MedicalKnowledgeSearchResult 表示检索结果。
import langchain4j.rag.MedicalKnowledgeSearchResult;
// RetrievalMode 表示检索模式。
import langchain4j.rag.RetrievalMode;
// List 表示一组命中。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// AssertJ 异常断言工具。
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 定义抽取式 RAG 回答生成器的单元测试。
class ExtractiveMedicalRagAnswerGeneratorTest {

    // 测试有证据时生成带引用回答。
    @Test
    // 方法名描述测试意图：抽取式回答应该包含安全提示和引用编号。
    void shouldGenerateExtractiveAnswerWithCitation() {
        // 创建 RAG 上下文。
        MedicalRagContext context = contextWithHit();
        // 创建回答生成器。
        ExtractiveMedicalRagAnswerGenerator generator = new ExtractiveMedicalRagAnswerGenerator();

        // 生成回答。
        String answer = generator.generate("prompt", context);

        // 验证回答包含安全提示。
        assertThat(answer).contains("不能替代医生诊断");
        // 验证回答包含片段内容。
        assertThat(answer).contains("A1C is a blood test");
        // 验证回答包含引用编号。
        assertThat(answer).contains("[1]");
    }

    // 测试无证据时生成资料不足回答。
    @Test
    // 方法名描述测试意图：没有命中时不能编造医学答案。
    void shouldGenerateInsufficientEvidenceAnswerWhenNoHits() {
        // 创建空命中上下文。
        MedicalRagContext context = MedicalRagContext.of("未知问题", RetrievalMode.HYBRID, "无资料", "安全提示", List.of(), List.of());
        // 创建回答生成器。
        ExtractiveMedicalRagAnswerGenerator generator = new ExtractiveMedicalRagAnswerGenerator();

        // 生成回答。
        String answer = generator.generate("prompt", context);

        // 验证回答提示资料不足。
        assertThat(answer).contains("没有检索到足够可靠的医学知识片段");
    }

    // 测试空上下文会被拒绝。
    @Test
    // 方法名描述测试意图：回答生成前必须有 RAG context。
    void shouldRejectNullContext() {
        // 创建回答生成器。
        ExtractiveMedicalRagAnswerGenerator generator = new ExtractiveMedicalRagAnswerGenerator();

        // 验证空上下文抛出异常。
        assertThatThrownBy(() -> generator.generate("prompt", null))
                // 验证异常类型。
                .isInstanceOf(IllegalArgumentException.class)
                // 验证异常信息。
                .hasMessageContaining("context");
    }

    // 创建包含命中的上下文。
    private static MedicalRagContext contextWithHit() {
        // 创建检索结果。
        MedicalKnowledgeSearchResult searchResult = MedicalKnowledgeSearchResult.of("A1C 是什么？", RetrievalMode.HYBRID, List.of(hit()));
        // 构建上下文。
        return new MedicalRagContextBuilder().build(searchResult);
    }

    // 创建测试命中。
    private static MedicalKnowledgeSearchHit hit() {
        // 创建命中对象。
        MedicalKnowledgeSearchHit hit = new MedicalKnowledgeSearchHit();
        // 设置 chunkId。
        hit.setChunkId("medlineplus:6308#chunk-0");
        // 设置文档 ID。
        hit.setDocumentId("medlineplus:6308");
        // 设置片段序号。
        hit.setChunkIndex(0);
        // 设置标题。
        hit.setTitle("A1C");
        // 设置片段。
        hit.setSnippet("A1C is a blood test for diabetes.");
        // 设置来源名称。
        hit.setSourceName("MedlinePlus");
        // 设置来源 URL。
        hit.setSourceUrl("https://example.org/a1c");
        // 设置召回方式。
        hit.setRetrievalSource("KEYWORD");
        // 返回命中。
        return hit;
    }
}
