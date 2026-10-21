// 声明 HTTP 控制器所在的包。
package langchain4j.controller;

// 引入项目统一的接口响应包装类型。
import langchain4j.common.ApiResponse;
// 引入文字提取结果类型。
import langchain4j.multimodal.extraction.MedicalTextExtractionResult;
// 引入医疗文件业务分类枚举。
import langchain4j.multimodal.file.MedicalFileCategory;
// 引入安全的上传文件元数据类型。
import langchain4j.multimodal.file.UploadedMedicalFile;
// 引入整份化验报告的解释结果类型。
import langchain4j.multimodal.report.LabReportInterpretation;
// 引入医疗报告提取、结构化和解释服务。
import langchain4j.multimodal.report.MedicalReportAnalysisService;
// 引入结构化报告结果类型。
import langchain4j.multimodal.report.StructuredMedicalReport;
// 引入医疗文件上传和查询服务。
import langchain4j.multimodal.upload.MedicalFileUploadService;
// 引入 OpenAPI 接口说明注解。
import io.swagger.v3.oas.annotations.Operation;
// 引入 OpenAPI 接口分组注解。
import io.swagger.v3.oas.annotations.tags.Tag;
// 引入日志接口。
import org.slf4j.Logger;
// 引入日志工厂。
import org.slf4j.LoggerFactory;
// 引入媒体类型常量。
import org.springframework.http.MediaType;
// 引入 DELETE 请求映射注解。
import org.springframework.web.bind.annotation.DeleteMapping;
// 引入 GET 请求映射注解。
import org.springframework.web.bind.annotation.GetMapping;
// 引入路径变量绑定注解。
import org.springframework.web.bind.annotation.PathVariable;
// 引入 POST 请求映射注解。
import org.springframework.web.bind.annotation.PostMapping;
// 引入控制器统一路径注解。
import org.springframework.web.bind.annotation.RequestMapping;
// 引入请求参数绑定注解。
import org.springframework.web.bind.annotation.RequestParam;
// 引入 REST 控制器注解。
import org.springframework.web.bind.annotation.RestController;
// 引入 Spring 上传文件抽象。
import org.springframework.web.multipart.MultipartFile;

/** 医疗报告上传、文本提取、结构化和指标解释接口。 */
// 将接口归入“医疗文件与报告”OpenAPI 分组。
@Tag(name = "医疗文件与报告")
// 声明当前类直接返回 JSON 响应。
@RestController
// 为本控制器中的全部接口设置统一路径前缀。
@RequestMapping("/xiaozhi/medical-files")
public class MedicalFileController {

    // 创建当前控制器专用日志记录器，日志中不记录文件正文。
    private static final Logger log = LoggerFactory.getLogger(MedicalFileController.class);

    // 保存上传、元数据查询和删除服务依赖。
    private final MedicalFileUploadService uploadService;
    // 保存文字提取、结构化和解释服务依赖。
    private final MedicalReportAnalysisService analysisService;

    // 通过构造器注入控制器需要的两个应用服务。
    public MedicalFileController(MedicalFileUploadService uploadService,
                                 // 接收医疗报告分析服务。
                                 MedicalReportAnalysisService analysisService) {
        // 保存上传服务供各接口复用。
        this.uploadService = uploadService;
        // 保存分析服务供各接口复用。
        this.analysisService = analysisService;
    }

    // 为 OpenAPI 文档描述上传接口。
    @Operation(summary = "上传医疗文件")
    // 接收 multipart/form-data 格式的 POST 请求。
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadedMedicalFile> upload(
            // 从 multipart 请求中读取必填的文件字段。
            @RequestParam("file") MultipartFile file,
            // 读取可选业务分类，未提供时由服务层使用 OTHER。
            @RequestParam(name = "category", required = false) MedicalFileCategory category
    ) {
        // 完成大小、类型、签名、安全扫描、哈希和本地持久化。
        UploadedMedicalFile uploaded = uploadService.upload(file, category);
        // 只记录安全元数据，避免把医疗正文或磁盘路径写入日志。
        log.info("Medical file uploaded. fileId={}, category={}, contentType={}, size={}, sha256={}",
                // 填充第一组日志参数。
                uploaded.getFileId(), uploaded.getCategory(), uploaded.getContentType(),
                // 填充第二组日志参数。
                uploaded.getSize(), uploaded.getSha256());
        // 使用统一成功响应返回不含磁盘路径的元数据。
        return ApiResponse.success(uploaded);
    }

    // 为 OpenAPI 文档描述元数据查询接口。
    @Operation(summary = "查询医疗文件元数据")
    // 将文件 ID 作为路径变量接收 GET 请求。
    @GetMapping("/{fileId}")
    public ApiResponse<UploadedMedicalFile> metadata(@PathVariable("fileId") String fileId) {
        // 查询并返回指定文件的安全元数据。
        return ApiResponse.success(uploadService.find(fileId));
    }

    // 为 OpenAPI 文档描述文字提取接口。
    @Operation(summary = "提取报告文本")
    // 将提取动作设计为 POST，避免中间缓存保存敏感结果。
    @PostMapping("/{fileId}/extract-text")
    public ApiResponse<MedicalTextExtractionResult> extractText(@PathVariable("fileId") String fileId) {
        // 根据文件类型执行 TXT、PDF 或 OCR 文字提取。
        return ApiResponse.success(analysisService.extractText(fileId));
    }

    // 为 OpenAPI 文档描述报告结构化接口。
    @Operation(summary = "结构化抽取化验报告")
    // 接收指定文件的结构化处理请求。
    @PostMapping("/{fileId}/structure")
    public ApiResponse<StructuredMedicalReport> structure(@PathVariable("fileId") String fileId) {
        // 先提取文字，再把符合规则的化验指标转换为结构化结果。
        return ApiResponse.success(analysisService.structure(fileId));
    }

    // 为 OpenAPI 文档描述化验指标解释接口。
    @Operation(summary = "解释化验指标")
    // 接收指定文件的非诊断性解释请求。
    @PostMapping("/{fileId}/interpret")
    public ApiResponse<LabReportInterpretation> interpret(@PathVariable("fileId") String fileId) {
        // 串联文字提取、结构化和指标解释后返回结果。
        return ApiResponse.success(analysisService.interpret(fileId));
    }

    // 为 OpenAPI 文档描述文件删除接口。
    @Operation(summary = "删除医疗文件")
    // 将文件 ID 作为路径变量接收 DELETE 请求。
    @DeleteMapping("/{fileId}")
    public ApiResponse<Boolean> delete(@PathVariable("fileId") String fileId) {
        // 同时删除文件正文和对应的元数据文件。
        return ApiResponse.success(uploadService.delete(fileId));
    }
}
