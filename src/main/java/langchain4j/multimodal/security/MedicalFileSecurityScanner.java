// 声明多模态医疗文件安全扫描功能所在的包。
package langchain4j.multimodal.security;

/** 上传文件安全扫描边界，生产环境可替换为 ClamAV 或云安全扫描。 */
public interface MedicalFileSecurityScanner {

    // 扫描文件名、扩展名和二进制正文，发现风险时直接抛出参数异常。
    void scan(String filename, String extension, byte[] content);
}
