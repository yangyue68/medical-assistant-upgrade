// 声明多模态医疗文件公共模型所在的包。
package langchain4j.multimodal.file;

// 引入时间戳类型，用于记录文件接收时间。
import java.time.Instant;

/** 上传文件的安全元数据，不包含磁盘绝对路径和文件正文。 */
public class UploadedMedicalFile {

    // 使用随机 UUID 标识文件，避免用原始文件名拼接磁盘路径。
    private String fileId;
    // 保存去除路径信息后的原始文件名，仅用于界面展示。
    private String originalFilename;
    // 保存经过规范化和白名单校验的 MIME 类型。
    private String contentType;
    // 保存经过白名单校验的小写扩展名。
    private String extension;
    // 保存文件正文的字节数。
    private long size;
    // 保存文件正文的 SHA-256 摘要，用于审计和重复文件识别。
    private String sha256;
    // 保存用户选择或系统默认的医疗文件业务分类。
    private MedicalFileCategory category;
    // 保存文件在处理流水线中的当前状态。
    private MedicalFileStatus status;
    // 保存服务端成功接收文件的时间。
    private Instant uploadedAt;

    // 返回随机文件 ID。
    public String getFileId() {
        // 将当前文件 ID 返回给调用方。
        return fileId;
    }

    // 设置随机文件 ID。
    public void setFileId(String fileId) {
        // 保存上传服务生成的 UUID。
        this.fileId = fileId;
    }

    // 返回用于展示的原始文件名。
    public String getOriginalFilename() {
        // 将安全化后的原始文件名返回给调用方。
        return originalFilename;
    }

    // 设置用于展示的原始文件名。
    public void setOriginalFilename(String originalFilename) {
        // 保存已经去除客户端路径的文件名。
        this.originalFilename = originalFilename;
    }

    // 返回规范化后的 MIME 类型。
    public String getContentType() {
        // 将文件内容类型返回给调用方。
        return contentType;
    }

    // 设置规范化后的 MIME 类型。
    public void setContentType(String contentType) {
        // 保存经过上传白名单校验的内容类型。
        this.contentType = contentType;
    }

    // 返回小写文件扩展名。
    public String getExtension() {
        // 将扩展名返回给调用方。
        return extension;
    }

    // 设置小写文件扩展名。
    public void setExtension(String extension) {
        // 保存经过白名单校验的扩展名。
        this.extension = extension;
    }

    // 返回文件大小。
    public long getSize() {
        // 将文件字节数返回给调用方。
        return size;
    }

    // 设置文件大小。
    public void setSize(long size) {
        // 保存上传正文的实际字节数。
        this.size = size;
    }

    // 返回文件 SHA-256 摘要。
    public String getSha256() {
        // 将文件摘要返回给调用方。
        return sha256;
    }

    // 设置文件 SHA-256 摘要。
    public void setSha256(String sha256) {
        // 保存上传服务计算出的十六进制摘要。
        this.sha256 = sha256;
    }

    // 返回医疗文件业务分类。
    public MedicalFileCategory getCategory() {
        // 将业务分类返回给调用方。
        return category;
    }

    // 设置医疗文件业务分类。
    public void setCategory(MedicalFileCategory category) {
        // 保存用户选择或默认的文件分类。
        this.category = category;
    }

    // 返回医疗文件处理状态。
    public MedicalFileStatus getStatus() {
        // 将当前处理状态返回给调用方。
        return status;
    }

    // 设置医疗文件处理状态。
    public void setStatus(MedicalFileStatus status) {
        // 保存流水线最新状态。
        this.status = status;
    }

    // 返回服务端接收文件的时间。
    public Instant getUploadedAt() {
        // 将上传时间返回给调用方。
        return uploadedAt;
    }

    // 设置服务端接收文件的时间。
    public void setUploadedAt(Instant uploadedAt) {
        // 保存上传服务生成的时间戳。
        this.uploadedAt = uploadedAt;
    }
}
