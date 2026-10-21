// 声明医疗报告结构化与解释功能所在的包。
package langchain4j.multimodal.report;

/** 化验单中的单项结构化指标。 */
public record LabReportItem(
        // 保存化验项目名称，例如白细胞计数。
        String name,
        // 保存报告中的原始结果字符串，保留大于号或小于号。
        String value,
        // 保存报告中的计量单位。
        String unit,
        // 保存便于前端展示的原始参考范围。
        String referenceRange,
        // 保存可用于数值比较的结果值。
        Double numericValue,
        // 保存参考范围下限。
        Double referenceLow,
        // 保存参考范围上限。
        Double referenceHigh,
        // 保存结果与参考范围比较后的状态。
        LabIndicatorStatus status
) {
}
