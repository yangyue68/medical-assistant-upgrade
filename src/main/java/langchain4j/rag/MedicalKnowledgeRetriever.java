// 声明当前接口所在的包。
package langchain4j.rag;

// 定义医学知识检索器接口，为后续 BM25、向量检索、混合检索和重排预留统一边界。
public interface MedicalKnowledgeRetriever {

    // 根据检索请求返回医学知识命中结果。
    MedicalKnowledgeSearchResult search(MedicalKnowledgeSearchRequest request);
}
