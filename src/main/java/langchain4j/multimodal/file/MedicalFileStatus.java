// 声明多模态医疗文件公共模型所在的包。
package langchain4j.multimodal.file;

/** 医疗文件在本地处理流水线中的状态。 */
public enum MedicalFileStatus {
    // 文件已通过校验并持久化到私有存储目录。
    STORED,
    // 文件中的文字已经成功提取。
    TEXT_EXTRACTED,
    // 当前环境缺少处理该文件所需的提取能力。
    EXTRACTION_UNAVAILABLE,
    // 文件处理过程中发生失败。
    FAILED
}
