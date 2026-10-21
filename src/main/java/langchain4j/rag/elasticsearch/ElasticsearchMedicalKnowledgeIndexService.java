// 声明当前类所在的包，elasticsearch 表示它负责 Elasticsearch 知识库索引。
package langchain4j.rag.elasticsearch;

// MedicalKnowledgeChunk 是待写入 Elasticsearch 的知识片段。
import langchain4j.rag.MedicalKnowledgeChunk;
// MedicalKnowledgeEmbeddingModel 用于把知识片段正文转换成向量。
import langchain4j.rag.embedding.MedicalKnowledgeEmbeddingModel;
// IOException 表示本地 marker 文件写入失败。
import java.io.IOException;
// StandardCharsets 提供 UTF-8 编码常量。
import java.nio.charset.StandardCharsets;
// Files 提供 marker 文件读写和目录创建能力。
import java.nio.file.Files;
// Path 表示 marker 文件目录和路径。
import java.nio.file.Path;
// Instant 用于记录索引写入时间。
import java.time.Instant;
// ArrayList 用于保存批量 embedding 结果。
import java.util.ArrayList;
// List 表示一组 chunk 或一组向量。
import java.util.List;
// Logger 是 SLF4J 日志接口。
import org.slf4j.Logger;
// LoggerFactory 用来创建日志对象。
import org.slf4j.LoggerFactory;
// StringUtils 提供字符串判空工具。
import org.springframework.util.StringUtils;

// 定义 Elasticsearch 医学知识索引服务，负责首次导入和显式重建。
public class ElasticsearchMedicalKnowledgeIndexService {

    // 默认每批处理 10 条，符合在线 embedding 常见批量限制并及时把进度写入 ES。
    private static final int DEFAULT_BATCH_SIZE = 10;

    // 创建当前类日志对象。
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchMedicalKnowledgeIndexService.class);

    // 保存 Elasticsearch 客户端。
    private final ElasticsearchMedicalKnowledgeClient client;

    // 保存 embedding 模型。
    private final MedicalKnowledgeEmbeddingModel embeddingModel;

    // 保存 Elasticsearch 索引名。
    private final String indexName;

    // 保存 marker 文件目录。
    private final String markerDirectory;

    // 保存是否启动时强制重建。
    private final boolean rebuildOnStartup;

    // 保存每次 embedding 和 bulk 写入的 chunk 数量。
    private final int batchSize;

    // 创建 Elasticsearch 索引服务。
    public ElasticsearchMedicalKnowledgeIndexService(
            // Elasticsearch 客户端。
            ElasticsearchMedicalKnowledgeClient client,
            // embedding 模型。
            MedicalKnowledgeEmbeddingModel embeddingModel,
            // Elasticsearch 索引名。
            String indexName,
            // marker 文件目录。
            String markerDirectory,
            // 是否启动时强制重建。
            boolean rebuildOnStartup
    ) {
        // 委托给可配置批量大小的完整构造器。
        this(client, embeddingModel, indexName, markerDirectory, rebuildOnStartup, DEFAULT_BATCH_SIZE);
    }

    // 创建可配置批量大小的 Elasticsearch 索引服务。
    public ElasticsearchMedicalKnowledgeIndexService(
            // Elasticsearch 客户端。
            ElasticsearchMedicalKnowledgeClient client,
            // embedding 模型。
            MedicalKnowledgeEmbeddingModel embeddingModel,
            // Elasticsearch 索引名。
            String indexName,
            // marker 文件目录。
            String markerDirectory,
            // 是否启动时强制重建。
            boolean rebuildOnStartup,
            // 每批处理的 chunk 数量。
            int batchSize
    ) {
        // Elasticsearch 客户端不能为空。
        if (client == null) {
            // 抛出调用错误。
            throw new IllegalArgumentException("client must not be null.");
        }
        // embedding 模型不能为空。
        if (embeddingModel == null) {
            // 抛出调用错误。
            throw new IllegalArgumentException("embeddingModel must not be null.");
        }
        // 索引名不能为空。
        if (!StringUtils.hasText(indexName)) {
            // 抛出配置错误。
            throw new IllegalArgumentException("indexName must not be blank.");
        }
        // 批量大小必须为正数。
        if (batchSize <= 0) {
            // 抛出配置错误。
            throw new IllegalArgumentException("batchSize must be greater than zero.");
        }
        // 保存 Elasticsearch 客户端。
        this.client = client;
        // 保存 embedding 模型。
        this.embeddingModel = embeddingModel;
        // 保存索引名。
        this.indexName = indexName.trim();
        // 保存 marker 目录。
        this.markerDirectory = StringUtils.hasText(markerDirectory) ? markerDirectory.trim() : "data/rag/elasticsearch";
        // 保存重建开关。
        this.rebuildOnStartup = rebuildOnStartup;
        // 保存批量大小。
        this.batchSize = batchSize;
    }

    // 仅在首次导入或显式重建时写入 Elasticsearch。
    public void indexOnFirstImportOrRebuild(List<MedicalKnowledgeChunk> chunks) {
        // 计算 marker 文件路径。
        Path markerFile = markerFile();
        // 判断本地 marker 是否存在。
        boolean markerExists = Files.isRegularFile(markerFile);
        // 判断远程 Elasticsearch 索引是否存在。
        boolean remoteIndexExists = client.indexExists();
        // 如果不要求重建、本地 marker 存在且远程索引也存在，就跳过导入。
        if (!rebuildOnStartup && markerExists && remoteIndexExists) {
            // 输出跳过日志。
            log.info("Skipping Elasticsearch knowledge import because marker exists: {}", markerFile);
            // 结束导入流程。
            return;
        }
        // 如果要求重建，就先删除旧索引。
        if (rebuildOnStartup) {
            // 输出重建日志。
            log.info("Rebuilding Elasticsearch knowledge index: {}", indexName);
            // 删除旧索引。
            client.deleteIndexIfExists();
        } else if (!markerExists && remoteIndexExists) {
            // 本地 marker 缺失说明当前项目需要重新导入；远端旧索引必须清理，避免残留旧语料。
            log.info("Deleting existing Elasticsearch knowledge index because local import marker is missing: {}", indexName);
            // 删除旧索引。
            client.deleteIndexIfExists();
        }
        // 确保索引存在。
        client.ensureIndex(embeddingModel.dimension());
        // 过滤可索引 chunk。
        List<MedicalKnowledgeChunk> indexableChunks = indexableChunks(chunks);
        // 记录本次导入总量，让 IDEA 控制台可以观察上传是否在推进。
        log.info("Starting Elasticsearch medical knowledge import. index={}, chunks={}, batchSize={}",
                indexName, indexableChunks.size(), batchSize);
        // 分批生成向量并立即写入，避免全量 embedding 完成前索引一直为 0。
        int indexedCount = indexInBatches(indexableChunks);
        // 写入 marker 文件。
        writeMarker(markerFile, indexedCount);
        // 记录完成日志。
        log.info("Elasticsearch medical knowledge import completed. index={}, chunks={}", indexName, indexedCount);
    }

    // 过滤可写入 Elasticsearch 的 chunk。
    private List<MedicalKnowledgeChunk> indexableChunks(List<MedicalKnowledgeChunk> chunks) {
        // 创建结果列表。
        List<MedicalKnowledgeChunk> indexableChunks = new ArrayList<>();
        // 如果原始列表为空，返回空列表。
        if (chunks == null || chunks.isEmpty()) {
            // 返回空列表。
            return indexableChunks;
        }
        // 遍历 chunk。
        for (MedicalKnowledgeChunk chunk : chunks) {
            // 跳过空 chunk 和空正文。
            if (chunk == null || !StringUtils.hasText(chunk.getText())) {
                // 继续下一个。
                continue;
            }
            // 添加可索引 chunk。
            indexableChunks.add(chunk);
        }
        // 返回可索引列表。
        return indexableChunks;
    }

    // 分批生成 embedding 并写入 Elasticsearch。
    private int indexInBatches(List<MedicalKnowledgeChunk> chunks) {
        // 初始化已写入数量。
        int indexedCount = 0;
        // 按 batchSize 遍历全部 chunk。
        for (int start = 0; start < chunks.size(); start += batchSize) {
            // 计算当前批次结束位置。
            int end = Math.min(start + batchSize, chunks.size());
            // 创建当前批次快照。
            List<MedicalKnowledgeChunk> batch = new ArrayList<>(chunks.subList(start, end));
            // 提取当前批次正文。
            List<String> texts = batch.stream().map(MedicalKnowledgeChunk::getText).toList();
            // 调用 embedding 批量接口生成向量。
            List<double[]> embeddings = embeddingModel.embedAll(texts);
            // 校验向量数量，防止 chunk 和 embedding 错位。
            if (embeddings == null || embeddings.size() != batch.size()) {
                // 抛出状态异常并停止写 marker，便于下次启动继续重试。
                throw new IllegalStateException("Embedding batch size does not match medical knowledge chunk batch size.");
            }
            // 当前批次生成成功后立即写入 Elasticsearch。
            client.bulkIndex(batch, embeddings);
            // 累加已写入数量。
            indexedCount += batch.size();
            // 每批输出进度日志。
            log.info("Elasticsearch medical knowledge import progress: {}/{}", indexedCount, chunks.size());
        }
        // 返回成功写入数量。
        return indexedCount;
    }

    // 生成 marker 文件路径。
    private Path markerFile() {
        // 清理索引名，避免非法文件名字符。
        String safeIndexName = indexName.replaceAll("[^a-zA-Z0-9._-]", "_");
        // 返回 marker 文件路径。
        return Path.of(markerDirectory, safeIndexName + ".uploaded");
    }

    // 写入 marker 文件。
    private void writeMarker(Path markerFile, int chunkCount) {
        // try 块中创建目录和写文件。
        try {
            // 创建 marker 目录。
            Files.createDirectories(markerFile.getParent());
            // 写入导入信息。
            Files.writeString(
                    // marker 文件路径。
                    markerFile,
                    // marker 文件内容。
                    "uploadedAt=" + Instant.now() + "\n"
                            + "elasticsearchIndex=" + indexName + "\n"
                            + "chunkCount=" + chunkCount + "\n",
                    // 使用 UTF-8。
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            // marker 写入失败时抛错，避免后续重复导入判断失效。
            throw new IllegalStateException("Failed to write Elasticsearch import marker: " + markerFile, exception);
        }
    }
}
