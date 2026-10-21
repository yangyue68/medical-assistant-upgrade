// 声明多模态医疗文件文字提取功能所在的包。
package langchain4j.multimodal.extraction;

// 引入医疗文件处理状态。
import langchain4j.multimodal.file.MedicalFileStatus;
// 引入只读告警列表类型。
import java.util.List;

/** OCR 或文档文本提取结果。 */
public record MedicalTextExtractionResult(
        // 标识本次提取所对应的上传文件。
        String fileId,
        // 记录实际执行提取的实现名称，便于排查 OCR 或解析器问题。
        String provider,
        // 表示文件当前所处的处理状态。
        MedicalFileStatus status,
        // 保存成功提取并经过长度限制的纯文本。
        String text,
        // 保存截断、能力缺失等不阻断返回的提示。
        List<String> warnings
) {
}
