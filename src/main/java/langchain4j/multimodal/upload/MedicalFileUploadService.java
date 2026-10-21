// 声明多模态医疗文件上传功能所在的包。
package langchain4j.multimodal.upload;

// 引入医疗文件业务分类。
import langchain4j.multimodal.file.MedicalFileCategory;
// 引入医疗文件处理状态。
import langchain4j.multimodal.file.MedicalFileStatus;
// 引入存储层内部文件对象。
import langchain4j.multimodal.file.StoredMedicalFile;
// 引入可安全返回的上传文件元数据。
import langchain4j.multimodal.file.UploadedMedicalFile;
// 引入文件安全扫描边界。
import langchain4j.multimodal.security.MedicalFileSecurityScanner;
// 引入医疗文件存储边界。
import langchain4j.multimodal.storage.MedicalFileStorage;
// 引入字节数组输入流，用于把完成扫描的正文交给存储层。
import java.io.ByteArrayInputStream;
// 引入读取 MultipartFile 时可能产生的输入输出异常。
import java.io.IOException;
// 引入消息摘要工具，用于计算文件 SHA-256。
import java.security.MessageDigest;
// 引入摘要算法不可用异常。
import java.security.NoSuchAlgorithmException;
// 引入时间戳类型，用于记录服务端接收时间。
import java.time.Instant;
// 引入十六进制格式化工具，用于编码摘要字节。
import java.util.HexFormat;
// 引入固定区域规则，避免大小写转换受系统语言影响。
import java.util.Locale;
// 引入映射类型，用于维护扩展名与 MIME 白名单。
import java.util.Map;
// 引入集合类型，用于保存每个扩展名允许的 MIME 类型。
import java.util.Set;
// 引入 UUID 工具，用于生成不可预测的文件 ID。
import java.util.UUID;
// 引入 Spring 上传文件抽象。
import org.springframework.web.multipart.MultipartFile;

/** 医疗文件上传校验、哈希和存储编排服务。 */
public class MedicalFileUploadService {

    // 定义受支持扩展名与允许 MIME 类型之间的白名单关系。
    private static final Map<String, Set<String>> ALLOWED_TYPES = Map.of(
            // PDF 允许标准类型和部分客户端使用的通用二进制类型。
            ".pdf", Set.of("application/pdf", "application/octet-stream"),
            // TXT 允许标准纯文本类型和通用二进制类型。
            ".txt", Set.of("text/plain", "application/octet-stream"),
            // PNG 允许标准图片类型和通用二进制类型。
            ".png", Set.of("image/png", "application/octet-stream"),
            // JPG 允许标准 JPEG 类型和通用二进制类型。
            ".jpg", Set.of("image/jpeg", "application/octet-stream"),
            // JPEG 与 JPG 使用相同 MIME 白名单。
            ".jpeg", Set.of("image/jpeg", "application/octet-stream")
    );

    // 保存可替换的医疗文件存储实现。
    private final MedicalFileStorage storage;
    // 保存可替换的文件安全扫描实现。
    private final MedicalFileSecurityScanner securityScanner;
    // 保存应用层允许的单文件最大字节数。
    private final long maxFileSize;

    // 创建上传服务并注入存储、安全扫描和大小限制。
    public MedicalFileUploadService(MedicalFileStorage storage,
                                    // 接收文件安全扫描实现。
                                    MedicalFileSecurityScanner securityScanner,
                                    // 接收单文件最大字节数。
                                    long maxFileSize) {
        // 存储实现是上传持久化的必要依赖。
        if (storage == null) {
            // 缺少存储实现时尽早失败。
            throw new IllegalArgumentException("Medical file storage must not be null.");
        }
        // 文件大小限制必须为正数。
        if (maxFileSize <= 0) {
            // 拒绝无效配置，避免所有文件被误接受或误拒绝。
            throw new IllegalArgumentException("Medical file maximum size must be positive.");
        }
        // 安全扫描器是上传前置检查的必要依赖。
        if (securityScanner == null) {
            // 缺少扫描实现时阻止服务启动。
            throw new IllegalArgumentException("Medical file security scanner must not be null.");
        }
        // 保存存储实现供上传、查询和删除使用。
        this.storage = storage;
        // 保存安全扫描实现供每次上传使用。
        this.securityScanner = securityScanner;
        // 保存最大文件大小限制。
        this.maxFileSize = maxFileSize;
    }

    // 校验、扫描、生成元数据并持久化一个医疗文件。
    public UploadedMedicalFile upload(MultipartFile file, MedicalFileCategory category) {
        // Multipart 字段缺失或正文为空时直接拒绝。
        if (file == null || file.isEmpty()) {
            // 返回适合展示给用户的选择文件提示。
            throw new IllegalArgumentException("请选择要上传的医疗文件。");
        }
        // 在读取完整正文前先使用 MultipartFile 提供的大小检查上限。
        if (file.getSize() > maxFileSize) {
            // 超过应用层限制时停止处理。
            throw new IllegalArgumentException("文件超过允许的最大大小。");
        }
        // 去除客户端路径并校验原始文件名。
        String originalFilename = safeOriginalFilename(file.getOriginalFilename());
        // 提取并规范化小写扩展名。
        String extension = extensionOf(originalFilename);
        // 规范化客户端声明的 MIME 类型。
        String contentType = normalizeContentType(file.getContentType());
        // 联合扩展名和 MIME 白名单做第一层类型校验。
        validateType(extension, contentType);
        // 读取正文后继续执行文件签名和内容安全检查。
        try {
            // 将上传正文读取到内存，供签名、扫描、哈希和存储复用。
            byte[] bytes = file.getBytes();
            // 根据扩展名检查真实文件头或文本特征。
            validateSignature(extension, bytes);
            // 执行 EICAR、PDF 主动内容和图片解码等安全检查。
            securityScanner.scan(originalFilename, extension, bytes);
            // 生成不包含正文和磁盘路径的安全元数据。
            UploadedMedicalFile metadata = metadata(originalFilename, extension, contentType, bytes,
                    // 没有明确分类时使用 OTHER，避免 null 进入后续流程。
                    category == null ? MedicalFileCategory.OTHER : category);
            // 将扫描通过的正文和元数据写入私有存储。
            storage.store(metadata, new ByteArrayInputStream(bytes));
            // 返回可安全暴露给 Controller 的元数据。
            return metadata;
        // 捕获读取 MultipartFile 正文时的输入输出异常。
        } catch (IOException exception) {
            // 转换为状态异常并保留原始原因。
            throw new IllegalStateException("读取上传文件失败。", exception);
        }
    }

    // 根据随机文件 ID 查询安全元数据。
    public UploadedMedicalFile find(String fileId) {
        // 委托存储层查询文件。
        return storage.find(fileId)
                // 从内部存储对象映射出不含磁盘路径的元数据。
                .map(StoredMedicalFile::metadata)
                // 查询不到时返回统一的业务参数异常。
                .orElseThrow(() -> new IllegalArgumentException("医疗文件不存在。"));
    }

    // 根据随机文件 ID 获取包含内部路径的存储对象。
    public StoredMedicalFile requireStoredFile(String fileId) {
        // 内部处理服务需要正文路径，因此保留 StoredMedicalFile。
        return storage.find(fileId)
                // 查询不到时停止后续提取流程。
                .orElseThrow(() -> new IllegalArgumentException("医疗文件不存在。"));
    }

    // 删除指定医疗文件的正文和元数据。
    public boolean delete(String fileId) {
        // 将删除动作委托给存储边界。
        return storage.delete(fileId);
    }

    // 根据校验通过的文件信息创建安全元数据对象。
    private UploadedMedicalFile metadata(String filename, String extension, String contentType,
                                         // 接收完整文件正文和最终业务分类。
                                         byte[] bytes, MedicalFileCategory category) {
        // 创建可由 Jackson 序列化的元数据对象。
        UploadedMedicalFile metadata = new UploadedMedicalFile();
        // 使用随机 UUID 作为存储键，避免泄漏原始文件名和枚举攻击。
        metadata.setFileId(UUID.randomUUID().toString());
        // 保存已经去除客户端路径的展示文件名。
        metadata.setOriginalFilename(filename);
        // 保存经过白名单校验的小写扩展名。
        metadata.setExtension(extension);
        // 保存经过规范化和白名单校验的 MIME 类型。
        metadata.setContentType(contentType);
        // 保存实际读取到的正文长度，而不只信任客户端声明。
        metadata.setSize(bytes.length);
        // 计算并保存正文 SHA-256 摘要。
        metadata.setSha256(sha256(bytes));
        // 保存非空业务分类。
        metadata.setCategory(category);
        // 新上传文件的初始状态为已安全存储。
        metadata.setStatus(MedicalFileStatus.STORED);
        // 使用服务端当前时间记录上传时间。
        metadata.setUploadedAt(Instant.now());
        // 返回构建完成的安全元数据。
        return metadata;
    }

    // 清除客户端路径并校验用于展示的原始文件名。
    private String safeOriginalFilename(String filename) {
        // 将 Windows 分隔符统一为斜杠，null 则使用安全默认名。
        String normalized = filename == null ? "medical-file" : filename.replace('\\', '/');
        // 只保留最后一个路径片段并清理首尾空白。
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        // 拒绝空名、过长名称和路径穿越片段。
        if (normalized.isEmpty() || normalized.length() > 200 || normalized.contains("..")) {
            // 返回统一的非法文件名提示。
            throw new IllegalArgumentException("文件名不合法。");
        }
        // 返回不含客户端目录信息的安全文件名。
        return normalized;
    }

    // 从安全文件名中提取小写扩展名。
    private String extensionOf(String filename) {
        // 查找最后一个点号作为扩展名起点。
        int dot = filename.lastIndexOf('.');
        // 没有扩展名时无法执行白名单和签名分派。
        if (dot < 0) {
            // 要求用户上传带明确扩展名的文件。
            throw new IllegalArgumentException("文件必须包含扩展名。");
        }
        // 使用固定区域规则转为小写并返回扩展名。
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    // 规范化客户端提供的 MIME 类型。
    private String normalizeContentType(String contentType) {
        // 缺少 MIME 类型时使用通用二进制类型。
        return contentType == null || contentType.isBlank()
                // 返回允许进入后续签名检查的通用类型。
                ? "application/octet-stream"
                // 去除 charset 等参数，并按固定区域规则转为小写。
                : contentType.toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
    }

    // 联合检查扩展名和 MIME 类型白名单。
    private void validateType(String extension, String contentType) {
        // 查询当前扩展名允许的 MIME 集合。
        Set<String> allowed = ALLOWED_TYPES.get(extension);
        // 扩展名未知或 MIME 不匹配时拒绝文件。
        if (allowed == null || !allowed.contains(contentType)) {
            // 向用户说明当前支持的文件类型。
            throw new IllegalArgumentException("仅支持 PDF、TXT、PNG、JPG 医疗文件。");
        }
    }

    // 根据扩展名检查正文真实签名，降低只改后缀绕过校验的风险。
    private void validateSignature(String extension, byte[] bytes) {
        // 任何受支持类型都不能是空正文。
        if (bytes.length == 0) {
            // 返回明确的空文件错误。
            throw new IllegalArgumentException("上传文件不能为空。");
        }
        // 使用 Java switch 表达式按扩展名选择签名规则。
        boolean valid = switch (extension) {
            // PDF 必须以标准 %PDF- 文件头开始。
            case ".pdf" -> startsWith(bytes, new byte[]{'%', 'P', 'D', 'F', '-'});
            // PNG 必须匹配固定的八字节魔数。
            case ".png" -> startsWith(bytes, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
            // JPEG 必须以 FF D8 FF 三字节标记开始。
            case ".jpg", ".jpeg" -> bytes.length >= 3
                    // 逐字节比较 JPEG 文件头。
                    && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF;
            // 纯文本不能包含常见二进制 NUL 字节。
            case ".txt" -> !containsNullByte(bytes);
            // 未知扩展名默认判定为无效。
            default -> false;
        };
        // 任一签名规则不通过都拒绝文件。
        if (!valid) {
            // 说明文件正文与扩展名不一致。
            throw new IllegalArgumentException("文件内容与扩展名不匹配。");
        }
    }

    // 判断字节数组是否以指定魔数开头。
    private boolean startsWith(byte[] bytes, byte[] prefix) {
        // 正文比魔数短时一定无法匹配。
        if (bytes.length < prefix.length) {
            // 直接返回不匹配结果。
            return false;
        }
        // 逐个比较魔数范围内的字节。
        for (int i = 0; i < prefix.length; i++) {
            // 任一位置不一致即可提前结束。
            if (bytes[i] != prefix[i]) {
                // 返回不匹配结果。
                return false;
            }
        }
        // 全部前缀字节一致时返回匹配成功。
        return true;
    }

    // 检查待作为纯文本处理的正文是否包含 NUL 字节。
    private boolean containsNullByte(byte[] bytes) {
        // 遍历正文中的每个字节。
        for (byte value : bytes) {
            // NUL 字节通常意味着文件并非普通文本。
            if (value == 0) {
                // 命中后立即返回 true。
                return true;
            }
        }
        // 遍历完成后确认未发现 NUL 字节。
        return false;
    }

    // 计算文件正文的 SHA-256 十六进制摘要。
    private String sha256(byte[] bytes) {
        // 获取 JDK 标准摘要实现并计算哈希。
        try {
            // 将摘要字节编码为固定长度的小写十六进制字符串。
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        // 标准 JDK 理论上始终提供 SHA-256，此异常代表运行环境异常。
        } catch (NoSuchAlgorithmException exception) {
            // 转换为不可恢复的状态异常并保留原因。
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
