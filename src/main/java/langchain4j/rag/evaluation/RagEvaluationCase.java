// 声明当前类所在的包，evaluation 表示它属于 RAG 评测模块。
package langchain4j.rag.evaluation;

// MedicalRagAnswerResponse 是被评测的 RAG 回答结果。
import langchain4j.rag.application.MedicalRagAnswerResponse;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义一条 RAG 回答评测用例。
public class RagEvaluationCase {

    // 评测用例名称，方便日志和测试报告定位。
    private String caseName;

    // 被评测的 RAG 回答响应。
    private MedicalRagAnswerResponse answerResponse;

    // 是否要求回答必须带引用。
    private boolean requireCitation;

    // 是否期望回答表达资料不足。
    private boolean expectInsufficientEvidenceAnswer;

    // 是否要求回答包含医疗安全边界。
    private boolean requireSafetyBoundary;

    // 创建评测用例。
    public static RagEvaluationCase of(
            // 评测用例名称。
            String caseName,
            // 被评测的回答响应。
            MedicalRagAnswerResponse answerResponse,
            // 是否要求引用。
            boolean requireCitation,
            // 是否期望资料不足回答。
            boolean expectInsufficientEvidenceAnswer,
            // 是否要求医疗安全边界。
            boolean requireSafetyBoundary
    ) {
        // 创建评测用例对象。
        RagEvaluationCase evaluationCase = new RagEvaluationCase();
        // 设置用例名称。
        evaluationCase.setCaseName(caseName);
        // 设置被评测回答。
        evaluationCase.setAnswerResponse(answerResponse);
        // 设置是否要求引用。
        evaluationCase.setRequireCitation(requireCitation);
        // 设置是否期望资料不足回答。
        evaluationCase.setExpectInsufficientEvidenceAnswer(expectInsufficientEvidenceAnswer);
        // 设置是否要求医疗安全边界。
        evaluationCase.setRequireSafetyBoundary(requireSafetyBoundary);
        // 返回评测用例。
        return evaluationCase;
    }
}
