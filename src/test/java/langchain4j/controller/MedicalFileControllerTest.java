// 声明医疗文件控制器测试所在的包。
package langchain4j.controller;

// 引入医疗文件业务分类枚举。
import langchain4j.multimodal.file.MedicalFileCategory;
// 引入医疗文件处理状态枚举。
import langchain4j.multimodal.file.MedicalFileStatus;
// 引入接口返回的安全文件元数据类型。
import langchain4j.multimodal.file.UploadedMedicalFile;
// 引入待模拟的上传服务。
import langchain4j.multimodal.upload.MedicalFileUploadService;
// 引入待模拟的报告分析服务。
import langchain4j.multimodal.report.MedicalReportAnalysisService;
import langchain4j.multimodal.extraction.MedicalTextExtractionResult;
// 引入时间戳类型，用于构造固定测试元数据。
import java.time.Instant;
// 引入 UTF-8 字符集，用于构造中文上传正文。
import java.nio.charset.StandardCharsets;
// 引入 JUnit 测试注解。
import org.junit.jupiter.api.Test;
// 引入 Spring 模拟上传文件类型。
import org.springframework.mock.web.MockMultipartFile;
// 引入 Spring MVC 测试客户端。
import org.springframework.test.web.servlet.MockMvc;
// 引入独立控制器测试构建器。
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// 静态引入 AssertJ 断言入口。
import static org.assertj.core.api.Assertions.assertThat;
// 静态引入 Mockito 任意参数匹配器。
import static org.mockito.ArgumentMatchers.any;
// 静态引入 Mockito 精确参数匹配器。
import static org.mockito.ArgumentMatchers.eq;
// 静态引入 Mockito 模拟对象工厂。
import static org.mockito.Mockito.mock;
// 静态引入 Mockito 调用验证工具。
import static org.mockito.Mockito.verify;
// 静态引入 Mockito 行为配置工具。
import static org.mockito.Mockito.when;
// 静态引入 multipart 请求构建器。
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// 静态引入 JSON 路径响应断言。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// 静态引入 HTTP 状态响应断言。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 验证医疗文件 Controller 的参数传递和响应隐私边界。
class MedicalFileControllerTest {

    @Test
    void shouldBindFileIdForAnalysisEndpointsThroughSpringMvc() throws Exception {
        MedicalFileUploadService uploadService = mock(MedicalFileUploadService.class);
        MedicalReportAnalysisService analysisService = mock(MedicalReportAnalysisService.class);
        when(analysisService.extractText("file-1")).thenReturn(new MedicalTextExtractionResult(
                "file-1", "paddleocr-http", MedicalFileStatus.TEXT_EXTRACTED, "白细胞 12.4 10^9/L 3.5-9.5",
                java.util.List.of()));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new MedicalFileController(uploadService, analysisService))
                .build();

        mockMvc.perform(post("/xiaozhi/medical-files/{fileId}/extract-text", "file-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.fileId").value("file-1"))
                .andExpect(jsonPath("$.data.provider").value("paddleocr-http"));

        verify(analysisService).extractText("file-1");
    }

    // 标记直接调用 Controller 的上传元数据测试。
    @Test
    void shouldUploadMedicalFileAndReturnOnlySafeMetadata() {
        // 创建上传服务模拟对象，避免测试触碰真实磁盘。
        MedicalFileUploadService uploadService = mock(MedicalFileUploadService.class);
        // 创建分析服务模拟对象，满足控制器构造依赖。
        MedicalReportAnalysisService analysisService = mock(MedicalReportAnalysisService.class);
        // 使用两个模拟服务创建待测试控制器。
        MedicalFileController controller = new MedicalFileController(uploadService, analysisService);
        // 构造一个最小纯文本 multipart 文件。
        MockMultipartFile file = new MockMultipartFile("file", "report.txt", "text/plain", "test".getBytes());
        // 创建固定的安全文件元数据。
        UploadedMedicalFile metadata = metadata();
        // 配置上传服务在指定分类下返回固定元数据。
        when(uploadService.upload(any(), eq(MedicalFileCategory.LAB_REPORT))).thenReturn(metadata);

        // 直接调用上传接口并取得统一响应中的数据。
        UploadedMedicalFile response = controller.upload(file, MedicalFileCategory.LAB_REPORT).getData();

        // 验证响应返回预期随机文件 ID。
        assertThat(response.getFileId()).isEqualTo("file-1");
        // 验证响应保留安全化后的展示文件名。
        assertThat(response.getOriginalFilename()).isEqualTo("report.txt");
        // 验证控制器把文件和业务分类正确传给服务层。
        verify(uploadService).upload(file, MedicalFileCategory.LAB_REPORT);
    }

    // 标记 Spring MVC multipart 参数绑定和 JSON 响应测试。
    @Test
    void shouldBindMultipartUploadRequestThroughSpringMvc() throws Exception {
        // 创建上传服务模拟对象。
        MedicalFileUploadService uploadService = mock(MedicalFileUploadService.class);
        // 创建分析服务模拟对象。
        MedicalReportAnalysisService analysisService = mock(MedicalReportAnalysisService.class);
        // 创建待测试控制器。
        MedicalFileController controller = new MedicalFileController(uploadService, analysisService);
        // 只注册当前控制器，避免启动完整 Spring 上下文。
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // 构造包含中文化验内容的 UTF-8 multipart 文件。
        MockMultipartFile file = new MockMultipartFile(
                // 设置表单字段名、文件名和 MIME 类型。
                "file", "report.txt", "text/plain",
                // 设置文件正文并显式使用 UTF-8 编码。
                "白细胞 12.5 10^9/L 3.5-9.5".getBytes(StandardCharsets.UTF_8)
        );
        // 配置上传服务返回固定安全元数据。
        when(uploadService.upload(any(), eq(MedicalFileCategory.LAB_REPORT))).thenReturn(metadata());

        // 向真实控制器路径发送 multipart 请求。
        mockMvc.perform(multipart("/xiaozhi/medical-files")
                        // 添加文件字段。
                        .file(file)
                        // 添加业务分类字段。
                        .param("category", "LAB_REPORT"))
                // 验证请求处理成功。
                .andExpect(status().isOk())
                // 验证统一响应成功码。
                .andExpect(jsonPath("$.code").value(0))
                // 验证响应包含安全文件 ID。
                .andExpect(jsonPath("$.data.fileId").value("file-1"))
                // 验证响应包含展示文件名。
                .andExpect(jsonPath("$.data.originalFilename").value("report.txt"))
                // 验证内部磁盘路径没有序列化到响应。
                .andExpect(jsonPath("$.data.path").doesNotExist());
    }

    // 构造多个测试复用的固定安全元数据。
    private UploadedMedicalFile metadata() {
        // 创建空元数据对象。
        UploadedMedicalFile metadata = new UploadedMedicalFile();
        // 设置固定文件 ID。
        metadata.setFileId("file-1");
        // 设置固定展示文件名。
        metadata.setOriginalFilename("report.txt");
        // 设置固定 MIME 类型。
        metadata.setContentType("text/plain");
        // 设置固定扩展名。
        metadata.setExtension(".txt");
        // 设置固定文件大小。
        metadata.setSize(4);
        // 设置简化测试摘要。
        metadata.setSha256("abc");
        // 设置化验报告业务分类。
        metadata.setCategory(MedicalFileCategory.LAB_REPORT);
        // 设置初始已存储状态。
        metadata.setStatus(MedicalFileStatus.STORED);
        // 设置固定上传时间，保证测试可重复。
        metadata.setUploadedAt(Instant.parse("2026-08-04T00:00:00Z"));
        // 返回构造完成的测试元数据。
        return metadata;
    }
}
