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

// 定义 RAG prompt 构建器的单元测试。
class MedicalRagPromptBuilderTest {

    // 测试 prompt 会包含安全边界、上下文和用户问题。
    @Test
    // 方法名描述测试意图：prompt 必须包含模型生成回答所需的完整材料。
    void shouldBuildPromptWithSafetyContextAndQuestion() {
        // 创建上下文。
        MedicalRagContext context = context();
        // 创建 prompt 构建器。
        MedicalRagPromptBuilder promptBuilder = new MedicalRagPromptBuilder();

        // 构建 prompt。
        String prompt = promptBuilder.build(context);

        // 验证 prompt 包含角色要求。
        assertThat(prompt).contains("严谨的医疗健康助手");
        // 验证 prompt 包含安全边界。
        assertThat(prompt).contains("不能替代医生诊断");
        // 验证 prompt 包含 RAG 上下文。
        assertThat(prompt).contains("【RAG 检索上下文】");
        // 验证 prompt 包含用户问题。
        assertThat(prompt).contains("A1C 是什么");
        // 验证 prompt 要求使用引用编号。
        assertThat(prompt).contains("例如 [1]");
    }

    // 测试空上下文会被拒绝。
    @Test
    // 方法名描述测试意图：构建 prompt 前必须先完成 RAG context。
    void shouldRejectNullContext() {
        // 创建 prompt 构建器。
        MedicalRagPromptBuilder promptBuilder = new MedicalRagPromptBuilder();

        // 验证空上下文抛出非法参数异常。
        assertThatThrownBy(() -> promptBuilder.build(null))
                // 验证异常类型。
                .isInstanceOf(IllegalArgumentException.class)
                // 验证异常信息。
                .hasMessageContaining("context");
    }

    // 创建测试上下文。
    private static MedicalRagContext context() {
        // 创建检索结果。
        MedicalKnowledgeSearchResult searchResult = MedicalKnowledgeSearchResult.of(
                // 用户问题。
                "A1C 是什么？",
                // 检索模式。
                RetrievalMode.HYBRID,
                // 命中列表。
                List.of(hit())
        );
        // 使用上下文构建器创建上下文。
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
        // 设置片段内容。
        hit.setSnippet("A1C is a blood test.");
        // 设置来源名称。
        hit.setSourceName("MedlinePlus");
        // 设置来源 URL。
        hit.setSourceUrl("https://example.org/a1c");
        // 设置召回方式。
        hit.setRetrievalSource("KEYWORD+VECTOR");
        // 返回命中。
        return hit;
    }
}
