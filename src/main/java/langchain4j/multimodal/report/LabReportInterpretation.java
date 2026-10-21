// 声明医疗报告结构化与解释功能所在的包。
package langchain4j.multimodal.report;

// 引入列表类型，用于承载多项指标解释。
import java.util.List;

/** 化验单解释响应。 */
public record LabReportInterpretation(
        // 标识解释结果对应的上传文件。
        String fileId,
        // 保存报告中每一项化验指标的解释。
        List<LabIndicatorInterpretation> indicators,
        // 保存面向用户的统一医疗安全提示。
        String safetyNotice
) {
}
