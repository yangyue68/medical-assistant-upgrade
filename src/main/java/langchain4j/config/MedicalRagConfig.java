// 声明当前类所在的包，config 表示它负责 Spring Bean 装配。
package langchain4j.config;

// MedicalKnowledgeChunk 是 RAG 检索阶段使用的知识片段。
import langchain4j.rag.MedicalKnowledgeChunk;
// MedicalKnowledgeDocument 是导入真实知识资料时使用的完整文档模型。
import langchain4j.rag.MedicalKnowledgeDocument;
// MedicalKnowledgeDocumentType 是医学知识文档类型枚举。
import langchain4j.rag.MedicalKnowledgeDocumentType;
// MedicalKnowledgeRetriever 是统一医学知识检索接口。
import langchain4j.rag.MedicalKnowledgeRetriever;
// MedicalRagContextBuilder 负责把检索结果组装成 prompt-ready 上下文。
import langchain4j.rag.application.MedicalRagContextBuilder;
// ExtractiveMedicalRagAnswerGenerator 是本地抽取式回答生成器。
import langchain4j.rag.application.ExtractiveMedicalRagAnswerGenerator;
// FallbackMedicalRagAnswerGenerator 给在线回答生成器提供本地兜底。
import langchain4j.rag.application.FallbackMedicalRagAnswerGenerator;
// MedicalRagAnswerGenerator 是 RAG 回答生成器接口。
import langchain4j.rag.application.MedicalRagAnswerGenerator;
// MedicalRagPromptBuilder 负责组装 RAG 回答 prompt。
import langchain4j.rag.application.MedicalRagPromptBuilder;
// MedicalRagService 是 RAG 应用编排服务。
import langchain4j.rag.application.MedicalRagService;
// OnlineMedicalRagAnswerGenerator 是在线模型 RAG 回答生成器。
import langchain4j.rag.application.OnlineMedicalRagAnswerGenerator;
// ElasticsearchMedicalKnowledgeClient 封装阿里云 Elasticsearch 索引和查询请求。
import langchain4j.rag.elasticsearch.ElasticsearchMedicalKnowledgeClient;
// ElasticsearchMedicalKnowledgeIndexService 负责首次导入或显式重建 Elasticsearch 索引。
import langchain4j.rag.elasticsearch.ElasticsearchMedicalKnowledgeIndexService;
// ElasticsearchMedicalKnowledgeRetriever 使用 Elasticsearch 承接关键词、向量和混合检索。
import langchain4j.rag.elasticsearch.ElasticsearchMedicalKnowledgeRetriever;
// DeterministicMedicalKnowledgeEmbeddingModel 是本地确定性 embedding 实现，主要用于测试和无在线模型的开发环境。
import langchain4j.rag.embedding.DeterministicMedicalKnowledgeEmbeddingModel;
// LangChain4jMedicalKnowledgeEmbeddingModel 适配 LangChain4j 在线 embedding 模型。
import langchain4j.rag.embedding.LangChain4jMedicalKnowledgeEmbeddingModel;
// MedicalKnowledgeEmbeddingModel 是医学知识 embedding 接口。
import langchain4j.rag.embedding.MedicalKnowledgeEmbeddingModel;
// MedicalKnowledgeChunker 负责把完整文档切分为可检索片段。
import langchain4j.rag.ingestion.MedicalKnowledgeChunker;
// MedicalKnowledgeIngestionService 负责解析后文档的切分和保存。
import langchain4j.rag.ingestion.MedicalKnowledgeIngestionService;
// PumchDepartmentPageHtmlParser 解析北京协和医院科室详情 HTML。
import langchain4j.rag.ingestion.PumchDepartmentPageHtmlParser;
// PumchDepartmentPageIngestionRunner 批量导入北京协和医院科室页面。
import langchain4j.rag.ingestion.PumchDepartmentPageIngestionRunner;
// DashScopeMedicalKnowledgeReranker 调用通义千问 rerank 模型精排召回结果。
import langchain4j.rag.rerank.DashScopeMedicalKnowledgeReranker;
// MedicalKnowledgeReranker 是 RAG 精排接口。
import langchain4j.rag.rerank.MedicalKnowledgeReranker;
// NoOpMedicalKnowledgeReranker 表示不启用精排。
import langchain4j.rag.rerank.NoOpMedicalKnowledgeReranker;
// InMemoryMedicalKnowledgeChunkStore 保存启动阶段解析出的 chunk 快照。
import langchain4j.rag.store.InMemoryMedicalKnowledgeChunkStore;
// MedicalKnowledgeChunkStore 是 chunk 仓储接口。
import langchain4j.rag.store.MedicalKnowledgeChunkStore;
// ChatLanguageModel 是 LangChain4j 的非流式聊天模型抽象。
import dev.langchain4j.model.chat.ChatLanguageModel;
// EmbeddingModel 是 LangChain4j 的在线 embedding 模型抽象。
import dev.langchain4j.model.embedding.EmbeddingModel;
// Path 表示本地知识目录和文件路径。
import java.nio.file.Path;
// LocalDate 表示真实资料的获取日期。
import java.time.LocalDate;
// List 表示一组知识片段或文档。
import java.util.List;
// Logger 是 SLF4J 日志接口，用于记录后台知识导入状态。
import org.slf4j.Logger;
// LoggerFactory 用于创建当前配置类的日志对象。
import org.slf4j.LoggerFactory;
// ConditionalOnProperty 按配置项决定是否注册某个 Bean。
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// ApplicationRunner 在 Spring Boot 应用启动后执行后台导入任务。
import org.springframework.boot.ApplicationRunner;
// Value 注解从配置文件或环境变量读取属性。
import org.springframework.beans.factory.annotation.Value;
// Bean 注解把方法返回值注册到 Spring 容器。
import org.springframework.context.annotation.Bean;
// Configuration 注解声明当前类是 Spring 配置类。
import org.springframework.context.annotation.Configuration;

// 标记当前类为 Spring 配置类。
@Configuration
// 定义医学 RAG 配置，当前由阿里云 Elasticsearch 承接关键词、向量和混合检索。
public class MedicalRagConfig {

    // 创建当前配置类的日志对象。
    private static final Logger log = LoggerFactory.getLogger(MedicalRagConfig.class);

    // 是否导入北京协和医院科室详情 HTML。
    @Value("${xiaozhi.rag.pumch-department-pages.enabled:true}")
    // 保存科室页面导入开关。
    private boolean pumchDepartmentPagesEnabled = true;

    // 北京协和医院科室详情 HTML 目录。
    @Value("${xiaozhi.rag.pumch-department-pages.source-dir:docs/knowledge/raw/pumch/2026-07-31/department-pages}")
    // 保存科室页面目录。
    private String pumchDepartmentPagesSourceDir = "docs/knowledge/raw/pumch/2026-07-31/department-pages";

    // 阿里云 Elasticsearch 访问地址。
    @Value("${xiaozhi.elasticsearch.endpoint:}")
    // 保存 Elasticsearch endpoint。
    private String elasticsearchEndpoint = "";

    // 阿里云 Elasticsearch 用户名。
    @Value("${xiaozhi.elasticsearch.username:elastic}")
    // 保存 Elasticsearch 用户名。
    private String elasticsearchUsername = "elastic";

    // 阿里云 Elasticsearch 密码。
    @Value("${xiaozhi.elasticsearch.password:}")
    // 保存 Elasticsearch 密码。
    private String elasticsearchPassword = "";

    // Elasticsearch 医学知识索引名。
    @Value("${xiaozhi.elasticsearch.index:xiaozhi-medical-knowledge}")
    // 保存 Elasticsearch 索引名。
    private String elasticsearchIndex = "xiaozhi-medical-knowledge";

    // Elasticsearch 正文索引分词器。
    @Value("${xiaozhi.elasticsearch.text-analyzer:standard}")
    // 保存正文索引分词器。
    private String elasticsearchTextAnalyzer = "standard";

    // Elasticsearch 正文查询分词器。
    @Value("${xiaozhi.elasticsearch.search-analyzer:standard}")
    // 保存正文查询分词器。
    private String elasticsearchSearchAnalyzer = "standard";

    // Elasticsearch 混合检索关键词分数权重。
    @Value("${xiaozhi.elasticsearch.keyword-weight:1.0}")
    // 保存关键词权重。
    private double elasticsearchKeywordWeight = 1.0;

    // Elasticsearch 混合检索向量相似度权重。
    @Value("${xiaozhi.elasticsearch.vector-weight:1.0}")
    // 保存向量权重。
    private double elasticsearchVectorWeight = 1.0;

    // 是否在启动时强制删除并重建 Elasticsearch 索引。
    @Value("${xiaozhi.elasticsearch.index.rebuild-on-startup:false}")
    // 保存重建开关。
    private boolean elasticsearchIndexRebuildOnStartup = false;

    // Elasticsearch 首次导入 marker 文件目录。
    @Value("${xiaozhi.elasticsearch.index.marker-dir:data/rag/elasticsearch}")
    // 保存 marker 目录。
    private String elasticsearchIndexMarkerDir = "data/rag/elasticsearch";

    // Elasticsearch 每批生成向量并写入的 chunk 数量。
    @Value("${xiaozhi.elasticsearch.index.batch-size:10}")
    // 保存批量大小。
    private int elasticsearchIndexBatchSize = 10;

    // 注册医学知识 embedding 模型 Bean。
    @Bean
    // 默认使用确定性 embedding，便于没有在线 key 时仍能跑通测试。
    @ConditionalOnProperty(name = "xiaozhi.rag.embedding.provider", havingValue = "deterministic", matchIfMissing = true)
    // 创建确定性 embedding 模型。
    public MedicalKnowledgeEmbeddingModel medicalKnowledgeEmbeddingModel() {
        // 创建 128 维确定性 embedding 模型。
        return new DeterministicMedicalKnowledgeEmbeddingModel(128);
    }

    // 注册通义千问 embedding 模型 Bean。
    @Bean
    // 只有显式配置 dashscope 时才启用在线 embedding。
    @ConditionalOnProperty(name = "xiaozhi.rag.embedding.provider", havingValue = "dashscope")
    // 创建 LangChain4j embedding 适配器。
    public MedicalKnowledgeEmbeddingModel dashScopeMedicalKnowledgeEmbeddingModel(
            // 注入 LangChain4j DashScope embedding 模型。
            EmbeddingModel embeddingModel,
            // 注入通义千问 API Key，用于提前失败提示。
            @Value("${langchain4j.community.dashscope.embedding-model.api-key:}") String apiKey
    ) {
        // 如果 API Key 为空，在线 embedding 无法工作。
        if (apiKey == null || apiKey.trim().isEmpty()) {
            // 抛出配置异常。
            throw new IllegalStateException("QWEN_API_KEY is required when xiaozhi.rag.embedding.provider=dashscope.");
        }
        // 返回在线 embedding 适配器。
        return new LangChain4jMedicalKnowledgeEmbeddingModel(embeddingModel);
    }

    // 注册 chunk 仓储 Bean。
    @Bean
    // chunk 仓储先保存轻量演示数据，真实资料在应用启动后由后台线程解析。
    public MedicalKnowledgeChunkStore medicalKnowledgeChunkStore() {
        // 创建内存 chunk 仓储。
        InMemoryMedicalKnowledgeChunkStore chunkStore = new InMemoryMedicalKnowledgeChunkStore();
        // 写入少量内置演示知识，保证空资料目录时仍有最小可用内容。
        chunkStore.saveAll(demoChunks());
        // 返回 chunk 仓储。
        return chunkStore;
    }

    // 注册 Elasticsearch 客户端 Bean。
    @Bean
    // 客户端统一负责索引创建、批量写入和检索查询。
    public ElasticsearchMedicalKnowledgeClient elasticsearchMedicalKnowledgeClient() {
        // 创建 Elasticsearch 客户端。
        return new ElasticsearchMedicalKnowledgeClient(
                // 传入 Elasticsearch endpoint。
                elasticsearchEndpoint,
                // 传入 Elasticsearch 用户名。
                elasticsearchUsername,
                // 传入 Elasticsearch 密码。
                elasticsearchPassword,
                // 传入索引名。
                elasticsearchIndex,
                // 传入索引分词器。
                elasticsearchTextAnalyzer,
                // 传入查询分词器。
                elasticsearchSearchAnalyzer,
                // 传入关键词权重。
                elasticsearchKeywordWeight,
                // 传入向量权重。
                elasticsearchVectorWeight
        );
    }

    // 注册统一医学知识检索器 Bean。
    @Bean
    // 关键词、向量和混合检索都由 ElasticsearchMedicalKnowledgeRetriever 分发到 Elasticsearch 查询。
    public MedicalKnowledgeRetriever medicalKnowledgeRetriever(
            // 注入 Elasticsearch 客户端。
            ElasticsearchMedicalKnowledgeClient client,
            // 注入医学知识 embedding 模型。
            MedicalKnowledgeEmbeddingModel embeddingModel
    ) {
        // 创建 Elasticsearch 统一检索器。
        return new ElasticsearchMedicalKnowledgeRetriever(client, embeddingModel);
    }

    // 注册 Elasticsearch 启动后后台导入任务。
    @Bean
    // 只有首次导入或显式重建时才会上传数据，避免每次启动重复写入和重复调用 embedding。
    public ApplicationRunner elasticsearchMedicalKnowledgeIndexStartupRunner(
            // 注入 Elasticsearch 客户端。
            ElasticsearchMedicalKnowledgeClient client,
            // 注入医学知识 embedding 模型。
            MedicalKnowledgeEmbeddingModel embeddingModel,
            // 注入 chunk 仓储。
            MedicalKnowledgeChunkStore chunkStore
    ) {
        // 返回应用启动后的执行逻辑。
        return args -> {
            // 创建后台导入服务。
            ElasticsearchMedicalKnowledgeIndexService indexService = new ElasticsearchMedicalKnowledgeIndexService(
                    // 传入 Elasticsearch 客户端。
                    client,
                    // 传入 embedding 模型。
                    embeddingModel,
                    // 传入索引名。
                    elasticsearchIndex,
                    // 传入 marker 目录。
                    elasticsearchIndexMarkerDir,
                    // 传入重建开关。
                    elasticsearchIndexRebuildOnStartup,
                    // 传入批量大小。
                    elasticsearchIndexBatchSize
            );
            // 创建后台线程，避免本地资料解析和首次索引阻塞前端访问。
            Thread uploadThread = new Thread(
                    // 在后台解析真实资料并执行首次导入或显式重建。
                    () -> ingestAndIndexMedicalKnowledge(chunkStore, indexService),
                    // 设置线程名，便于 IDEA 控制台排查。
                    "elasticsearch-medical-knowledge-index-startup"
            );
            // 设置为守护线程，应用关闭时不阻塞 JVM 退出。
            uploadThread.setDaemon(true);
            // 记录后台导入异常，避免静默失败并保留完整堆栈。
            uploadThread.setUncaughtExceptionHandler((thread, exception) ->
                    log.error("Elasticsearch medical knowledge background import failed.", exception));
            // 启动后台导入线程。
            uploadThread.start();
        };
    }

    // 在后台线程中解析全部本地知识资料并写入 Elasticsearch。
    private void ingestAndIndexMedicalKnowledge(
            // 保存解析结果的 chunk 仓储。
            MedicalKnowledgeChunkStore chunkStore,
            // Elasticsearch 索引服务。
            ElasticsearchMedicalKnowledgeIndexService indexService
    ) {
        // 统一捕获并记录失败，保证异常不会影响已经启动的 Web 服务。
        try {
            // 记录后台资料解析开始。
            log.info("Starting background medical knowledge parsing.");
            // 导入北京协和医院科室详情页。
            ingestPumchDepartmentPages(chunkStore);
            // 记录解析出的 chunk 数量。
            log.info("Background medical knowledge parsing completed. chunks={}", chunkStore.count());
            // 仅在首次导入或明确重建时写入 Elasticsearch。
            indexService.indexOnFirstImportOrRebuild(chunkStore.findAll());
        } catch (Exception exception) {
            // 输出完整异常，IDEA 控制台可直接看到失败环节和原因。
            log.error("Elasticsearch medical knowledge import failed. The web service remains available.", exception);
        }
    }

    // 注册不启用精排的 RAG reranker Bean。
    @Bean
    // 默认不精排，保留低成本开发路径。
    @ConditionalOnProperty(name = "xiaozhi.rag.rerank.provider", havingValue = "none", matchIfMissing = true)
    // 创建空操作精排器。
    public MedicalKnowledgeReranker noOpMedicalKnowledgeReranker() {
        // 返回 NoOp 精排器。
        return new NoOpMedicalKnowledgeReranker();
    }

    // 注册通义千问 rerank 精排 Bean。
    @Bean
    // 只有显式配置 dashscope 时才启用在线 rerank。
    @ConditionalOnProperty(name = "xiaozhi.rag.rerank.provider", havingValue = "dashscope")
    // 创建 DashScope rerank 精排器。
    public MedicalKnowledgeReranker dashScopeMedicalKnowledgeReranker(
            // 注入通义千问 API Key。
            @Value("${langchain4j.community.dashscope.embedding-model.api-key:}") String apiKey,
            // 注入 rerank 模型名。
            @Value("${xiaozhi.rag.rerank.model-name:qwen3-rerank}") String modelName,
            // 注入阿里云百炼业务空间 ID。
            @Value("${xiaozhi.rag.rerank.workspace-id:}") String workspaceId
    ) {
        // 如果 key 或 workspace 为空，rerank 无法调用。
        if (apiKey == null || apiKey.trim().isEmpty() || workspaceId == null || workspaceId.trim().isEmpty()) {
            // 抛出配置异常。
            throw new IllegalStateException("QWEN_API_KEY and QWEN_WORKSPACE_ID are required when xiaozhi.rag.rerank.provider=dashscope.");
        }
        // 组装 DashScope 兼容 API rerank endpoint。
        String endpoint = "https://" + workspaceId.trim() + ".cn-beijing.maas.aliyuncs.com/compatible-api/v1/reranks";
        // 返回 DashScope 精排器。
        return new DashScopeMedicalKnowledgeReranker(endpoint, apiKey, modelName);
    }

    // 注册 RAG 上下文构建器 Bean。
    @Bean
    // 上下文构建器负责把检索结果组装成 prompt-ready 文本。
    public MedicalRagContextBuilder medicalRagContextBuilder() {
        // 使用默认上下文预算和安全提示。
        return new MedicalRagContextBuilder();
    }

    // 注册 RAG prompt 构建器 Bean。
    @Bean
    // Prompt 构建器负责把上下文和用户问题组装成模型输入。
    public MedicalRagPromptBuilder medicalRagPromptBuilder() {
        // 创建 RAG prompt 构建器。
        return new MedicalRagPromptBuilder();
    }

    // 注册本地抽取式回答生成器 Bean。
    @Bean
    // 默认使用本地抽取式生成器，不调用外部大模型 API。
    @ConditionalOnProperty(name = "xiaozhi.rag.answer-generator.mode", havingValue = "extractive", matchIfMissing = true)
    // 创建本地抽取式回答生成器。
    public MedicalRagAnswerGenerator medicalRagAnswerGenerator() {
        // 返回抽取式回答生成器。
        return new ExtractiveMedicalRagAnswerGenerator();
    }

    // 注册在线 RAG 回答生成器 Bean。
    @Bean
    // 只有显式配置 xiaozhi.rag.answer-generator.mode=online 时才启用在线模型。
    @ConditionalOnProperty(name = "xiaozhi.rag.answer-generator.mode", havingValue = "online")
    // 创建在线模式回答生成器。
    public MedicalRagAnswerGenerator onlineMedicalRagAnswerGenerator(
            // Spring 注入 LangChain4j 在线聊天模型，当前通常是 DashScope QwenChatModel。
            ChatLanguageModel chatLanguageModel
    ) {
        // 创建在线回答生成器。
        OnlineMedicalRagAnswerGenerator onlineGenerator = new OnlineMedicalRagAnswerGenerator(chatLanguageModel);
        // 创建本地抽取式兜底生成器。
        ExtractiveMedicalRagAnswerGenerator fallbackGenerator = new ExtractiveMedicalRagAnswerGenerator();
        // 返回带兜底能力的在线回答生成器。
        return new FallbackMedicalRagAnswerGenerator(onlineGenerator, fallbackGenerator);
    }

    // 注册 RAG 应用服务 Bean。
    @Bean
    // RAG 应用服务是 Controller 和 Agent 调用的统一入口。
    public MedicalRagService medicalRagService(
            // 注入统一检索器。
            MedicalKnowledgeRetriever retriever,
            // 注入上下文构建器。
            MedicalRagContextBuilder contextBuilder,
            // 注入 prompt 构建器。
            MedicalRagPromptBuilder promptBuilder,
            // 注入回答生成器。
            MedicalRagAnswerGenerator answerGenerator,
            // 注入可选精排器。
            MedicalKnowledgeReranker reranker
    ) {
        // 创建 RAG 应用服务。
        return new MedicalRagService(retriever, contextBuilder, promptBuilder, answerGenerator, reranker);
    }

    // 创建内置演示知识片段。
    private List<MedicalKnowledgeChunk> demoChunks() {
        // 返回少量演示 chunk；真实知识库导入后会和真实数据一起进入 Elasticsearch。
        return List.of(
                // 糖尿病中文演示知识。
                MedicalKnowledgeChunk.from(
                        // 创建糖尿病文档。
                        demoDocument(
                                // 文档 ID。
                                "demo:diabetes",
                                // 标题。
                                "糖尿病健康科普",
                                // 正文。
                                "糖尿病是一类以血糖升高为特征的慢性代谢性疾病。常见表现包括口渴、多尿、体重下降、乏力等。"
                                        + " 如果出现明显不适或血糖异常，应及时到内分泌科就诊，由医生结合检查结果判断。",
                                // 语言。
                                "Chinese",
                                // 文档类型。
                                MedicalKnowledgeDocumentType.HEALTH_TOPIC,
                                // 科室。
                                "内分泌科"
                        ),
                        // 片段序号。
                        0,
                        // 片段正文。
                        "糖尿病是一类以血糖升高为特征的慢性代谢性疾病。常见表现包括口渴、多尿、体重下降、乏力等。"
                                + " 如果出现明显不适或血糖异常，应及时到内分泌科就诊，由医生结合检查结果判断。"
                ),
                // 急症安全边界演示知识。
                MedicalKnowledgeChunk.from(
                        // 创建急症安全文档。
                        demoDocument(
                                // 文档 ID。
                                "demo:emergency",
                                // 标题。
                                "急症就医提醒",
                                // 正文。
                                "如果出现胸痛、呼吸困难、意识障碍、大出血、严重过敏、疑似卒中等急症表现，应立即拨打急救电话或前往急诊。",
                                // 语言。
                                "Chinese",
                                // 文档类型。
                                MedicalKnowledgeDocumentType.SAFETY_POLICY,
                                // 科室。
                                "急诊科"
                        ),
                        // 片段序号。
                        0,
                        // 片段正文。
                        "如果出现胸痛、呼吸困难、意识障碍、大出血、严重过敏、疑似卒中等急症表现，应立即拨打急救电话或前往急诊。"
                )
        );
    }

    // 导入北京协和医院科室详情 HTML。
    private void ingestPumchDepartmentPages(MedicalKnowledgeChunkStore chunkStore) {
        // 如果关闭导入，就直接返回。
        if (!pumchDepartmentPagesEnabled) {
            // 结束导入流程。
            return;
        }
        // 创建导入服务。
        MedicalKnowledgeIngestionService ingestionService = new MedicalKnowledgeIngestionService(
                // 使用默认 chunker。
                new MedicalKnowledgeChunker(),
                // 注入 chunk 仓储。
                chunkStore
        );
        // 创建科室页面导入 runner 并执行导入。
        new PumchDepartmentPageIngestionRunner(
                // 使用科室页面 HTML 解析器。
                new PumchDepartmentPageHtmlParser(),
                // 注入导入服务。
                ingestionService
        ).ingest(Path.of(pumchDepartmentPagesSourceDir.trim()), LocalDate.of(2026, 7, 31));
    }

    // 创建演示医学知识文档。
    private MedicalKnowledgeDocument demoDocument(
            // 文档 ID。
            String documentId,
            // 标题。
            String title,
            // 正文。
            String text,
            // 语言。
            String language,
            // 文档类型。
            MedicalKnowledgeDocumentType documentType,
            // 科室。
            String department
    ) {
        // 创建医学知识文档。
        MedicalKnowledgeDocument document = new MedicalKnowledgeDocument();
        // 设置文档 ID。
        document.setDocumentId(documentId);
        // 设置标题。
        document.setTitle(title);
        // 设置正文。
        document.setText(text);
        // 设置语言。
        document.setLanguage(language);
        // 设置来源名称，明确这是项目内置演示知识。
        document.setSourceName("项目内置演示知识");
        // 设置内部来源 URL，避免伪装成外部权威来源。
        document.setSourceUrl("internal://demo/" + documentId);
        // 设置文档类型。
        document.setDocumentType(documentType);
        // 设置关联科室。
        document.setDepartment(department);
        // 设置疾病或主题。
        document.setDiseaseOrTopic(title);
        // 设置来源创建日期。
        document.setSourceCreatedAt("2026-07-27");
        // 设置来源生成日期。
        document.setSourceGeneratedAt("2026-07-27");
        // 设置项目获取日期。
        document.setRetrievedAt(LocalDate.of(2026, 7, 27));
        // 返回文档。
        return document;
    }
}
