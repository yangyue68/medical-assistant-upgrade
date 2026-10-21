// 声明医疗文档文字提取器测试所在的包。
package langchain4j.multimodal.extraction;

// 引入医疗文件业务分类。
import langchain4j.multimodal.file.MedicalFileCategory;
// 引入医疗文件处理状态。
import langchain4j.multimodal.file.MedicalFileStatus;
// 引入存储层内部文件对象。
import langchain4j.multimodal.file.StoredMedicalFile;
// 引入可安全返回的上传文件元数据。
import langchain4j.multimodal.file.UploadedMedicalFile;
// 引入本地私有文件存储实现。
import langchain4j.multimodal.storage.LocalMedicalFileStorage;
// 引入支持 Java 时间模块的 Jackson 构建器。
import com.fasterxml.jackson.databind.json.JsonMapper;
// 引入字节数组输入流，用于向存储层写入测试正文。
import java.io.ByteArrayInputStream;
// 引入 UTF-8 字符集，用于构造中文文本文件。
import java.nio.charset.StandardCharsets;
// 引入路径类型，用于接收临时目录。
import java.nio.file.Path;
// 引入时间戳类型，用于构造固定元数据。
import java.time.Instant;
// 引入 PDFBox 文档对象，用于生成测试 PDF。
import org.apache.pdfbox.pdmodel.PDDocument;
// 引入 PDFBox 页面对象。
import org.apache.pdfbox.pdmodel.PDPage;
// 引入 PDFBox 页面内容写入流。
import org.apache.pdfbox.pdmodel.PDPageContentStream;
// 引入 PDFBox 内置 Helvetica 字体。
import org.apache.pdfbox.pdmodel.font.PDType1Font;
// 引入 JUnit 测试注解。
import org.junit.jupiter.api.Test;
// 引入 JUnit 临时目录注解。
import org.junit.jupiter.api.io.TempDir;

// 静态引入 AssertJ 断言入口。
import static org.assertj.core.api.Assertions.assertThat;

// 验证 TXT、图片能力缺失和 PDF 三种文字提取分支。
class MedicalDocumentTextExtractorTest {

    // 要求 JUnit 为测试创建隔离的私有存储目录。
    @TempDir
    // 保存当前测试使用的临时目录。
    Path tempDir;

    // 标记 UTF-8 文本提取和状态持久化测试。
    @Test
    void shouldExtractUtf8TextAndPersistStatus() {
        // 创建当前测试使用的本地存储。
        LocalMedicalFileStorage storage = storage();
        // 在私有存储中写入一份 UTF-8 化验报告。
        StoredMedicalFile file = store(storage, "text-file", ".txt",
                // 使用中文内容验证编码处理。
                "报告名称：血常规\n血红蛋白 98 g/L 115-150".getBytes(StandardCharsets.UTF_8));
        // 创建文字提取器，OCR 在本测试中不需要可用。
        MedicalDocumentTextExtractor extractor = new MedicalDocumentTextExtractor(
                // 设置一千字符和一百页资源限制。
                new UnavailableMedicalOcrProvider(), storage, 1000, 100
        );

        // 执行纯文本提取。
        MedicalTextExtractionResult result = extractor.extract(file);

        // 验证提取状态为成功。
        assertThat(result.status()).isEqualTo(MedicalFileStatus.TEXT_EXTRACTED);
        // 验证实际提供者标识为本地文本读取。
        assertThat(result.provider()).isEqualTo("local-text");
        // 验证中文指标没有发生乱码或丢失。
        assertThat(result.text()).contains("血红蛋白");
        // 从存储层重新读取最新元数据状态。
        assertThat(storage.find("text-file").orElseThrow().metadata().getStatus())
                // 验证提取成功状态已持久化。
                .isEqualTo(MedicalFileStatus.TEXT_EXTRACTED);
    }

    // 标记图片 OCR 未配置时的显式降级测试。
    @Test
    void shouldReturnUnavailableWhenImageOcrProviderIsNotConfigured() {
        // 创建当前测试使用的本地存储。
        LocalMedicalFileStorage storage = storage();
        // 直接存入一个扩展名为 PNG 的内部测试文件。
        StoredMedicalFile file = store(storage, "image-file", ".png", new byte[]{1, 2, 3});
        // 使用不可用 OCR 占位实现创建提取器。
        MedicalDocumentTextExtractor extractor = new MedicalDocumentTextExtractor(
                // 设置一千字符和一百页资源限制。
                new UnavailableMedicalOcrProvider(), storage, 1000, 100
        );

        // 执行图片文字提取。
        MedicalTextExtractionResult result = extractor.extract(file);

        // 验证状态明确表示当前无法提取。
        assertThat(result.status()).isEqualTo(MedicalFileStatus.EXTRACTION_UNAVAILABLE);
        // 验证系统没有伪造 OCR 正文。
        assertThat(result.text()).isEmpty();
        // 验证告警明确说明 OCR 能力缺失。
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("OCR"));
    }

    // 标记 PDF 文本层提取测试。
    @Test
    void shouldExtractTextFromPdf() throws Exception {
        // 创建当前测试使用的本地存储。
        LocalMedicalFileStorage storage = storage();
        // 声明稍后生成的 PDF 字节数组。
        byte[] pdfBytes;
        // 创建内存 PDF 文档并确保测试结束时关闭。
        try (PDDocument document = new PDDocument()) {
            // 创建一个空白 PDF 页面。
            PDPage page = new PDPage();
            // 将页面添加到文档。
            document.addPage(page);
            // 创建页面内容写入流并确保及时关闭。
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                // 开始 PDF 文字对象。
                content.beginText();
                // 使用内置 Helvetica 字体和 12 磅字号。
                content.setFont(PDType1Font.HELVETICA, 12);
                // 将文字起点移动到页面可见区域。
                content.newLineAtOffset(72, 720);
                // 写入一行符合结构化规则的英文指标文本。
                content.showText("Glucose 7.2 mmol/L 3.9-6.1");
                // 结束 PDF 文字对象。
                content.endText();
            }
            // 创建内存输出流接收生成的 PDF。
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            // 将 PDF 文档保存到内存输出流。
            document.save(output);
            // 取得完整 PDF 字节供存储层使用。
            pdfBytes = output.toByteArray();
        }
        // 将生成的 PDF 写入测试私有存储。
        StoredMedicalFile file = store(storage, "pdf-file", ".pdf", pdfBytes);
        // 创建 PDF 文字提取器。
        MedicalDocumentTextExtractor extractor = new MedicalDocumentTextExtractor(
                // 设置一千字符和一百页资源限制。
                new UnavailableMedicalOcrProvider(), storage, 1000, 100
        );

        // 执行 PDF 文本层提取。
        MedicalTextExtractionResult result = extractor.extract(file);

        // 验证 PDF 提取成功。
        assertThat(result.status()).isEqualTo(MedicalFileStatus.TEXT_EXTRACTED);
        // 验证实际提供者标识为 PDFBox。
        assertThat(result.provider()).isEqualTo("pdfbox");
        // 验证 PDF 中写入的完整指标文本被提取。
        assertThat(result.text()).contains("Glucose 7.2 mmol/L 3.9-6.1");
    }

    // 创建各测试复用的本地私有存储。
    private LocalMedicalFileStorage storage() {
        // 使用临时目录和支持时间类型的 JsonMapper。
        return new LocalMedicalFileStorage(tempDir.toString(),
                // 自动发现并注册 Jackson Java 时间模块。
                JsonMapper.builder().findAndAddModules().build());
    }

    // 直接向存储层写入测试文件，绕过本类不关心的上传安全检查。
    private StoredMedicalFile store(LocalMedicalFileStorage storage, String fileId,
                                    // 接收测试文件扩展名和正文。
                                    String extension, byte[] content) {
        // 创建与测试正文对应的安全元数据。
        UploadedMedicalFile metadata = new UploadedMedicalFile();
        // 设置可预测文件 ID，便于断言。
        metadata.setFileId(fileId);
        // 使用文件 ID 和扩展名构造展示文件名。
        metadata.setOriginalFilename(fileId + extension);
        // 设置待测试的扩展名。
        metadata.setExtension(extension);
        // 使用通用二进制 MIME 类型，存储层本身不做类型校验。
        metadata.setContentType("application/octet-stream");
        // 设置正文实际字节数。
        metadata.setSize(content.length);
        // 设置简化测试摘要。
        metadata.setSha256("test");
        // 使用 OTHER 分类避免无关业务分类影响测试。
        metadata.setCategory(MedicalFileCategory.OTHER);
        // 设置文件初始状态为已存储。
        metadata.setStatus(MedicalFileStatus.STORED);
        // 设置固定上传时间，保证测试可重复。
        metadata.setUploadedAt(Instant.parse("2026-08-04T00:00:00Z"));
        // 将元数据和正文写入本地存储并返回内部对象。
        return storage.store(metadata, new ByteArrayInputStream(content));
    }
}
