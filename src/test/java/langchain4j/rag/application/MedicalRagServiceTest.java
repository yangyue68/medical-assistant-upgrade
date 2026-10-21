// 声明测试类所在的包。
package langchain4j.rag.application;

// MedicalKnowledgeRetriever 是统一医学知识检索接口。
import langchain4j.rag.MedicalKnowledgeRetriever;
// MedicalKnowledgeSearchHit 表示检索命中。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// MedicalKnowledgeSearchRequest 表示检索请求。
import langchain4j.rag.MedicalKnowledgeSearchRequest;
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

// 定义医学 RAG 应用服务的单元测试。
class MedicalRagServiceTest {

    // 测试默认请求会选择混合检索。
    @Test
    // 方法名描述测试意图：未指定模式时应该走 HYBRID。
    void shouldUseHybridRetrieverByDefault() {
        // 创建 RAG 服务。
        MedicalRagService service = service();
        // 创建检索请求。
        MedicalKnowledgeSearchRequest request = request("糖尿病早期症状", null);

        // 准备 RAG 上下文。
        MedicalRagContext context = service.prepareContext(request);

        // 验证使用混合检索。
        assertThat(context.getRetrievalMode()).isEqualTo(RetrievalMode.HYBRID);
        // 验证上下文来自混合检索命中。
        assertThat(context.getContextText()).contains("HYBRID snippet");
    }

    // 测试显式 KEYWORD 模式会选择关键词检索。
    @Test
    // 方法名描述测试意图：KEYWORD 请求不应该走向量或混合检索器。
    void shouldUseKeywordRetrieverWhenRequested() {
        // 创建 RAG 服务。
        MedicalRagService service = service();
        // 创建关键词检索请求。
        MedicalKnowledgeSearchRequest request = request("A1C", RetrievalMode.KEYWORD);

        // 准备 RAG 上下文。
        MedicalRagContext context = service.prepareContext(request);

        // 验证使用关键词检索。
        assertThat(context.getRetrievalMode()).isEqualTo(RetrievalMode.KEYWORD);
        // 验证上下文来自关键词检索命中。
        assertThat(context.getContextText()).contains("KEYWORD snippet");
    }

    // 测试显式 VECTOR 模式会选择向量检索。
    @Test
    // 方法名描述测试意图：VECTOR 请求应该走向量检索器。
    void shouldUseVectorRetrieverWhenRequested() {
        // 创建 RAG 服务。
        MedicalRagService service = service();
        // 创建向量检索请求。
        MedicalKnowledgeSearchRequest request = request("blood sugar", RetrievalMode.VECTOR);

        // 准备 RAG 上下文。
        MedicalRagContext context = service.prepareContext(request);

        // 验证使用向量检索。
        assertThat(context.getRetrievalMode()).isEqualTo(RetrievalMode.VECTOR);
        // 验证上下文来自向量检索命中。
        assertThat(context.getContextText()).contains("VECTOR snippet");
    }

    // 测试空问题会被拒绝。
    @Test
    // 方法名描述测试意图：RAG 应用服务不接受空问题。
    void shouldRejectBlankQuery() {
        // 创建 RAG 服务。
        MedicalRagService service = service();
        // 创建空查询请求。
        MedicalKnowledgeSearchRequest request = request(" ", RetrievalMode.HYBRID);

        // 验证准备上下文时抛出非法参数异常。
        assertThatThrownBy(() -> service.prepareContext(request))
                // 验证异常类型。
                .isInstanceOf(IllegalArgumentException.class)
                // 验证异常信息。
                .hasMessageContaining("检索问题不能为空");
    }

    // 测试 RAG 应用服务可以生成回答且默认不返回 prompt。
    @Test
    // 方法名描述测试意图：answer 方法应该返回回答和引用，但默认隐藏内部 prompt。
    void shouldGenerateAnswerWithoutPromptByDefault() {
        // 创建 RAG 服务。
        MedicalRagService service = service();
        // 创建回答请求。
        MedicalRagAnswerRequest request = answerRequest("糖尿病早期症状", false);

        // 生成 RAG 回答。
        MedicalRagAnswerResponse response = service.answer(request);

        // 验证回答包含检索片段内容。
        assertThat(response.getAnswer()).contains("HYBRID snippet");
        // 验证响应中带有引用。
        assertThat(response.getCitations()).hasSize(1);
        // 验证默认不返回 prompt。
        assertThat(response.getPrompt()).isNull();
    }

    // 测试显式 includePrompt=true 时返回 prompt。
    @Test
    // 方法名描述测试意图：调试场景可以显式要求返回 prompt。
    void shouldReturnPromptWhenRequested() {
        // 创建 RAG 服务。
        MedicalRagService service = service();
        // 创建回答请求。
        MedicalRagAnswerRequest request = answerRequest("糖尿病早期症状", true);

        // 生成 RAG 回答。
        MedicalRagAnswerResponse response = service.answer(request);

        // 验证 prompt 被返回。
        assertThat(response.getPrompt()).contains("【RAG 检索上下文】");
        // 验证 prompt 包含用户问题。
        assertThat(response.getPrompt()).contains("糖尿病早期症状");
    }

    // 创建 RAG 回答请求。
    private static MedicalRagAnswerRequest answerRequest(String query, boolean includePrompt) {
        // 创建回答请求。
        MedicalRagAnswerRequest answerRequest = new MedicalRagAnswerRequest();
        // 设置检索请求。
        answerRequest.setSearchRequest(request(query, RetrievalMode.HYBRID));
        // 设置是否返回 prompt。
        answerRequest.setIncludePrompt(includePrompt);
        // 返回回答请求。
        return answerRequest;
    }

    // 创建测试用 RAG 服务。
    private static MedicalRagService service() {
        // 创建统一检索器。
        MedicalKnowledgeRetriever retriever = fixedRetriever();
        // 创建 RAG 服务。
        return new MedicalRagService(
                // 注入统一检索器。
                retriever,
                // 注入上下文构建器。
                new MedicalRagContextBuilder(),
                // 注入 prompt 构建器。
                new MedicalRagPromptBuilder(),
                // 注入抽取式回答生成器。
                new ExtractiveMedicalRagAnswerGenerator()
        );
    }

    // 创建固定返回结果的统一检索器。
    private static MedicalKnowledgeRetriever fixedRetriever() {
        // 返回 lambda 检索器。
        return request -> {
            // 读取请求中的实际检索模式。
            RetrievalMode retrievalMode = request.normalizedRetrievalMode();
            // 根据模式生成可断言的片段。
            String snippet = retrievalMode.name() + " snippet";
            // 返回测试检索结果。
            return MedicalKnowledgeSearchResult.of(request.getQuery(), retrievalMode, List.of(hit(snippet)));
        };
    }

    // 创建检索请求。
    private static MedicalKnowledgeSearchRequest request(String query, RetrievalMode retrievalMode) {
        // 创建请求对象。
        MedicalKnowledgeSearchRequest request = new MedicalKnowledgeSearchRequest();
        // 设置查询问题。
        request.setQuery(query);
        // 设置检索模式。
        request.setRetrievalMode(retrievalMode);
        // 返回请求对象。
        return request;
    }

    // 创建测试命中。
    private static MedicalKnowledgeSearchHit hit(String snippet) {
        // 创建命中对象。
        MedicalKnowledgeSearchHit hit = new MedicalKnowledgeSearchHit();
        // 设置 chunkId。
        hit.setChunkId("test:doc#chunk-0");
        // 设置文档 ID。
        hit.setDocumentId("test:doc");
        // 设置片段序号。
        hit.setChunkIndex(0);
        // 设置标题。
        hit.setTitle("Test");
        // 设置片段。
        hit.setSnippet(snippet);
        // 设置来源名称。
        hit.setSourceName("TestSource");
        // 设置来源 URL。
        hit.setSourceUrl("https://example.org/test");
        // 设置召回方式。
        hit.setRetrievalSource("TEST");
        // 返回命中。
        return hit;
    }
}
