// 声明当前枚举所在的包。
package langchain4j.rag;

// 定义检索模式枚举，为后续 BM25、向量检索、混合检索预留统一表达。
public enum RetrievalMode {

    // 只使用关键词检索，例如 BM25。
    KEYWORD,

    // 只使用向量检索。
    VECTOR,

    // 同时使用关键词和向量检索，并融合结果。
    HYBRID
}
