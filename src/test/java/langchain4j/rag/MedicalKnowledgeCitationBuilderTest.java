// 声明测试类所在的包。
package langchain4j.rag;

// List 表示一组检索命中或引用。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;

// 定义医学知识引用构建器的单元测试。
class MedicalKnowledgeCitationBuilderTest {

    // 测试相同 chunkId 的命中只生成一条引用。
    @Test
    // 方法名描述测试意图：引用应该按具体 chunk 去重。
    void shouldDeduplicateCitationsByChunkId() {
        // 创建引用构建器。
        MedicalKnowledgeCitationBuilder builder = new MedicalKnowledgeCitationBuilder();
        // 创建第一条命中。
        MedicalKnowledgeSearchHit firstHit = hit("medlineplus:6308#chunk-0", "A1C");
        // 创建同 chunkId 的第二条命中。
        MedicalKnowledgeSearchHit duplicatedHit = hit("medlineplus:6308#chunk-0", "A1C");
        // 创建另一个 chunk 的命中。
        MedicalKnowledgeSearchHit secondHit = hit("medlineplus:9#chunk-0", "Asthma");

        // 构建引用列表。
        List<MedicalKnowledgeCitation> citations = builder.build(List.of(firstHit, duplicatedHit, secondHit));

        // 验证只生成两条引用。
        assertThat(citations).hasSize(2);
        // 验证第一条引用编号为 1。
        assertThat(citations.get(0).getCitationNumber()).isEqualTo(1);
        // 验证第二条引用编号为 2。
        assertThat(citations.get(1).getCitationNumber()).isEqualTo(2);
        // 验证第一条引用来自 A1C。
        assertThat(citations.get(0).getTitle()).isEqualTo("A1C");
        // 验证第二条引用来自 Asthma。
        assertThat(citations.get(1).getTitle()).isEqualTo("Asthma");
    }

    // 创建测试用检索命中。
    private static MedicalKnowledgeSearchHit hit(String chunkId, String title) {
        // 创建检索命中。
        MedicalKnowledgeSearchHit hit = new MedicalKnowledgeSearchHit();
        // 设置 chunkId。
        hit.setChunkId(chunkId);
        // 设置文档 ID。
        hit.setDocumentId(chunkId.split("#")[0]);
        // 设置标题。
        hit.setTitle(title);
        // 设置来源名称。
        hit.setSourceName("MedlinePlus");
        // 设置来源 URL。
        hit.setSourceUrl("https://example.org/" + title.toLowerCase());
        // 返回检索命中。
        return hit;
    }
}
