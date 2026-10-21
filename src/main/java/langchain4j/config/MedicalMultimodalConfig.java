// 声明应用配置组件所在的包。
package langchain4j.config;

// 引入医疗文档文字提取器。
import langchain4j.multimodal.extraction.MedicalDocumentTextExtractor;
import langchain4j.multimodal.extraction.DashScopeMedicalOcrProvider;
import langchain4j.multimodal.extraction.PaddleOcrHttpProvider;
// 引入可替换的 OCR 服务接口。
import langchain4j.multimodal.extraction.MedicalOcrProvider;
// 引入 OCR 未配置时的占位实现。
import langchain4j.multimodal.extraction.UnavailableMedicalOcrProvider;
// 引入化验指标解释器。
import langchain4j.multimodal.report.LabIndicatorInterpreter;
// 引入医疗报告分析编排服务。
import langchain4j.multimodal.report.MedicalReportAnalysisService;
// 引入化验报告结构化解析器。
import langchain4j.multimodal.report.MedicalReportParser;
// 引入本地基础文件安全扫描实现。
import langchain4j.multimodal.security.BasicMedicalFileSecurityScanner;
// 引入可替换的文件安全扫描接口。
import langchain4j.multimodal.security.MedicalFileSecurityScanner;
// 引入本地私有文件存储实现。
import langchain4j.multimodal.storage.LocalMedicalFileStorage;
// 引入可替换的文件存储接口。
import langchain4j.multimodal.storage.MedicalFileStorage;
// 引入医疗文件上传编排服务。
import langchain4j.multimodal.upload.MedicalFileUploadService;
// 引入 Jackson 对象映射器，用于读写元数据 JSON。
import com.fasterxml.jackson.databind.ObjectMapper;
// 引入配置值注入注解。
import org.springframework.beans.factory.annotation.Value;
// 引入缺失 Bean 条件注解，允许后续替换真实 OCR 实现。
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// 引入 Bean 工厂方法注解。
import org.springframework.context.annotation.Bean;
// 引入配置类注解。
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

/** 第 51-55 天多模态文件与报告处理 Bean 配置。 */
// 声明该类用于向 Spring 容器注册多模态模块 Bean。
@Configuration
public class MedicalMultimodalConfig {

    @Bean
    @ConditionalOnProperty(name = "xiaozhi.multimodal.ocr.provider", havingValue = "paddle", matchIfMissing = true)
    public MedicalOcrProvider paddleMedicalOcrProvider(
            @Value("${xiaozhi.multimodal.ocr.base-url:http://127.0.0.1:18080}") String baseUrl,
            @Value("${xiaozhi.multimodal.ocr.connect-timeout-seconds:5}") long connectTimeoutSeconds,
            @Value("${xiaozhi.multimodal.ocr.read-timeout-seconds:60}") long readTimeoutSeconds
    ) {
        return new PaddleOcrHttpProvider(baseUrl, Duration.ofSeconds(connectTimeoutSeconds),
                Duration.ofSeconds(readTimeoutSeconds));
    }

    @Bean
    @ConditionalOnProperty(name = "xiaozhi.multimodal.ocr.provider", havingValue = "dashscope")
    public MedicalOcrProvider dashScopeMedicalOcrProvider(
            @Value("${xiaozhi.multimodal.ocr.endpoint}") String endpoint,
            @Value("${xiaozhi.multimodal.ocr.api-key}") String apiKey,
            @Value("${xiaozhi.multimodal.ocr.model-name:qwen3.5-ocr}") String modelName,
            @Value("${xiaozhi.multimodal.ocr.connect-timeout-seconds:10}") long connectTimeoutSeconds,
            @Value("${xiaozhi.multimodal.ocr.read-timeout-seconds:60}") long readTimeoutSeconds
    ) {
        return new DashScopeMedicalOcrProvider(endpoint, apiKey, modelName,
                Duration.ofSeconds(connectTimeoutSeconds), Duration.ofSeconds(readTimeoutSeconds));
    }

    // 注册医疗文件存储接口的本地实现。
    @Bean
    public MedicalFileStorage medicalFileStorage(
            // 从配置读取私有存储目录，并提供开发环境默认值。
            @Value("${xiaozhi.multimodal.storage.directory:data/uploads/medical}") String directory,
            // 注入应用统一配置的 Jackson 对象映射器。
            ObjectMapper objectMapper
    ) {
        // 创建使用指定目录和 JSON 映射器的本地存储实现。
        return new LocalMedicalFileStorage(directory, objectMapper);
    }

    // 注册基础文件安全扫描器。
    @Bean
    public MedicalFileSecurityScanner medicalFileSecurityScanner() {
        // 返回无需外部服务即可工作的安全扫描实现。
        return new BasicMedicalFileSecurityScanner();
    }

    // 注册上传校验、哈希和持久化编排服务。
    @Bean
    public MedicalFileUploadService medicalFileUploadService(
            // 注入上面注册的文件存储实现。
            MedicalFileStorage storage,
            // 注入上面注册的安全扫描实现。
            MedicalFileSecurityScanner securityScanner,
            // 从配置读取应用层单文件最大字节数。
            @Value("${xiaozhi.multimodal.upload.max-bytes:10485760}") long maxBytes
    ) {
        // 使用存储、安全扫描和大小限制创建上传服务。
        return new MedicalFileUploadService(storage, securityScanner, maxBytes);
    }

    // 注册默认 OCR Bean。
    @Bean
    // 仅在项目没有提供真实 OCR Bean 时启用占位实现。
    @ConditionalOnMissingBean(MedicalOcrProvider.class)
    public MedicalOcrProvider medicalOcrProvider() {
        // 返回明确报告不可用的 OCR 实现，避免伪造识别结果。
        return new UnavailableMedicalOcrProvider();
    }

    // 注册按文件类型分派的文字提取器。
    @Bean
    public MedicalDocumentTextExtractor medicalDocumentTextExtractor(
            // 注入真实或占位的 OCR 实现。
            MedicalOcrProvider ocrProvider,
            // 注入存储接口，用于持久化文件处理状态。
            MedicalFileStorage storage,
            // 从配置读取单次提取允许返回的最大字符数。
            @Value("${xiaozhi.multimodal.extraction.max-characters:100000}") int maxCharacters,
            // 从配置读取单个 PDF 允许解析的最大页数。
            @Value("${xiaozhi.multimodal.extraction.max-pdf-pages:100}") int maxPdfPages
    ) {
        // 使用 OCR、存储和两个资源限制创建文字提取器。
        return new MedicalDocumentTextExtractor(ocrProvider, storage, maxCharacters, maxPdfPages);
    }

    // 注册化验报告结构化解析器。
    @Bean
    public MedicalReportParser medicalReportParser() {
        // 创建无状态的本地规则解析器。
        return new MedicalReportParser();
    }

    // 注册化验指标解释器。
    @Bean
    public LabIndicatorInterpreter labIndicatorInterpreter() {
        // 创建无状态的保守指标解释器。
        return new LabIndicatorInterpreter();
    }

    // 注册串联上传、提取、解析和解释能力的应用服务。
    @Bean
    public MedicalReportAnalysisService medicalReportAnalysisService(
            // 注入文件上传和查询服务。
            MedicalFileUploadService uploadService,
            // 注入文档文字提取器。
            MedicalDocumentTextExtractor textExtractor,
            // 注入报告结构化解析器。
            MedicalReportParser reportParser,
            // 注入指标解释器。
            LabIndicatorInterpreter indicatorInterpreter
    ) {
        // 组合四个独立组件并返回统一分析服务。
        return new MedicalReportAnalysisService(uploadService, textExtractor, reportParser, indicatorInterpreter);
    }
}
