// 声明多模态医疗文件安全扫描功能所在的包。
package langchain4j.multimodal.security;

// 引入内存图片类型，用于验证图片能否被安全解码。
import java.awt.image.BufferedImage;
// 引入字节数组输入流，用于从上传正文解析图片。
import java.io.ByteArrayInputStream;
// 引入图片解码过程可能产生的输入输出异常。
import java.io.IOException;
// 引入单字节字符集，用于无损扫描二进制中的 ASCII 标记。
import java.nio.charset.StandardCharsets;
// 引入固定区域规则，避免文件名小写转换受系统语言影响。
import java.util.Locale;
// 引入 Java 标准图片解码工具。
import javax.imageio.ImageIO;

/** 无外部服务时的基础安全扫描，覆盖测试病毒特征和图片解压炸弹风险。 */
public class BasicMedicalFileSecurityScanner implements MedicalFileSecurityScanner {

    // 定义业界通用的 EICAR 防病毒测试文件特征。
    private static final String EICAR_MARKER = "EICAR-STANDARD-ANTIVIRUS-TEST-FILE";
    // 限制图片总像素数，降低超大图片造成内存耗尽的风险。
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;

    // 实现基础安全扫描流程，任一规则失败都会拒绝上传。
    @Override
    public void scan(String filename, String extension, byte[] content) {
        // 拒绝没有正文的文件。
        if (content == null || content.length == 0) {
            // 使用参数异常交给统一异常处理器转换为客户端错误。
            throw new IllegalArgumentException("上传文件不能为空。");
        }
        // 使用 ISO-8859-1 一一映射字节，便于扫描二进制中的 ASCII 特征。
        String ascii = new String(content, StandardCharsets.ISO_8859_1);
        // 检查文件是否包含 EICAR 测试病毒标记。
        if (ascii.contains(EICAR_MARKER)) {
            // 命中测试病毒特征时立即拒绝文件。
            throw new IllegalArgumentException("文件未通过安全扫描。");
        }
        // 对 PDF 额外检查脚本、启动动作和嵌入附件标记。
        if (".pdf".equals(extension) && containsActivePdfContent(ascii)) {
            // 拒绝含主动内容的 PDF，减少客户端打开文件时的风险。
            throw new IllegalArgumentException("PDF 包含不允许的主动内容或附件。");
        }
        // 图片文件需要实际解码并检查像素上限。
        if (".png".equals(extension) || ".jpg".equals(extension) || ".jpeg".equals(extension)) {
            // 调用图片专用校验逻辑。
            validateImage(content);
        }
        // 将文件名按固定区域规则转为小写，便于检查危险后缀。
        String normalizedName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        // 补充拒绝可执行文件和脚本文件名。
        if (normalizedName.endsWith(".exe") || normalizedName.endsWith(".js") || normalizedName.endsWith(".html")) {
            // 返回明确且不泄漏内部实现的拒绝原因。
            throw new IllegalArgumentException("不允许上传可执行文件或脚本。");
        }
    }

    // 检查 PDF 正文中常见的主动内容对象标记。
    private boolean containsActivePdfContent(String content) {
        // 检查 JavaScript 动作的完整标记。
        return content.contains("/JavaScript")
                // 检查 JavaScript 动作的缩写标记。
                || content.contains("/JS")
                // 检查外部程序或文件启动动作。
                || content.contains("/Launch")
                // 检查嵌入文件对象。
                || content.contains("/EmbeddedFile")
                // 检查打开文档时自动执行的动作。
                || content.contains("/OpenAction");
    }

    // 实际解码图片并校验像素数量。
    private void validateImage(byte[] content) {
        // 捕获 Java 图片解码器产生的输入输出异常。
        try {
            // 从内存中的上传字节解码图片，不写入临时公开目录。
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            // ImageIO 对无法识别的格式可能返回 null。
            if (image == null) {
                // 拒绝扩展名伪装或损坏的图片。
                throw new IllegalArgumentException("无法解析上传的图片。");
            }
            // 使用 long 计算宽高乘积，避免 int 乘法溢出。
            long pixels = (long) image.getWidth() * image.getHeight();
            // 拒绝无效尺寸和超过四千万像素的图片。
            if (pixels <= 0 || pixels > MAX_IMAGE_PIXELS) {
                // 给调用方返回统一的图片尺寸限制提示。
                throw new IllegalArgumentException("图片尺寸超过安全限制。");
            }
        // 将底层图片读取异常转换为项目统一处理的参数异常。
        } catch (IOException exception) {
            // 保留原始异常作为 cause，便于服务端诊断。
            throw new IllegalArgumentException("无法解析上传的图片。", exception);
        }
    }
}
