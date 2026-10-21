// 声明医疗报告结构化与解释功能所在的包。
package langchain4j.multimodal.report;

// 引入医疗文档文字提取器。
import langchain4j.multimodal.extraction.MedicalDocumentTextExtractor;
// 引入医疗文字提取结果。
import langchain4j.multimodal.extraction.MedicalTextExtractionResult;
// 引入医疗文件处理状态。
import langchain4j.multimodal.file.MedicalFileStatus;
// 引入医疗文件上传和内部查询服务。
import langchain4j.multimodal.upload.MedicalFileUploadService;

/** 串联文件读取、文本提取、报告解析和指标解释。 */
public class MedicalReportAnalysisService {

    // 保存医疗文件上传和内部查询服务。
    private final MedicalFileUploadService uploadService;
    // 保存按文件类型分派的文字提取器。
    private final MedicalDocumentTextExtractor textExtractor;
    // 保存化验报告结构化解析器。
    private final MedicalReportParser reportParser;
    // 保存化验指标保守解释器。
    private final LabIndicatorInterpreter indicatorInterpreter;

    // 创建报告分析服务并注入完整处理链依赖。
    public MedicalReportAnalysisService(MedicalFileUploadService uploadService,
                                        // 接收文字提取器。
                                        MedicalDocumentTextExtractor textExtractor,
                                        // 接收结构化解析器。
                                        MedicalReportParser reportParser,
                                        // 接收指标解释器。
                                        LabIndicatorInterpreter indicatorInterpreter) {
        // 四项依赖缺一不可，避免运行到一半才发生空指针异常。
        if (uploadService == null || textExtractor == null || reportParser == null || indicatorInterpreter == null) {
            // 配置不完整时在 Bean 创建阶段尽早失败。
            throw new IllegalArgumentException("Medical report analysis dependencies must not be null.");
        }
        // 保存上传服务供文件查找使用。
        this.uploadService = uploadService;
        // 保存文字提取器供提取流程使用。
        this.textExtractor = textExtractor;
        // 保存报告解析器供结构化流程使用。
        this.reportParser = reportParser;
        // 保存指标解释器供解释流程使用。
        this.indicatorInterpreter = indicatorInterpreter;
    }

    // 从指定已上传文件中提取文字。
    public MedicalTextExtractionResult extractText(String fileId) {
        // 先取得内部存储对象，再交给文字提取器按类型处理。
        return textExtractor.extract(uploadService.requireStoredFile(fileId));
    }

    // 从指定已上传文件生成结构化化验报告。
    public StructuredMedicalReport structure(String fileId) {
        // 先执行文字提取并取得状态、正文和告警。
        MedicalTextExtractionResult extraction = extractText(fileId);
        // 只有成功得到真实文字时才能继续结构化。
        if (extraction.status() != MedicalFileStatus.TEXT_EXTRACTED) {
            // 将提取器提供的告警合并为可展示的错误信息。
            throw new IllegalArgumentException(String.join(" ", extraction.warnings()));
        }
        // 把真实提取文本交给保守规则解析器。
        return reportParser.parse(fileId, extraction.text());
    }

    // 从指定已上传文件生成非诊断性指标解释。
    public LabReportInterpretation interpret(String fileId) {
        // 复用结构化流程，并把结果交给指标解释器。
        return indicatorInterpreter.interpret(structure(fileId));
    }
}
