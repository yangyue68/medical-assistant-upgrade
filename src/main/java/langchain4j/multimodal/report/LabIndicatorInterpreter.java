// 声明医疗报告结构化与解释功能所在的包。
package langchain4j.multimodal.report;

// 引入列表类型，用于保存整份报告的逐项解释。
import java.util.List;

/** 基于报告自身参考范围生成保守提示，不进行疾病诊断。 */
public class LabIndicatorInterpreter {

    // 将结构化化验报告转换为非诊断性解释结果。
    public LabReportInterpretation interpret(StructuredMedicalReport report) {
        // 解释器必须接收已经完成结构化的报告。
        if (report == null) {
            // 对无效调用返回明确错误。
            throw new IllegalArgumentException("结构化报告不能为空。");
        }
        // 遍历报告中的每个化验指标并生成解释。
        List<LabIndicatorInterpretation> indicators = report.items().stream()
                // 使用单项解释方法处理每个指标。
                .map(this::interpretItem)
                // 收集为不可变语义的结果列表。
                .toList();
        // 组合文件 ID、逐项解释和统一医疗安全提示。
        return new LabReportInterpretation(report.fileId(), indicators,
                // 明确参考范围比较的局限，避免用户将结果当成诊断。
                "结果仅按报告参考范围做高低提示，不能用于诊断。参考范围可能因实验室、年龄、性别和检测方法不同而变化；请结合原始报告咨询医生。若伴明显不适或危急值提示，请及时就医。");
    }

    // 根据单项指标状态生成对应的摘要和行动建议。
    private LabIndicatorInterpretation interpretItem(LabReportItem item) {
        // 先查找该指标通常反映的医学含义。
        String meaning = meaningOf(item.name());
        // 使用穷尽 switch 为四种状态分别生成保守提示。
        return switch (item.status()) {
            // 高于报告上限时只描述比较结果，不判断疾病。
            case HIGH -> new LabIndicatorInterpretation(item.name(), item.status(), meaning,
                    // 提供高值摘要和复核建议。
                    "该结果高于报告所列参考上限。", "建议结合症状、其他指标和医生意见复核，不能仅凭单项结果判断疾病。");
            // 低于报告下限时只描述比较结果，不判断疾病。
            case LOW -> new LabIndicatorInterpretation(item.name(), item.status(), meaning,
                    // 提供低值摘要和复核建议。
                    "该结果低于报告所列参考下限。", "建议结合症状、其他指标和医生意见复核，不能仅凭单项结果判断疾病。");
            // 位于参考范围内时仍提醒不能据此排除所有疾病。
            case NORMAL -> new LabIndicatorInterpretation(item.name(), item.status(), meaning,
                    // 提供范围内摘要和有症状时就医建议。
                    "该结果位于报告所列参考范围内。", "参考范围内不等于排除所有疾病，如有症状仍应咨询医生。");
            // 缺少可比较数据时不做高低猜测。
            case UNKNOWN -> new LabIndicatorInterpretation(item.name(), item.status(), meaning,
                    // 建议回到原始报告或检验科核对。
                    "暂时无法根据报告参考范围判断。", "请核对原始报告并咨询医生或检验科。");
        };
    }

    // 根据常见中文名或缩写返回简短指标含义。
    private String meaningOf(String name) {
        // 去除空格并转为大写，统一处理中英文名称。
        String normalized = name == null ? "" : name.replace(" ", "").toUpperCase();
        // 识别白细胞中文名称或 WBC 缩写。
        if (normalized.contains("白细胞") || normalized.equals("WBC")) {
            // 返回不涉及疾病诊断的基础生理含义。
            return "主要反映外周血白细胞数量，与免疫和炎症反应有关。";
        }
        // 识别血红蛋白中文名称或常见英文缩写。
        if (normalized.contains("血红蛋白") || normalized.equals("HGB") || normalized.equals("HB")) {
            // 返回血红蛋白与携氧能力相关的基础说明。
            return "主要反映血液携氧能力，是血常规中的常用指标。";
        }
        // 识别血小板中文名称或 PLT 缩写。
        if (normalized.contains("血小板") || normalized.equals("PLT")) {
            // 返回血小板与止血凝血过程相关的基础说明。
            return "主要反映血小板数量，与止血和凝血过程有关。";
        }
        // 识别葡萄糖、血糖中文名称或 GLU 缩写。
        if (normalized.contains("葡萄糖") || normalized.contains("血糖") || normalized.equals("GLU")) {
            // 强调采样和空腹条件会影响血糖结果理解。
            return "反映采样时的血糖水平，需结合是否空腹及检测条件理解。";
        }
        // 识别肌酐中文名称或常见英文缩写。
        if (normalized.contains("肌酐") || normalized.equals("CREA") || normalized.equals("CR")) {
            // 强调肌酐需要结合人口学和其他检查信息理解。
            return "是评估肾功能时常用的指标之一，需要结合年龄、性别和其他检查理解。";
        }
        // 识别丙氨酸氨基转移酶中文名称或 ALT 缩写。
        if (normalized.contains("丙氨酸氨基转移酶") || normalized.equals("ALT")) {
            // 返回 ALT 在肝功能检查中的基础定位。
            return "是肝功能检查中的常用酶学指标之一。";
        }
        // 识别天门冬氨酸氨基转移酶中文名称或 AST 缩写。
        if (normalized.contains("天门冬氨酸氨基转移酶") || normalized.equals("AST")) {
            // 返回 AST 在肝功能等检查中的基础定位。
            return "是肝功能等检查中的常用酶学指标之一。";
        }
        // 未命中内置词典时返回中性的上下文依赖说明。
        return "该指标的具体含义需要结合报告类型、检测方法和临床背景解释。";
    }
}
