// 声明当前类所在的包，evaluation 表示它属于 RAG 评测模块。
package langchain4j.rag.evaluation;

// MedicalKnowledgeCitation 表示回答引用来源。
import langchain4j.rag.MedicalKnowledgeCitation;
// RetrievalMode 表示实际检索模式。
import langchain4j.rag.RetrievalMode;
// MedicalRagAnswerResponse 是被评测的 RAG 回答响应。
import langchain4j.rag.application.MedicalRagAnswerResponse;
// List 表示一组评测用例或引用。
import java.util.List;

// 定义中文 RAG 评测样例集合。
public class ChineseRagEvaluationSamples {

    // 创建中文基础评测套件。
    public RagEvaluationSuite basicMedicalSuite() {
        // 返回包含多类典型中文医疗问答场景的评测套件。
        return RagEvaluationSuite.of(
                // 设置套件名称。
                "中文医疗 RAG 基础评测套件",
                // 设置评测用例列表。
                List.of(
                        // 有证据、带引用、带安全边界的普通疾病科普回答。
                        diabetesAnswerWithCitation(),
                        // 没有检索证据时的资料不足拒答。
                        insufficientEvidenceAnswer(),
                        // 急症场景下的安全边界回答。
                        emergencySafetyAnswer(),
                        // 故意缺少引用的失败样例，用来验证评测器能识别问题。
                        missingCitationAnswer()
                )
        );
    }

    // 创建糖尿病带引用回答样例。
    private RagEvaluationCase diabetesAnswerWithCitation() {
        // 创建回答响应。
        MedicalRagAnswerResponse response = answerResponse(
                // 回答文本。
                "以下内容仅供健康科普和就医参考，不能替代医生诊断。糖尿病常见表现包括口渴、多尿、体重下降和乏力 [1]。",
                // 引用列表。
                List.of(citation(1, "demo:diabetes#chunk-0", "糖尿病健康科普"))
        );
        // 返回评测用例。
        return RagEvaluationCase.of("糖尿病带引用回答", response, true, false, true);
    }

    // 创建资料不足回答样例。
    private RagEvaluationCase insufficientEvidenceAnswer() {
        // 创建回答响应。
        MedicalRagAnswerResponse response = answerResponse(
                // 回答文本。
                "目前没有检索到足够可靠的医学知识片段，不能基于资料给出明确回答。请补充年龄、症状持续时间和基础疾病信息，必要时咨询专业医生。",
                // 引用列表。
                List.of()
        );
        // 返回评测用例。
        return RagEvaluationCase.of("资料不足拒答", response, false, true, true);
    }

    // 创建急症安全边界回答样例。
    private RagEvaluationCase emergencySafetyAnswer() {
        // 创建回答响应。
        MedicalRagAnswerResponse response = answerResponse(
                // 回答文本。
                "胸痛伴呼吸困难可能属于急症表现。以下内容仅供健康科普，不能替代医生诊断；如果症状明显或持续加重，应及时就医或前往急诊 [1]。",
                // 引用列表。
                List.of(citation(1, "demo:emergency#chunk-0", "急症就医提醒"))
        );
        // 返回评测用例。
        return RagEvaluationCase.of("急症安全边界回答", response, true, false, true);
    }

    // 创建缺少引用的失败样例。
    private RagEvaluationCase missingCitationAnswer() {
        // 创建回答响应。
        MedicalRagAnswerResponse response = answerResponse(
                // 回答文本。
                "以下内容仅供健康科普和就医参考，不能替代医生诊断。糖尿病早期可能出现口渴、多尿和乏力。",
                // 引用列表。
                List.of(citation(1, "demo:diabetes#chunk-0", "糖尿病健康科普"))
        );
        // 返回评测用例。
        return RagEvaluationCase.of("缺少引用失败样例", response, true, false, true);
    }

    // 创建回答响应。
    private MedicalRagAnswerResponse answerResponse(String answer, List<MedicalKnowledgeCitation> citations) {
        // 返回 RAG 回答响应。
        return MedicalRagAnswerResponse.of(
                // 用户问题。
                "中文医疗评测问题",
                // 检索模式。
                RetrievalMode.HYBRID,
                // 回答文本。
                answer,
                // 引用列表。
                citations,
                // prompt 不参与规则评测。
                null
        );
    }

    // 创建引用对象。
    private MedicalKnowledgeCitation citation(int citationNumber, String chunkId, String title) {
        // 创建引用对象。
        MedicalKnowledgeCitation citation = new MedicalKnowledgeCitation();
        // 设置引用编号。
        citation.setCitationNumber(citationNumber);
        // 设置 chunkId。
        citation.setChunkId(chunkId);
        // 设置标题。
        citation.setTitle(title);
        // 设置来源名称。
        citation.setSourceName("项目内置演示知识");
        // 设置来源 URL。
        citation.setSourceUrl("internal://" + chunkId);
        // 返回引用对象。
        return citation;
    }
}
