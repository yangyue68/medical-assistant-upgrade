// 声明医疗报告结构化与解释功能所在的包。
package langchain4j.multimodal.report;

/** 化验指标相对参考范围的状态。 */
public enum LabIndicatorStatus {
    // 指标值低于报告提供的参考下限。
    LOW,
    // 指标值位于报告提供的参考范围内。
    NORMAL,
    // 指标值高于报告提供的参考上限。
    HIGH,
    // 缺少数值或参考范围，暂时无法比较。
    UNKNOWN
}
