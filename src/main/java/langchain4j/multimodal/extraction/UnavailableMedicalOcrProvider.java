// 声明多模态医疗文件文字提取功能所在的包。
package langchain4j.multimodal.extraction;

// 引入存储层内部文件对象，以实现统一 OCR 接口。
import langchain4j.multimodal.file.StoredMedicalFile;

/** 未配置 OCR 服务时的明确占位实现，不伪造识别结果。 */
public class UnavailableMedicalOcrProvider implements MedicalOcrProvider {

    // 覆盖接口方法，提供稳定的占位实现名称。
    @Override
    public String providerName() {
        // 明确告诉调用方当前没有可用的 OCR 服务。
        return "unavailable";
    }

    // 覆盖接口方法，报告服务不可用状态。
    @Override
    public boolean available() {
        // 固定返回 false，防止业务层误调用占位实现。
        return false;
    }

    // 覆盖识别方法，在被误调用时给出明确错误。
    @Override
    public String recognize(StoredMedicalFile file) {
        // 不生成虚假的 OCR 内容，要求先配置真实服务。
        throw new IllegalStateException("图片 OCR 服务尚未配置。");
    }
}
