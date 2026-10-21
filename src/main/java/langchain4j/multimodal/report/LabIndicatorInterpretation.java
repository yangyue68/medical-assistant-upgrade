// 声明医疗报告结构化与解释功能所在的包。
package langchain4j.multimodal.report;

/** 单项化验指标的非诊断性解释。 */
public record LabIndicatorInterpretation(
        // 保存被解释的化验指标名称。
        String name,
        // 保存该指标相对报告参考范围的状态。
        LabIndicatorStatus status,
        // 简要说明指标通常反映的生理意义。
        String meaning,
        // 说明本次结果位于参考范围的哪个位置。
        String summary,
        // 给出保守且非诊断性的后续行动建议。
        String action
) {
}
