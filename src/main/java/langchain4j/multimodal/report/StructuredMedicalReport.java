// 声明医疗报告结构化与解释功能所在的包。
package langchain4j.multimodal.report;

// 引入列表类型，用于保存指标、未解析行和告警。
import java.util.List;

/** 从报告文本中抽取出的结构化结果。 */
public record StructuredMedicalReport(
        // 标识结构化结果对应的上传文件。
        String fileId,
        // 保存从文本头部提取的报告名称。
        String reportTitle,
        // 保存报告中的标本类型，例如全血或血清。
        String specimen,
        // 保存成功解析的化验指标列表。
        List<LabReportItem> items,
        // 保存没有匹配规则且不属于个人信息的文本行。
        List<String> unparsedLines,
        // 保存自动解析限制和异常情况提示。
        List<String> warnings
) {
}
