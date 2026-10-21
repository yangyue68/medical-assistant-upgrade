// 声明医疗报告解析测试所在的包。
package langchain4j.multimodal.report;

// 引入 JUnit 测试注解。
import org.junit.jupiter.api.Test;

// 静态引入 AssertJ 断言入口。
import static org.assertj.core.api.Assertions.assertThat;

// 验证纯文本化验单结构化和保守解释行为。
class MedicalReportParserTest {

    // 标记化验指标提取和高低状态判断测试。
    @Test
    void shouldExtractLabItemsAndDetermineReferenceRangeStatus() {
        // 构造包含标题、标本、个人信息和三项指标的报告文本。
        String reportText = """
                报告名称：血常规
                标本：静脉血
                姓名：测试患者
                白细胞 12.5 10^9/L 3.5-9.5
                血红蛋白 98 g/L 115-150
                血小板 230 10^9/L 125-350
                """;

        // 调用待测试解析器生成结构化报告。
        StructuredMedicalReport report = new MedicalReportParser().parse("file-1", reportText);

        // 验证报告标题提取正确。
        assertThat(report.reportTitle()).isEqualTo("血常规");
        // 验证标本类型提取正确。
        assertThat(report.specimen()).isEqualTo("静脉血");
        // 验证三项数值指标全部成功解析。
        assertThat(report.items()).hasSize(3);
        // 提取每个指标的参考范围状态。
        assertThat(report.items()).extracting(LabReportItem::status)
                // 验证高、低、正常状态与输入值一致。
                .containsExactly(LabIndicatorStatus.HIGH, LabIndicatorStatus.LOW, LabIndicatorStatus.NORMAL);
        // 验证包含姓名的个人信息没有进入未解析行响应。
        assertThat(report.unparsedLines()).noneMatch(line -> line.contains("测试患者"));
    }

    // 标记非诊断性解释和医疗安全提示测试。
    @Test
    void shouldGenerateConservativeNonDiagnosticInterpretation() {
        // 解析一项高值和一项低值化验指标。
        StructuredMedicalReport report = new MedicalReportParser().parse("file-1",
                // 使用最小文本覆盖两种异常状态。
                "白细胞 12.5 10^9/L 3.5-9.5\n血红蛋白 98 g/L 115-150");

        // 调用指标解释器生成整份报告解释。
        LabReportInterpretation interpretation = new LabIndicatorInterpreter().interpret(report);

        // 验证两项指标都得到解释。
        assertThat(interpretation.indicators()).hasSize(2);
        // 验证第一项摘要只说明高于参考范围。
        assertThat(interpretation.indicators().get(0).summary()).contains("高于");
        // 验证第二项摘要只说明低于参考范围。
        assertThat(interpretation.indicators().get(1).summary()).contains("低于");
        // 验证统一提示明确禁止诊断并建议咨询医生。
        assertThat(interpretation.safetyNotice()).contains("不能用于诊断", "咨询医生");
    }
}
