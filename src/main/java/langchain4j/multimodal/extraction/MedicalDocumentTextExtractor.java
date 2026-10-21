// 声明多模态医疗文件文字提取功能所在的包。
package langchain4j.multimodal.extraction;

// 引入医疗文件处理状态。
import langchain4j.multimodal.file.MedicalFileStatus;
// 引入存储层内部文件对象。
import langchain4j.multimodal.file.StoredMedicalFile;
// 引入医疗文件存储边界，用于持久化提取状态。
import langchain4j.multimodal.storage.MedicalFileStorage;
// 引入文档读取过程可能产生的输入输出异常。
import java.io.IOException;
// 引入 UTF-8 字符集，统一读取纯文本医疗文件。
import java.nio.charset.StandardCharsets;
// 引入文件系统工具类，用于读取私有存储中的 TXT 文件。
import java.nio.file.Files;
// 引入只读列表类型，用于返回提取告警。
import java.util.List;
// 引入 PDFBox 文档对象，用于安全打开 PDF。
import org.apache.pdfbox.pdmodel.PDDocument;
// 引入 PDFBox 文字提取器。
import org.apache.pdfbox.text.PDFTextStripper;

/** 按文件类型分派本地文本提取或图片 OCR。 */
public class MedicalDocumentTextExtractor {

    // 保存可替换的图片 OCR 服务。
    private final MedicalOcrProvider ocrProvider;
    // 保存文件存储接口，用于读取文件和更新处理状态。
    private final MedicalFileStorage storage;
    // 保存单次提取允许返回的最大字符数。
    private final int maxExtractedCharacters;
    // 保存单个 PDF 允许处理的最大页数。
    private final int maxPdfPages;

    // 创建文字提取器并注入处理能力与资源限制。
    public MedicalDocumentTextExtractor(MedicalOcrProvider ocrProvider,
                                        // 接收医疗文件存储实现。
                                        MedicalFileStorage storage,
                                        // 接收提取文本最大字符数。
                                        int maxExtractedCharacters,
                                        // 接收 PDF 最大页数。
                                        int maxPdfPages) {
        // 确保依赖存在且两个限制均为正数。
        if (ocrProvider == null || storage == null || maxExtractedCharacters <= 0 || maxPdfPages <= 0) {
            // 配置不完整时在应用启动阶段尽早失败。
            throw new IllegalArgumentException("OCR provider, storage and extraction limits are required.");
        }
        // 保存 OCR 服务供图片提取使用。
        this.ocrProvider = ocrProvider;
        // 保存存储接口供读取文件和更新元数据使用。
        this.storage = storage;
        // 保存字符数上限，防止超长文本占用过多内存和响应带宽。
        this.maxExtractedCharacters = maxExtractedCharacters;
        // 保存页数上限，控制 PDF 解析成本。
        this.maxPdfPages = maxPdfPages;
    }

    // 根据扩展名选择 TXT、PDF 或 OCR 提取流程。
    public MedicalTextExtractionResult extract(StoredMedicalFile file) {
        // 存储对象和元数据都是后续分派所必需的。
        if (file == null || file.metadata() == null) {
            // 对无效调用给出明确错误。
            throw new IllegalArgumentException("Medical file must not be null.");
        }
        // 从已校验的元数据读取小写扩展名。
        String extension = file.metadata().getExtension();
        // 纯文本文件直接按 UTF-8 本地读取。
        if (".txt".equals(extension)) {
            // 统一经过 success 方法做清洗、截断和状态更新。
            return success(file, "local-text", readText(file));
        }
        // PDF 文件使用 PDFBox 提取文本层。
        if (".pdf".equals(extension)) {
            // 统一经过 success 方法做清洗、截断和状态更新。
            return success(file, "pdfbox", readPdf(file));
        }
        // PNG 和 JPEG 等图片依赖 OCR 服务。
        if (!ocrProvider.available()) {
            // 明确把元数据状态更新为当前不可提取。
            file.metadata().setStatus(MedicalFileStatus.EXTRACTION_UNAVAILABLE);
            // 将状态变化持久化到元数据旁路文件。
            storage.updateMetadata(file.metadata());
            // 返回结构化的能力缺失结果，而不是伪造识别文字。
            return new MedicalTextExtractionResult(
                    // 返回当前文件 ID。
                    file.metadata().getFileId(),
                    // 返回占位或真实 OCR 实现名称。
                    ocrProvider.providerName(),
                    // 返回提取能力不可用状态。
                    MedicalFileStatus.EXTRACTION_UNAVAILABLE,
                    // 没有真实识别结果时正文保持为空。
                    "",
                    // 告知调用方文件已保存但尚未识别。
                    List.of("图片 OCR 服务尚未配置，文件已安全保存，暂未识别内容。")
            );
        }
        // OCR 可用时执行真实图片识别并统一处理结果。
        return success(file, ocrProvider.providerName(), ocrProvider.recognize(file));
    }

    // 规范化各提取器的原始文本，并同步文件状态。
    private MedicalTextExtractionResult success(StoredMedicalFile file, String provider, String rawText) {
        // 将 null 转为空字符串，同时删除 NUL 字符并清理首尾空白。
        String normalized = rawText == null ? "" : rawText.replace("\u0000", "").trim();
        // 超长文本只保留配置允许的前缀。
        if (normalized.length() > maxExtractedCharacters) {
            // 按 Java 字符下标截断，避免响应和后续解析无限增长。
            normalized = normalized.substring(0, maxExtractedCharacters);
        }
        // 清洗后为空说明文件没有可使用的文本层。
        if (normalized.isEmpty()) {
            // 更新内存中的文件处理状态。
            file.metadata().setStatus(MedicalFileStatus.EXTRACTION_UNAVAILABLE);
            // 将不可提取状态持久化。
            storage.updateMetadata(file.metadata());
            // 返回空文本和明确告警。
            return new MedicalTextExtractionResult(file.metadata().getFileId(), provider,
                    // 设置提取不可用状态、空正文和原因说明。
                    MedicalFileStatus.EXTRACTION_UNAVAILABLE, "", List.of("文件中没有提取到可读文本。"));
        }
        // 有有效文本时把文件状态更新为已提取。
        file.metadata().setStatus(MedicalFileStatus.TEXT_EXTRACTED);
        // 将成功状态持久化到元数据文件。
        storage.updateMetadata(file.metadata());
        // 返回提取器名称、规范化文本和空告警列表。
        return new MedicalTextExtractionResult(file.metadata().getFileId(), provider,
                // 设置成功状态并附带经过限制的文本。
                MedicalFileStatus.TEXT_EXTRACTED, normalized, List.of());
    }

    // 按 UTF-8 读取本地纯文本医疗文件。
    private String readText(StoredMedicalFile file) {
        // 捕获文件系统读取异常并转换为业务层异常。
        try {
            // 读取存储层内部路径对应的全部文本。
            return Files.readString(file.path(), StandardCharsets.UTF_8);
        // 处理文件不存在、权限不足或编码读取失败等情况。
        } catch (IOException exception) {
            // 保留底层异常作为 cause，便于服务端排查。
            throw new IllegalStateException("读取医疗文本文件失败。", exception);
        }
    }

    // 使用 PDFBox 读取 PDF 自带的文本层。
    private String readPdf(StoredMedicalFile file) {
        // 使用 try-with-resources 确保 PDF 文档句柄及时关闭。
        try (PDDocument document = PDDocument.load(file.path().toFile())) {
            // 加密 PDF 需要密码和更复杂的授权流程，当前明确拒绝。
            if (document.isEncrypted()) {
                // 向调用方说明当前不支持加密文档。
                throw new IllegalArgumentException("暂不支持加密 PDF。");
            }
            // 在完整提取前检查页数，限制 CPU 和内存开销。
            if (document.getNumberOfPages() > maxPdfPages) {
                // 超过配置页数时停止处理。
                throw new IllegalArgumentException("PDF 页数超过允许的最大值。");
            }
            // 提取 PDF 中已有的文字层；扫描件仍需要后续 OCR 能力。
            return new PDFTextStripper().getText(document);
        // 捕获 PDF 打开或解析过程中的输入输出异常。
        } catch (IOException exception) {
            // 转换为状态异常并保留底层原因。
            throw new IllegalStateException("解析医疗 PDF 失败。", exception);
        }
    }
}
