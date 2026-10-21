// 声明当前类所在的包，application 表示它属于 RAG 应用编排层。
package langchain4j.rag.application;

// MedicalKnowledgeRetriever 是统一医学知识检索接口。
import langchain4j.rag.MedicalKnowledgeRetriever;
// MedicalKnowledgeSearchRequest 表示检索请求。
import langchain4j.rag.MedicalKnowledgeSearchRequest;
// MedicalKnowledgeSearchResult 表示检索结果。
import langchain4j.rag.MedicalKnowledgeSearchResult;
import langchain4j.rag.rerank.MedicalKnowledgeReranker;
import langchain4j.rag.rerank.NoOpMedicalKnowledgeReranker;
// List 表示一组校验错误。
import java.util.List;

// 定义医学 RAG 应用服务，负责选择检索器并组装模型上下文。
public class MedicalRagService {

    // 医学知识检索器；当前由 Elasticsearch 同时承接关键词、向量和混合检索。
    private final MedicalKnowledgeRetriever retriever;

    // RAG 上下文构建器。
    private final MedicalRagContextBuilder contextBuilder;

    // RAG prompt 构建器。
    private final MedicalRagPromptBuilder promptBuilder;

    // RAG 回答生成器。
    private final MedicalRagAnswerGenerator answerGenerator;

    private final MedicalKnowledgeReranker reranker;

    // 创建医学 RAG 应用服务。
    public MedicalRagService(
            // 医学知识检索器。
            MedicalKnowledgeRetriever retriever,
            // RAG 上下文构建器。
            MedicalRagContextBuilder contextBuilder,
            // RAG prompt 构建器。
            MedicalRagPromptBuilder promptBuilder,
            // RAG 回答生成器。
            MedicalRagAnswerGenerator answerGenerator
    ) {
        this(retriever, contextBuilder, promptBuilder, answerGenerator, new NoOpMedicalKnowledgeReranker());
    }

    public MedicalRagService(
            MedicalKnowledgeRetriever retriever,
            MedicalRagContextBuilder contextBuilder,
            MedicalRagPromptBuilder promptBuilder,
            MedicalRagAnswerGenerator answerGenerator,
            MedicalKnowledgeReranker reranker
    ) {
        // 如果检索器为空，就无法支持 RAG 检索。
        if (retriever == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("retriever must not be null.");
        }
        // 如果上下文构建器为空，就无法构建模型上下文。
        if (contextBuilder == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("contextBuilder must not be null.");
        }
        // 如果 prompt 构建器为空，就无法组装模型输入。
        if (promptBuilder == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("promptBuilder must not be null.");
        }
        // 如果回答生成器为空，就无法生成最终回答。
        if (answerGenerator == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("answerGenerator must not be null.");
        }
        if (reranker == null) {
            throw new IllegalArgumentException("reranker must not be null.");
        }
        // 保存统一检索器。
        this.retriever = retriever;
        // 保存上下文构建器。
        this.contextBuilder = contextBuilder;
        // 保存 prompt 构建器。
        this.promptBuilder = promptBuilder;
        // 保存回答生成器。
        this.answerGenerator = answerGenerator;
        this.reranker = reranker;
    }

    // 根据检索请求准备模型可用的 RAG 上下文。
    public MedicalRagContext prepareContext(MedicalKnowledgeSearchRequest request) {
        // 校验检索请求。
        validateRequest(request);
        // 执行检索。
        MedicalKnowledgeSearchResult searchResult = retriever.search(retrievalRequest(request));
        if (reranker.isEnabled()) {
            searchResult = MedicalKnowledgeSearchResult.of(
                    request.getQuery(),
                    searchResult.getRetrievalMode(),
                    reranker.rerank(request.getQuery(), searchResult.getHits(), request.normalizedTopK())
            );
        }
        // 把检索结果转换成模型上下文。
        return contextBuilder.build(searchResult);
    }

    private MedicalKnowledgeSearchRequest retrievalRequest(MedicalKnowledgeSearchRequest request) {
        if (!reranker.isEnabled()) {
            return request;
        }
        MedicalKnowledgeSearchRequest expanded = new MedicalKnowledgeSearchRequest();
        expanded.setQuery(request.getQuery());
        expanded.setTopK(Math.min(request.normalizedTopK() * 3, 20));
        expanded.setRetrievalMode(request.getRetrievalMode());
        expanded.setLanguage(request.getLanguage());
        expanded.setDepartment(request.getDepartment());
        expanded.setDocumentTypes(request.getDocumentTypes());
        return expanded;
    }

    // 根据 RAG 回答请求生成带引用的回答。
    public MedicalRagAnswerResponse answer(MedicalRagAnswerRequest request) {
        // 校验回答请求。
        validateAnswerRequest(request);
        // 准备 RAG 上下文。
        MedicalRagContext context = prepareContext(request.getSearchRequest());
        // 根据上下文构建 prompt。
        String prompt = promptBuilder.build(context);
        // 调用回答生成器生成答案。
        String answer = answerGenerator.generate(prompt, context);
        // 如果请求明确要求返回 prompt，就保留 prompt；否则返回 null。
        String visiblePrompt = request.includePrompt() ? prompt : null;
        // 创建回答响应。
        return MedicalRagAnswerResponse.of(
                // 用户问题。
                context.getQuery(),
                // 实际检索模式。
                context.getRetrievalMode(),
                // 生成回答。
                answer,
                // 引用列表。
                context.getCitations(),
                // 可选 prompt。
                visiblePrompt
        );
    }

    // 校验检索请求。
    private void validateRequest(MedicalKnowledgeSearchRequest request) {
        // 如果请求为空，说明调用方错误。
        if (request == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("Medical knowledge search request must not be null.");
        }
        // 读取请求校验错误。
        List<String> errors = request.validate();
        // 如果存在错误，就拒绝继续处理。
        if (!errors.isEmpty()) {
            // 把错误信息拼接后抛出。
            throw new IllegalArgumentException(String.join(" ", errors));
        }
    }

    // 校验 RAG 回答请求。
    private void validateAnswerRequest(MedicalRagAnswerRequest request) {
        // 如果回答请求为空，说明调用方错误。
        if (request == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("Medical RAG answer request must not be null.");
        }
        // 校验内部检索请求。
        validateRequest(request.getSearchRequest());
    }
}
