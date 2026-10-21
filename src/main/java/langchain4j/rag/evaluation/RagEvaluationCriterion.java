// 声明当前枚举所在的包，evaluation 表示它属于 RAG 评测模块。
package langchain4j.rag.evaluation;

// 定义 RAG 回答评测维度枚举。
public enum RagEvaluationCriterion {

    // 回答文本不能为空。
    ANSWER_NOT_BLANK,

    // 回答需要包含可追溯引用。
    CITATION_COVERAGE,

    // 资料不足时需要明确拒绝编造。
    INSUFFICIENT_EVIDENCE_REFUSAL,

    // 医疗回答需要包含安全边界提醒。
    MEDICAL_SAFETY_BOUNDARY
}
