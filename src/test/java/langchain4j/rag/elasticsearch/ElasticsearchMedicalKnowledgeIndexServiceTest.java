// 声明测试类所在的包。
package langchain4j.rag.elasticsearch;

// MedicalKnowledgeChunk 是索引服务写入 Elasticsearch 的知识片段。
import langchain4j.rag.MedicalKnowledgeChunk;
// MedicalKnowledgeDocument 是构造测试 chunk 的完整文档。
import langchain4j.rag.MedicalKnowledgeDocument;
// MedicalKnowledgeDocumentType 是测试文档类型枚举。
import langchain4j.rag.MedicalKnowledgeDocumentType;
// DeterministicMedicalKnowledgeEmbeddingModel 提供无需联网的测试 embedding。
import langchain4j.rag.embedding.DeterministicMedicalKnowledgeEmbeddingModel;
// MedicalKnowledgeSearchHit 是 fake 客户端 search 方法的返回类型。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// MedicalKnowledgeSearchRequest 是 fake 客户端 search 方法的入参类型。
import langchain4j.rag.MedicalKnowledgeSearchRequest;
// RetrievalMode 是 fake 客户端 search 方法的入参类型。
import langchain4j.rag.RetrievalMode;
// Path 表示 JUnit 临时 marker 目录。
import java.nio.file.Path;
// LocalDate 表示测试文档获取日期。
import java.time.LocalDate;
// List 表示一组 chunk 或搜索命中。
import java.util.List;
// ArrayList 用于记录 fake 客户端每次 bulk 写入的批量大小。
import java.util.ArrayList;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;
// TempDir 提供 JUnit 管理的临时目录。
import org.junit.jupiter.api.io.TempDir;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;

// 定义 Elasticsearch 索引服务的单元测试。
class ElasticsearchMedicalKnowledgeIndexServiceTest {

    // 测试用临时目录，避免 marker 写入仓库目录。
    @TempDir
    // 保存测试 marker 目录。
    Path tempDir;

    // 测试 marker 和远程索引都存在时不会重复导入。
    @Test
    // 方法名描述测试意图：第二次启动不应该重复 bulk 写入。
    void shouldSkipImportWhenMarkerAndRemoteIndexExist() {
        // 创建 fake Elasticsearch 客户端。
        FakeElasticsearchMedicalKnowledgeClient client = new FakeElasticsearchMedicalKnowledgeClient();
        // 创建索引服务。
        ElasticsearchMedicalKnowledgeIndexService service = service(client, false);

        // 第一次执行导入。
        service.indexOnFirstImportOrRebuild(chunks());
        // 第二次执行导入。
        service.indexOnFirstImportOrRebuild(chunks());

        // 验证只创建或确认了一次索引。
        assertThat(client.ensureIndexCalls).isEqualTo(1);
        // 验证只 bulk 写入了一次。
        assertThat(client.bulkIndexCalls).isEqualTo(1);
        // 验证远程索引状态已经存在。
        assertThat(client.remoteIndexExists).isTrue();
    }

    // 测试 marker 存在但远程索引不存在时会重新导入。
    @Test
    // 方法名描述测试意图：云上索引被删后不能被本地 marker 误判为已完成。
    void shouldImportAgainWhenMarkerExistsButRemoteIndexIsMissing() {
        // 创建 fake Elasticsearch 客户端。
        FakeElasticsearchMedicalKnowledgeClient client = new FakeElasticsearchMedicalKnowledgeClient();
        // 创建索引服务。
        ElasticsearchMedicalKnowledgeIndexService service = service(client, false);

        // 第一次执行导入并写入 marker。
        service.indexOnFirstImportOrRebuild(chunks());
        // 模拟云上索引被手动删除。
        client.remoteIndexExists = false;
        // 第二次执行导入。
        service.indexOnFirstImportOrRebuild(chunks());

        // 验证两次都执行了索引确认。
        assertThat(client.ensureIndexCalls).isEqualTo(2);
        // 验证第二次没有被 marker 误跳过。
        assertThat(client.bulkIndexCalls).isEqualTo(2);
        // 验证远程索引状态重新变为存在。
        assertThat(client.remoteIndexExists).isTrue();
    }

    // 测试 marker 缺失但远程索引存在时会先删除旧索引，避免旧语料残留。
    @Test
    // 方法名描述测试意图：清理本地 marker 后重新启动应得到干净索引。
    void shouldDeleteRemoteIndexWhenMarkerIsMissingButRemoteIndexExists() {
        // 创建 fake Elasticsearch 客户端。
        FakeElasticsearchMedicalKnowledgeClient client = new FakeElasticsearchMedicalKnowledgeClient();
        // 模拟云端已有旧索引，但本地 marker 不存在。
        client.remoteIndexExists = true;
        // 创建索引服务。
        ElasticsearchMedicalKnowledgeIndexService service = service(client, false);

        // 执行导入。
        service.indexOnFirstImportOrRebuild(chunks());

        // 验证先删除旧索引。
        assertThat(client.deleteIndexCalls).isEqualTo(1);
        // 验证随后重新创建或确认索引。
        assertThat(client.ensureIndexCalls).isEqualTo(1);
        // 验证新数据被写入。
        assertThat(client.bulkIndexCalls).isEqualTo(1);
    }

    // 测试大量 chunk 会分批生成向量并立即写入 Elasticsearch。
    @Test
    // 方法名描述测试意图：五条数据、批量大小二，应该执行三次 bulk。
    void shouldIndexChunksInSmallBatches() {
        // 创建 fake Elasticsearch 客户端。
        FakeElasticsearchMedicalKnowledgeClient client = new FakeElasticsearchMedicalKnowledgeClient();
        // 使用批量大小二创建索引服务。
        ElasticsearchMedicalKnowledgeIndexService service = new ElasticsearchMedicalKnowledgeIndexService(
                // 注入 fake 客户端。
                client,
                // 注入确定性 embedding 模型。
                new DeterministicMedicalKnowledgeEmbeddingModel(16),
                // 设置测试索引名。
                "test-index",
                // 设置临时 marker 目录。
                tempDir.toString(),
                // 不强制重建。
                false,
                // 每批两条。
                2
        );

        // 写入五条测试 chunk。
        service.indexOnFirstImportOrRebuild(chunks(5));

        // 验证执行了三次 bulk。
        assertThat(client.bulkIndexCalls).isEqualTo(3);
        // 验证三个批次分别是 2、2、1 条。
        assertThat(client.bulkBatchSizes).containsExactly(2, 2, 1);
    }

    // 创建测试用索引服务。
    private ElasticsearchMedicalKnowledgeIndexService service(
            // fake Elasticsearch 客户端。
            FakeElasticsearchMedicalKnowledgeClient client,
            // 是否强制重建。
            boolean rebuildOnStartup
    ) {
        // 返回索引服务实例。
        return new ElasticsearchMedicalKnowledgeIndexService(
                // 注入 fake 客户端。
                client,
                // 注入确定性 embedding 模型。
                new DeterministicMedicalKnowledgeEmbeddingModel(16),
                // 设置测试索引名。
                "test-index",
                // 设置测试 marker 目录。
                tempDir.toString(),
                // 设置重建开关。
                rebuildOnStartup
        );
    }

    // 创建测试 chunk 列表。
    private static List<MedicalKnowledgeChunk> chunks() {
        // 返回单条测试 chunk。
        return List.of(MedicalKnowledgeChunk.from(document(), 0, "糖尿病患者应关注血糖监测。"));
    }

    // 创建指定数量的测试 chunk。
    private static List<MedicalKnowledgeChunk> chunks(int count) {
        // 创建结果列表。
        List<MedicalKnowledgeChunk> chunks = new ArrayList<>();
        // 生成不同 chunkIndex，确保 chunkId 唯一。
        for (int index = 0; index < count; index++) {
            // 添加当前测试片段。
            chunks.add(MedicalKnowledgeChunk.from(document(), index, "糖尿病患者应关注血糖监测。片段 " + index));
        }
        // 返回测试片段。
        return chunks;
    }

    // 创建测试文档。
    private static MedicalKnowledgeDocument document() {
        // 创建文档对象。
        MedicalKnowledgeDocument document = new MedicalKnowledgeDocument();
        // 设置文档 ID。
        document.setDocumentId("test:diabetes");
        // 设置标题。
        document.setTitle("糖尿病测试资料");
        // 设置正文。
        document.setText("糖尿病患者应关注血糖监测。");
        // 设置语言。
        document.setLanguage("Chinese");
        // 设置来源名称。
        document.setSourceName("测试来源");
        // 设置来源 URL。
        document.setSourceUrl("https://example.org/diabetes");
        // 设置文档类型。
        document.setDocumentType(MedicalKnowledgeDocumentType.HEALTH_TOPIC);
        // 设置科室。
        document.setDepartment("内分泌科");
        // 设置疾病或主题。
        document.setDiseaseOrTopic("糖尿病");
        // 设置获取日期。
        document.setRetrievedAt(LocalDate.of(2026, 7, 31));
        // 返回文档。
        return document;
    }

    // 定义不访问网络的 fake Elasticsearch 客户端。
    private static class FakeElasticsearchMedicalKnowledgeClient extends ElasticsearchMedicalKnowledgeClient {

        // 记录远程索引是否存在。
        private boolean remoteIndexExists;

        // 记录 ensureIndex 调用次数。
        private int ensureIndexCalls;

        // 记录 bulkIndex 调用次数。
        private int bulkIndexCalls;

        // 记录 deleteIndexIfExists 调用次数。
        private int deleteIndexCalls;

        // 记录每次 bulk 写入的文档数量。
        private final List<Integer> bulkBatchSizes = new ArrayList<>();

        // 创建 fake 客户端。
        private FakeElasticsearchMedicalKnowledgeClient() {
            // 调用父类构造器，但不会触发网络请求。
            super("http://localhost:9200", "", "", "test-index", "standard", "standard", 1.0, 1.0);
        }

        // fake 判断索引是否存在。
        @Override
        // 返回当前 fake 远程索引状态。
        public boolean indexExists() {
            // 返回索引状态。
            return remoteIndexExists;
        }

        // fake 删除索引。
        @Override
        // 删除时只修改内存状态。
        public void deleteIndexIfExists() {
            // 调用次数加一。
            deleteIndexCalls++;
            // 设置索引不存在。
            remoteIndexExists = false;
        }

        // fake 确保索引存在。
        @Override
        // 创建索引时只记录调用次数。
        public void ensureIndex(int embeddingDimension) {
            // 调用次数加一。
            ensureIndexCalls++;
            // 设置索引存在。
            remoteIndexExists = true;
        }

        // fake 批量写入。
        @Override
        // 写入时只记录调用次数。
        public void bulkIndex(List<MedicalKnowledgeChunk> chunks, List<double[]> embeddings) {
            // 调用次数加一。
            bulkIndexCalls++;
            // 记录当前批次大小。
            bulkBatchSizes.add(chunks.size());
        }

        // fake 检索方法。
        @Override
        // 当前测试不需要检索，返回空列表。
        public List<MedicalKnowledgeSearchHit> search(
                // 检索请求。
                MedicalKnowledgeSearchRequest request,
                // 检索模式。
                RetrievalMode retrievalMode,
                // 查询向量。
                double[] queryVector
        ) {
            // 返回空命中。
            return List.of();
        }
    }
}
