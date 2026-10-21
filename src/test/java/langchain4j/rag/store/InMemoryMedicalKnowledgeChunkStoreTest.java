// 声明测试类所在的包。
package langchain4j.rag.store;

// MedicalKnowledgeChunk 是被存储的知识片段。
import langchain4j.rag.MedicalKnowledgeChunk;
// MedicalKnowledgeDocument 是构造测试片段时使用的原始文档。
import langchain4j.rag.MedicalKnowledgeDocument;
// MedicalKnowledgeDocumentType 是文档类型枚举。
import langchain4j.rag.MedicalKnowledgeDocumentType;
// List 表示一组知识片段。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// AssertJ 异常断言工具。
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 定义内存医学知识 chunk 存储的单元测试。
class InMemoryMedicalKnowledgeChunkStoreTest {

    // 测试保存、查询和按 ID 查找。
    @Test
    // 方法名描述测试意图：仓储应该保存 chunk，并支持快照查询。
    void shouldSaveAndFindChunks() {
        // 创建内存仓储。
        InMemoryMedicalKnowledgeChunkStore store = new InMemoryMedicalKnowledgeChunkStore();
        // 创建第一个 chunk。
        MedicalKnowledgeChunk firstChunk = chunk("medlineplus:6308", 0, "A1C is a blood test.");
        // 创建第二个 chunk。
        MedicalKnowledgeChunk secondChunk = chunk("medlineplus:6308", 1, "A1C can monitor diabetes treatment.");

        // 保存两个 chunk。
        store.saveAll(List.of(firstChunk, secondChunk));

        // 验证数量为 2。
        assertThat(store.count()).isEqualTo(2);
        // 验证查询所有可以拿到两个 chunk。
        assertThat(store.findAll()).hasSize(2);
        // 验证可以按 chunkId 找到第一条。
        assertThat(store.findByChunkId("medlineplus:6308#chunk-0")).contains(firstChunk);
        // 验证不存在的 chunkId 返回空结果。
        assertThat(store.findByChunkId("missing")).isEmpty();
    }

    // 测试重复保存同一个 chunkId 会覆盖旧值。
    @Test
    // 方法名描述测试意图：同一个 chunkId 重复导入时应该保持幂等。
    void shouldOverwriteDuplicateChunkId() {
        // 创建内存仓储。
        InMemoryMedicalKnowledgeChunkStore store = new InMemoryMedicalKnowledgeChunkStore();
        // 创建旧 chunk。
        MedicalKnowledgeChunk oldChunk = chunk("medlineplus:6308", 0, "old text");
        // 创建同 ID 的新 chunk。
        MedicalKnowledgeChunk newChunk = chunk("medlineplus:6308", 0, "new text");

        // 保存旧 chunk。
        store.saveAll(List.of(oldChunk));
        // 保存新 chunk。
        store.saveAll(List.of(newChunk));

        // 验证数量仍然为 1。
        assertThat(store.count()).isEqualTo(1);
        // 验证同 ID 数据已经更新成新文本。
        assertThat(store.findByChunkId("medlineplus:6308#chunk-0").orElseThrow().getText()).isEqualTo("new text");
    }

    // 测试空 chunkId 会被拒绝。
    @Test
    // 方法名描述测试意图：没有 chunkId 的片段不能进入存储。
    void shouldRejectBlankChunkId() {
        // 创建内存仓储。
        InMemoryMedicalKnowledgeChunkStore store = new InMemoryMedicalKnowledgeChunkStore();
        // 创建空 ID 的 chunk。
        MedicalKnowledgeChunk chunk = new MedicalKnowledgeChunk();

        // 验证保存时抛出非法参数异常。
        assertThatThrownBy(() -> store.saveAll(List.of(chunk)))
                // 验证异常类型。
                .isInstanceOf(IllegalArgumentException.class)
                // 验证异常信息。
                .hasMessageContaining("chunkId");
    }

    // 创建测试用知识片段。
    private static MedicalKnowledgeChunk chunk(String documentId, int chunkIndex, String text) {
        // 创建原始文档。
        MedicalKnowledgeDocument document = new MedicalKnowledgeDocument();
        // 设置文档 ID。
        document.setDocumentId(documentId);
        // 设置标题。
        document.setTitle("A1C");
        // 设置正文。
        document.setText(text);
        // 设置语言。
        document.setLanguage("English");
        // 设置来源名称。
        document.setSourceName("MedlinePlus");
        // 设置来源 URL。
        document.setSourceUrl("https://medlineplus.gov/a1c.html");
        // 设置文档类型。
        document.setDocumentType(MedicalKnowledgeDocumentType.HEALTH_TOPIC);
        // 根据文档创建 chunk。
        return MedicalKnowledgeChunk.from(document, chunkIndex, text);
    }
}
