// 声明多模态配置测试所在的包。
package langchain4j.config;

// 引入医疗文档文字提取器类型。
import langchain4j.multimodal.extraction.MedicalDocumentTextExtractor;
// 引入 OCR 服务接口。
import langchain4j.multimodal.extraction.MedicalOcrProvider;
// 引入文件安全扫描接口。
import langchain4j.multimodal.security.MedicalFileSecurityScanner;
// 引入文件存储接口。
import langchain4j.multimodal.storage.MedicalFileStorage;
// 引入文件上传服务。
import langchain4j.multimodal.upload.MedicalFileUploadService;
// 引入报告分析服务。
import langchain4j.multimodal.report.MedicalReportAnalysisService;
// 引入支持 Java 时间模块的 Jackson 构建器。
import com.fasterxml.jackson.databind.json.JsonMapper;
// 引入文件路径类型，用于接收 JUnit 临时目录。
import java.nio.file.Path;
// 引入 JUnit 测试注解。
import org.junit.jupiter.api.Test;
// 引入 JUnit 临时目录注解。
import org.junit.jupiter.api.io.TempDir;

// 静态引入 AssertJ 断言入口。
import static org.assertj.core.api.Assertions.assertThat;

// 验证多模态配置类可以在没有外部 OCR 的环境中完整装配。
class MedicalMultimodalConfigTest {

    // 要求 JUnit 为每次测试创建隔离的临时存储目录。
    @TempDir
    // 保存当前测试使用的临时目录路径。
    Path tempDir;

    // 标记无外部 OCR 场景的 Bean 装配测试。
    @Test
    void shouldAssembleDay51To55ServicesWithoutExternalOcrProvider() {
        // 直接创建待测试的配置类。
        MedicalMultimodalConfig config = new MedicalMultimodalConfig();
        // 使用临时目录和带时间模块的 ObjectMapper 创建本地存储。
        MedicalFileStorage storage = config.medicalFileStorage(
                // 传入临时目录字符串和完整 JsonMapper。
                tempDir.toString(), JsonMapper.builder().findAndAddModules().build());
        // 创建基础文件安全扫描器。
        MedicalFileSecurityScanner scanner = config.medicalFileSecurityScanner();
        // 使用 1 MiB 上限创建上传服务。
        MedicalFileUploadService uploadService = config.medicalFileUploadService(storage, scanner, 1024 * 1024);
        // 在没有自定义 Bean 时创建不可用 OCR 占位实现。
        MedicalOcrProvider ocrProvider = config.medicalOcrProvider();
        // 使用一万字符和一百页限制创建文字提取器。
        MedicalDocumentTextExtractor extractor = config.medicalDocumentTextExtractor(ocrProvider, storage, 10000,
                // 提供 PDF 最大页数参数，覆盖新增的资源限制配置。
                100);
        // 组合上传、提取、解析和解释组件创建分析服务。
        MedicalReportAnalysisService analysisService = config.medicalReportAnalysisService(
                // 复用配置类创建的无状态组件。
                uploadService, extractor, config.medicalReportParser(), config.labIndicatorInterpreter());

        // 验证存储 Bean 创建成功。
        assertThat(storage).isNotNull();
        // 验证上传服务 Bean 创建成功。
        assertThat(uploadService).isNotNull();
        // 验证默认 OCR 实现明确报告不可用。
        assertThat(ocrProvider.available()).isFalse();
        // 验证完整分析服务创建成功。
        assertThat(analysisService).isNotNull();
    }
}
