// 声明当前类所在的包，elasticsearch 表示它属于 Elasticsearch 检索实现。
package langchain4j.rag.elasticsearch;

// MedicalKnowledgeRetriever 是项目统一医学知识检索接口。
import langchain4j.rag.MedicalKnowledgeRetriever;
// MedicalKnowledgeSearchHit 是统一检索命中对象。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// MedicalKnowledgeSearchRequest 是统一检索请求对象。
import langchain4j.rag.MedicalKnowledgeSearchRequest;
// MedicalKnowledgeSearchResult 是统一检索结果对象。
import langchain4j.rag.MedicalKnowledgeSearchResult;
// RetrievalMode 表示关键词、向量、混合三种检索模式。
import langchain4j.rag.RetrievalMode;
// MedicalKnowledgeEmbeddingModel 把用户问题转换成查询向量。
import langchain4j.rag.embedding.MedicalKnowledgeEmbeddingModel;
// List 表示一组校验错误或命中结果。
import java.util.List;

// 定义基于 Elasticsearch 的医学知识检索器，统一承接关键词、向量和混合检索。
public class ElasticsearchMedicalKnowledgeRetriever implements MedicalKnowledgeRetriever {

    // 保存 Elasticsearch 客户端。
    private final ElasticsearchMedicalKnowledgeClient client;

    // 保存 embedding 模型。
    private final MedicalKnowledgeEmbeddingModel embeddingModel;

    // 创建 Elasticsearch 检索器。
    public ElasticsearchMedicalKnowledgeRetriever(
            // Elasticsearch 客户端。
            ElasticsearchMedicalKnowledgeClient client,
            // embedding 模型。
            MedicalKnowledgeEmbeddingModel embeddingModel
    ) {
        // Elasticsearch 客户端不能为空。
        if (client == null) {
            // 抛出调用错误。
            throw new IllegalArgumentException("client must not be null.");
        }
        // embedding 模型不能为空，因为 VECTOR 和 HYBRID 都依赖它。
        if (embeddingModel == null) {
            // 抛出调用错误。
            throw new IllegalArgumentException("embeddingModel must not be null.");
        }
        // 保存 Elasticsearch 客户端。
        this.client = client;
        // 保存 embedding 模型。
        this.embeddingModel = embeddingModel;
    }

    // 执行医学知识检索。
    @Override
    public MedicalKnowledgeSearchResult search(MedicalKnowledgeSearchRequest request) {
        // 校验请求。
        validateRequest(request);
        // 读取规整后的检索模式。
        RetrievalMode retrievalMode = request.normalizedRetrievalMode();
        // KEYWORD 模式不需要向量。
        double[] queryVector = retrievalMode == RetrievalMode.KEYWORD ? null : embeddingModel.embed(request.getQuery());
        // 调用 Elasticsearch 检索。
        List<MedicalKnowledgeSearchHit> hits = client.search(request, retrievalMode, queryVector);
        // 返回统一检索结果。
        return MedicalKnowledgeSearchResult.of(request.getQuery(), retrievalMode, hits);
    }

    // 校验检索请求。
    private void validateRequest(MedicalKnowledgeSearchRequest request) {
        // 如果请求为空，说明调用方错误。
        if (request == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("Medical knowledge search request must not be null.");
        }
        // 读取请求自带的校验结果。
        List<String> errors = request.validate();
        // 如果存在校验错误，就拒绝检索。
        if (!errors.isEmpty()) {
            // 拼接错误并抛出。
            throw new IllegalArgumentException(String.join(" ", errors));
        }
    }
}
