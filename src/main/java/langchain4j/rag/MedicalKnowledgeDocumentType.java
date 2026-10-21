// 声明当前枚举所在的包，rag 表示它属于检索增强生成模块。
package langchain4j.rag;

// 定义医学知识文档类型枚举，方便后续按文档类型过滤和评测。
public enum MedicalKnowledgeDocumentType {

    // 面向患者的健康科普主题，例如疾病介绍、症状、检查说明。
    HEALTH_TOPIC,

    // 临床指南或诊疗规范，后续接入指南数据时使用。
    CLINICAL_GUIDELINE,

    // 检查检验说明，例如血常规、A1C、肝功能等。
    MEDICAL_TEST,

    // 医院或科室信息，例如虚拟医院科室介绍。
    HOSPITAL_INFO,

    // 安全边界文档，例如急诊提醒、免责声明、不能替代医生诊断等。
    SAFETY_POLICY
}
