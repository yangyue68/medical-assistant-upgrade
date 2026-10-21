// 声明多模态医疗文件文字提取功能所在的包。
package langchain4j.multimodal.extraction;

// 引入存储层内部文件对象，供 OCR 实现读取图片。
import langchain4j.multimodal.file.StoredMedicalFile;

/** 图片 OCR/视觉模型边界。 */
public interface MedicalOcrProvider {

    // 返回 OCR 实现名称，用于接口响应和运行诊断。
    String providerName();

    // 表示当前 OCR 实现是否已经配置且可调用。
    boolean available();

    // 对存储中的图片进行文字识别，并返回识别后的纯文本。
    String recognize(StoredMedicalFile file);
}
