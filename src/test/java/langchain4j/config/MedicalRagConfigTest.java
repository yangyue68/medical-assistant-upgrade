// 声明测试类所在的包。
package langchain4j.config;

// MedicalKnowledgeRetriever 是统一医学知识检索接口。
import langchain4j.rag.MedicalKnowledgeRetriever;
// MedicalKnowledgeSearchHit 表示检索命中。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// MedicalKnowledgeSearchRequest 是 RAG 检索请求。
import langchain4j.rag.MedicalKnowledgeSearchRequest;
// MedicalKnowledgeSearchResult 表示检索结果。
import langchain4j.rag.MedicalKnowledgeSearchResult;
// RetrievalMode 是检索模式枚举。
import langchain4j.rag.RetrievalMode;
// MedicalRagContext 是 RAG 应用服务返回的上下文。
import langchain4j.rag.application.MedicalRagContext;
// MedicalRagContextBuilder 是 RAG 上下文构建器。
import langchain4j.rag.application.MedicalRagContextBuilder;
// MedicalRagAnswerGenerator 是 RAG 回答生成器接口。
import langchain4j.rag.application.MedicalRagAnswerGenerator;
// MedicalRagPromptBuilder 是 RAG prompt 构建器。
import langchain4j.rag.application.MedicalRagPromptBuilder;
// MedicalRagService 是 RAG 应用服务。
import langchain4j.rag.application.MedicalRagService;
// FallbackMedicalRagAnswerGenerator 是在线模式下使用的带兜底回答生成器。
import langchain4j.rag.application.FallbackMedicalRagAnswerGenerator;
// ElasticsearchMedicalKnowledgeClient 是新的 Elasticsearch 知识库客户端。
import langchain4j.rag.elasticsearch.ElasticsearchMedicalKnowledgeClient;
// MedicalKnowledgeEmbeddingModel 是医学知识 embedding 模型接口。
import langchain4j.rag.embedding.MedicalKnowledgeEmbeddingModel;
// MedicalKnowledgeChunkStore 是 chunk 仓储接口。
import langchain4j.rag.store.MedicalKnowledgeChunkStore;
// ChatLanguageModel 是 LangChain4j 的聊天模型抽象。
import dev.langchain4j.model.chat.ChatLanguageModel;
// List 表示一组命中。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;
// ReflectionTestUtils 用于给直接 new 出来的配置对象注入测试配置值。
import org.springframework.test.util.ReflectionTestUtils;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;

// 定义医学 RAG 配置的单元测试。
class MedicalRagConfigTest {

    // 测试配置类可以轻量创建知识仓储和 Elasticsearch 客户端边界。
    @Test
    // 方法名描述测试意图：Bean 创建阶段不应同步解析全部真实资料阻塞 Web 服务。
    void shouldBuildLightweightKnowledgeStoreAndElasticsearchClient() {
        // 创建配置类。
        MedicalRagConfig config = new MedicalRagConfig();
        // 关闭协和科室页面导入，让测试保持轻量。
        ReflectionTestUtils.setField(config, "pumchDepartmentPagesEnabled", false);
        // 设置 Elasticsearch 测试 endpoint；创建客户端不会发起网络请求。
        ReflectionTestUtils.setField(config, "elasticsearchEndpoint", "http://localhost:9200");

        // 创建 chunk 仓储。
        MedicalKnowledgeChunkStore chunkStore = config.medicalKnowledgeChunkStore();
        // 创建 Elasticsearch 客户端。
        ElasticsearchMedicalKnowledgeClient client = config.elasticsearchMedicalKnowledgeClient();

        // 验证 Bean 创建阶段只加载两条内置演示数据。
        assertThat(chunkStore.count()).isEqualTo(2);
        // 验证真实资料尚未在主启动线程中解析。
        assertThat(chunkStore.findAll())
                .extracting("sourceName")
                .containsOnly("项目内置演示知识");
        // 验证 Elasticsearch 客户端可以被配置类创建。
        assertThat(client).isNotNull();
    }

    // 测试配置类可以用统一检索器组装 RAG 应用服务。
    @Test
    // 方法名描述测试意图：RAG 服务构造不再依赖旧的三套检索器。
    void shouldBuildRagServiceWithSingleRetriever() {
        // 创建配置类。
        MedicalRagConfig config = new MedicalRagConfig();
        // 创建固定返回结果的统一检索器。
        MedicalKnowledgeRetriever retriever = fixedRetriever();
        // 创建上下文构建器。
        MedicalRagContextBuilder contextBuilder = config.medicalRagContextBuilder();
        // 创建 prompt 构建器。
        MedicalRagPromptBuilder promptBuilder = config.medicalRagPromptBuilder();
        // 创建回答生成器。
        MedicalRagAnswerGenerator answerGenerator = config.medicalRagAnswerGenerator();
        // 创建 RAG 应用服务。
        MedicalRagService ragService = config.medicalRagService(
                // 注入统一检索器。
                retriever,
                // 注入上下文构建器。
                contextBuilder,
                // 注入 prompt 构建器。
                promptBuilder,
                // 注入回答生成器。
                answerGenerator,
                // 注入不精排实现。
                config.noOpMedicalKnowledgeReranker()
        );

        // 准备 RAG 上下文。
        MedicalRagContext context = ragService.prepareContext(request());

        // 验证默认上下文使用混合检索。
        assertThat(context.getRetrievalMode()).isEqualTo(RetrievalMode.HYBRID);
        // 验证上下文能包含 fake 片段。
        assertThat(context.getContextText()).contains("糖尿病测试片段");
        // 验证上下文包含医疗安全边界。
        assertThat(context.getContextText()).contains("【医疗安全边界】");
        // 验证引用列表不为空。
        assertThat(context.getCitations()).isNotEmpty();
    }

    // 测试在线模式会创建带兜底能力的回答生成器。
    @Test
    // 方法名描述测试意图：配置类应该能组装在线模型生成器边界。
    void shouldBuildOnlineAnswerGeneratorWithFallback() {
        // 创建配置类。
        MedicalRagConfig config = new MedicalRagConfig();
        // 创建 fake 聊天模型，避免测试访问真实在线模型。
        ChatLanguageModel chatLanguageModel = new ChatLanguageModel() {
            // 覆盖 chat(String) 方法。
            @Override
            public String chat(String userMessage) {
                // 返回 fake 在线模型回答。
                return "online answer";
            }
        };

        // 创建在线回答生成器。
        MedicalRagAnswerGenerator answerGenerator = config.onlineMedicalRagAnswerGenerator(chatLanguageModel);

        // 验证在线模式返回的是带兜底能力的生成器。
        assertThat(answerGenerator).isInstanceOf(FallbackMedicalRagAnswerGenerator.class);
    }

    // 创建固定返回结果的统一检索器。
    private static MedicalKnowledgeRetriever fixedRetriever() {
        // 返回 lambda 检索器。
        return request -> MedicalKnowledgeSearchResult.of(
                // 设置查询文本。
                request.getQuery(),
                // 使用请求规整后的检索模式。
                request.normalizedRetrievalMode(),
                // 返回一条测试命中。
                List.of(hit())
        );
    }

    // 创建测试检索请求。
    private static MedicalKnowledgeSearchRequest request() {
        // 创建请求对象。
        MedicalKnowledgeSearchRequest request = new MedicalKnowledgeSearchRequest();
        // 设置查询问题。
        request.setQuery("糖尿病早期有哪些症状");
        // 设置返回数量。
        request.setTopK(3);
        // 设置语言过滤。
        request.setLanguage("Chinese");
        // 返回请求对象。
        return request;
    }

    // 创建测试命中。
    private static MedicalKnowledgeSearchHit hit() {
        // 创建命中对象。
        MedicalKnowledgeSearchHit hit = new MedicalKnowledgeSearchHit();
        // 设置 chunkId。
        hit.setChunkId("test:diabetes#chunk-0");
        // 设置文档 ID。
        hit.setDocumentId("test:diabetes");
        // 设置片段序号。
        hit.setChunkIndex(0);
        // 设置标题。
        hit.setTitle("糖尿病测试资料");
        // 设置片段。
        hit.setSnippet("糖尿病测试片段：患者应关注血糖监测、规律复诊和生活方式管理。");
        // 设置来源名称。
        hit.setSourceName("测试来源");
        // 设置来源 URL。
        hit.setSourceUrl("https://example.org/diabetes");
        // 设置召回方式。
        hit.setRetrievalSource("TEST");
        // 返回命中。
        return hit;
    }
}
