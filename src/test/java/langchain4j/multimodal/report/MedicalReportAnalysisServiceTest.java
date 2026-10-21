// 声明医疗报告完整流水线测试所在的包。
package langchain4j.multimodal.report;

// 引入医疗文档文字提取器。
import langchain4j.multimodal.extraction.MedicalDocumentTextExtractor;
// 引入医疗文字提取结果。
import langchain4j.multimodal.extraction.MedicalTextExtractionResult;
// 引入 OCR 未配置时的占位实现。
import langchain4j.multimodal.extraction.UnavailableMedicalOcrProvider;
// 引入医疗文件业务分类。
import langchain4j.multimodal.file.MedicalFileCategory;
// 引入医疗文件处理状态。
import langchain4j.multimodal.file.MedicalFileStatus;
// 引入可安全返回的上传文件元数据。
import langchain4j.multimodal.file.UploadedMedicalFile;
// 引入真实基础文件安全扫描器。
import langchain4j.multimodal.security.BasicMedicalFileSecurityScanner;
// 引入本地私有文件存储实现。
import langchain4j.multimodal.storage.LocalMedicalFileStorage;
// 引入医疗文件上传服务。
import langchain4j.multimodal.upload.MedicalFileUploadService;
// 引入支持 Java 时间模块的 Jackson 构建器。
import com.fasterxml.jackson.databind.json.JsonMapper;
// 引入 UTF-8 字符集，用于构造中文上传正文。
import java.nio.charset.StandardCharsets;
// 引入文件路径类型，用于接收临时目录。
import java.nio.file.Path;
// 引入 JUnit 测试注解。
import org.junit.jupiter.api.Test;
// 引入 JUnit 临时目录注解。
import org.junit.jupiter.api.io.TempDir;
// 引入 Spring 模拟上传文件类型。
import org.springframework.mock.web.MockMultipartFile;

// 静态引入 AssertJ 断言入口。
import static org.assertj.core.api.Assertions.assertThat;

// 验证上传、提取、结构化、解释和删除组成的完整本地流水线。
class MedicalReportAnalysisServiceTest {

    // 要求 JUnit 为测试创建隔离的私有存储目录。
    @TempDir
    // 保存当前测试使用的临时目录。
    Path tempDir;

    // 标记纯文本化验报告端到端处理测试。
    @Test
    void shouldCompleteTextLabReportPipelineAndDeletePrivateFile() {
        // 使用临时目录创建本地文件存储。
        LocalMedicalFileStorage storage = new LocalMedicalFileStorage(
                // 使用支持时间类型的 JsonMapper 持久化元数据。
                tempDir.toString(), JsonMapper.builder().findAndAddModules().build());
        // 使用基础扫描器和 1 MiB 限制创建上传服务。
        MedicalFileUploadService uploadService = new MedicalFileUploadService(
                // 注入本地存储、安全扫描器和文件大小上限。
                storage, new BasicMedicalFileSecurityScanner(), 1024 * 1024);
        // 使用不可用 OCR 占位实现创建文字提取器。
        MedicalDocumentTextExtractor extractor = new MedicalDocumentTextExtractor(
                // 为文本流程设置一万字符和一百页限制。
                new UnavailableMedicalOcrProvider(), storage, 10000, 100);
        // 组合上传、提取、解析和解释组件创建分析服务。
        MedicalReportAnalysisService analysisService = new MedicalReportAnalysisService(
                // 注入完整的本地处理链依赖。
                uploadService, extractor, new MedicalReportParser(), new LabIndicatorInterpreter());
        // 构造包含一项高值和一项低值的化验报告文本。
        String content = """
                报告名称：血常规
                标本：静脉血
                白细胞 12.5 10^9/L 3.5-9.5
                血红蛋白 98 g/L 115-150
                """;
        // 将报告文本包装为 UTF-8 multipart 文件。
        MockMultipartFile file = new MockMultipartFile(
                // 设置字段名、文件名、MIME 类型和正文。
                "file", "report.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));

        // 执行安全上传并取得随机文件 ID。
        UploadedMedicalFile uploaded = uploadService.upload(file, MedicalFileCategory.LAB_REPORT);
        // 执行本地 UTF-8 文字提取。
        MedicalTextExtractionResult extraction = analysisService.extractText(uploaded.getFileId());
        // 执行化验指标结构化解析。
        StructuredMedicalReport structured = analysisService.structure(uploaded.getFileId());
        // 执行非诊断性指标解释。
        LabReportInterpretation interpretation = analysisService.interpret(uploaded.getFileId());

        // 验证提取结果包含两项中文指标。
        assertThat(extraction.text()).contains("白细胞", "血红蛋白");
        // 提取结构化指标的状态。
        assertThat(structured.items()).extracting(LabReportItem::status)
                // 验证状态顺序为高值和低值。
                .containsExactly(LabIndicatorStatus.HIGH, LabIndicatorStatus.LOW);
        // 验证两项指标都生成解释。
        assertThat(interpretation.indicators()).hasSize(2);
        // 验证统一医疗安全提示明确不能用于诊断。
        assertThat(interpretation.safetyNotice()).contains("不能用于诊断");
        // 查询持久化元数据中的最新处理状态。
        assertThat(uploadService.find(uploaded.getFileId()).getStatus())
                // 验证状态已持久化为文字提取成功。
                .isEqualTo(MedicalFileStatus.TEXT_EXTRACTED);
        // 验证删除接口实际删除正文和元数据。
        assertThat(uploadService.delete(uploaded.getFileId())).isTrue();
        // 验证存储层已无法查询到删除后的文件。
        assertThat(storage.find(uploaded.getFileId())).isEmpty();
    }
}
