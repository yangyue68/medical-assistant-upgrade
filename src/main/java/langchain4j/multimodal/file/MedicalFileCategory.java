// 声明多模态医疗文件公共模型所在的包。
package langchain4j.multimodal.file;

/** 用户上传医疗文件的业务类型。 */
public enum MedicalFileCategory {
    // 化验报告，例如血常规、生化检查等包含数值指标的报告。
    LAB_REPORT,
    // 检查报告，例如超声、CT、核磁共振等文字报告。
    EXAMINATION_REPORT,
    // 病历资料，例如门诊病历、出院记录等。
    MEDICAL_RECORD,
    // 医学影像或医学报告截图。
    MEDICAL_IMAGE,
    // 无法归入以上类型的其他医疗文件。
    OTHER
}
