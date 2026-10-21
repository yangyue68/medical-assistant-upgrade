// 声明多模态医疗文件存储功能所在的包。
package langchain4j.multimodal.storage;

// 引入存储层内部文件对象。
import langchain4j.multimodal.file.StoredMedicalFile;
// 引入可安全持久化的上传文件元数据。
import langchain4j.multimodal.file.UploadedMedicalFile;
// 引入 Jackson 对象映射器，用于读写元数据 JSON 文件。
import com.fasterxml.jackson.databind.ObjectMapper;
// 引入文件系统操作可能产生的输入输出异常。
import java.io.IOException;
// 引入输入流类型，用于接收待持久化的文件正文。
import java.io.InputStream;
// 引入文件系统工具类。
import java.nio.file.Files;
// 引入跨平台文件路径类型。
import java.nio.file.Path;
// 引入文件复制选项，用于明确允许同 ID 文件覆盖。
import java.nio.file.StandardCopyOption;
// 引入映射接口，用于内存索引。
import java.util.Map;
// 引入 Optional，显式表示文件可能不存在。
import java.util.Optional;
// 引入线程安全映射，支持多个请求并发访问内存索引。
import java.util.concurrent.ConcurrentHashMap;

/** 开发环境本地私有文件存储实现。 */
public class LocalMedicalFileStorage implements MedicalFileStorage {

    // 保存经过绝对化和规范化的私有存储根目录。
    private final Path rootDirectory;
    // 保存项目统一的 JSON 序列化组件。
    private final ObjectMapper objectMapper;
    // 缓存文件 ID 到内部存储对象的映射，减少重复读取元数据。
    private final Map<String, StoredMedicalFile> files = new ConcurrentHashMap<>();

    // 创建本地存储并校验目录与 JSON 组件。
    public LocalMedicalFileStorage(String rootDirectory, ObjectMapper objectMapper) {
        // 存储目录必须由配置提供且不能是空白字符串。
        if (rootDirectory == null || rootDirectory.trim().isEmpty()) {
            // 配置错误时尽早失败，避免文件写入未知位置。
            throw new IllegalArgumentException("Medical file storage directory must not be blank.");
        }
        // 元数据持久化必须依赖有效的对象映射器。
        if (objectMapper == null) {
            // 缺失依赖时阻止创建不完整的存储实现。
            throw new IllegalArgumentException("ObjectMapper must not be null.");
        }
        // 将配置目录转换为规范化绝对路径，作为后续路径边界。
        this.rootDirectory = Path.of(rootDirectory).toAbsolutePath().normalize();
        // 保存对象映射器供元数据读写复用。
        this.objectMapper = objectMapper;
    }

    // 实现文件正文和元数据的本地持久化。
    @Override
    public StoredMedicalFile store(UploadedMedicalFile metadata, InputStream inputStream) {
        // 文件元数据和正文流缺一不可。
        if (metadata == null || inputStream == null) {
            // 对无效调用返回明确错误。
            throw new IllegalArgumentException("Medical file metadata and input stream must not be null.");
        }
        // 使用随机文件 ID 和白名单扩展名生成磁盘文件名。
        String storedFilename = metadata.getFileId() + metadata.getExtension();
        // 在存储根目录下解析并规范化目标路径。
        Path target = rootDirectory.resolve(storedFilename).normalize();
        // 再次确认目标路径没有逃逸出私有存储根目录。
        if (!target.startsWith(rootDirectory)) {
            // 拒绝任何可能的路径穿越存储键。
            throw new IllegalArgumentException("Invalid medical file storage key.");
        }
        // 将正文和元数据作为一次逻辑操作写入。
        try {
            // 首次使用时创建私有存储目录及父目录。
            Files.createDirectories(rootDirectory);
            // 将上传正文复制到随机文件 ID 对应的目标路径。
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            // 组合安全元数据与内部磁盘路径。
            StoredMedicalFile stored = new StoredMedicalFile(metadata, target);
            // 把元数据写入独立 JSON 旁路文件，支持应用重启后恢复。
            writeMetadata(metadata);
            // 将最新存储对象放入线程安全内存索引。
            files.put(metadata.getFileId(), stored);
            // 返回仅供后端内部使用的存储对象。
            return stored;
        // 捕获正文或元数据写入失败。
        } catch (IOException exception) {
            // 尝试清理可能已写入一半的文件，保持正文和元数据一致。
            try {
                // 删除可能已创建的正文文件。
                Files.deleteIfExists(target);
                // 删除可能已创建的元数据旁路文件。
                Files.deleteIfExists(metadataPath(metadata.getFileId()));
            // 清理失败时保留原始写入异常作为主异常。
            } catch (IOException cleanupException) {
                // 将清理异常附加到主异常，便于诊断而不覆盖根因。
                exception.addSuppressed(cleanupException);
            }
            // 将底层输入输出异常转换为存储状态异常。
            throw new IllegalStateException("Failed to store medical file.", exception);
        }
    }

    // 根据随机文件 ID 查询本地存储对象。
    @Override
    public Optional<StoredMedicalFile> find(String fileId) {
        // 空文件 ID 不执行任何磁盘查找。
        if (fileId == null || fileId.isBlank()) {
            // 用 Optional.empty 表示没有有效查询条件。
            return Optional.empty();
        }
        // 优先从线程安全内存索引查找。
        StoredMedicalFile stored = files.get(fileId);
        // 只有缓存存在且正文文件仍存在时才命中。
        if (stored != null && Files.isRegularFile(stored.path())) {
            // 返回有效的缓存存储对象。
            return Optional.of(stored);
        }
        // 缓存未命中时通过元数据旁路文件恢复存储对象。
        return readStoredFile(fileId);
    }

    // 更新已存在文件的安全元数据。
    @Override
    public void updateMetadata(UploadedMedicalFile metadata) {
        // 更新操作必须携带有效文件 ID。
        if (metadata == null || metadata.getFileId() == null || metadata.getFileId().isBlank()) {
            // 阻止写入无法与正文关联的元数据。
            throw new IllegalArgumentException("Medical file metadata must contain a file ID.");
        }
        // 确认目标正文真实存在，并取得其内部路径。
        StoredMedicalFile stored = find(metadata.getFileId())
                // 文件不存在时拒绝产生孤立元数据。
                .orElseThrow(() -> new IllegalArgumentException("医疗文件不存在。"));
        // 持久化新元数据并刷新内存索引。
        try {
            // 覆盖该文件对应的元数据 JSON。
            writeMetadata(metadata);
            // 保留原正文路径并替换缓存中的元数据对象。
            files.put(metadata.getFileId(), new StoredMedicalFile(metadata, stored.path()));
        // 捕获元数据文件写入异常。
        } catch (IOException exception) {
            // 转换为存储状态异常并保留底层原因。
            throw new IllegalStateException("Failed to update medical file metadata.", exception);
        }
    }

    // 删除文件正文、元数据旁路文件和内存索引。
    @Override
    public boolean delete(String fileId) {
        // 删除前查询正文及其内部路径。
        Optional<StoredMedicalFile> stored = find(fileId);
        // 无论磁盘状态如何都先清理内存索引。
        files.remove(fileId);
        // 查询不到文件时无需执行磁盘删除。
        if (stored.isEmpty()) {
            // 返回 false 表示目标原本不存在。
            return false;
        }
        // 执行正文和元数据文件删除。
        try {
            // 删除正文并记录正文是否实际存在。
            boolean deleted = Files.deleteIfExists(stored.get().path());
            // 删除对应元数据旁路文件。
            Files.deleteIfExists(metadataPath(fileId));
            // 返回正文文件的删除结果。
            return deleted;
        // 捕获文件系统删除失败。
        } catch (IOException exception) {
            // 转换为存储状态异常并保留底层原因。
            throw new IllegalStateException("Failed to delete medical file.", exception);
        }
    }

    // 将安全元数据序列化到文件 ID 对应的 JSON 文件。
    private void writeMetadata(UploadedMedicalFile metadata) throws IOException {
        // 使用项目统一 ObjectMapper 直接写入元数据旁路文件。
        objectMapper.writeValue(metadataPath(metadata.getFileId()).toFile(), metadata);
    }

    // 应用重启或缓存未命中时从磁盘恢复存储对象。
    private Optional<StoredMedicalFile> readStoredFile(String fileId) {
        // 计算并校验元数据旁路文件路径。
        Path metadataPath = metadataPath(fileId);
        // 元数据文件不存在时视为整个医疗文件不存在。
        if (!Files.isRegularFile(metadataPath)) {
            // 返回空结果，不创建任何缓存项。
            return Optional.empty();
        }
        // 读取元数据并恢复正文路径。
        try {
            // 将 JSON 元数据反序列化为安全元数据对象。
            UploadedMedicalFile metadata = objectMapper.readValue(metadataPath.toFile(), UploadedMedicalFile.class);
            // 使用文件 ID 和已校验扩展名恢复正文路径。
            Path filePath = rootDirectory.resolve(fileId + metadata.getExtension()).normalize();
            // 同时验证路径边界和正文文件存在性。
            if (!filePath.startsWith(rootDirectory) || !Files.isRegularFile(filePath)) {
                // 元数据孤立或路径异常时返回空结果。
                return Optional.empty();
            }
            // 组合恢复后的元数据和内部正文路径。
            StoredMedicalFile stored = new StoredMedicalFile(metadata, filePath);
            // 将恢复结果写回内存索引供后续请求复用。
            files.put(fileId, stored);
            // 返回恢复成功的存储对象。
            return Optional.of(stored);
        // 捕获元数据损坏或读取失败。
        } catch (IOException exception) {
            // 转换为存储状态异常，避免把损坏数据当成文件不存在。
            throw new IllegalStateException("Failed to read medical file metadata.", exception);
        }
    }

    // 根据文件 ID 构造并校验元数据旁路文件路径。
    private Path metadataPath(String fileId) {
        // 在私有根目录内拼接固定的元数据文件后缀。
        Path path = rootDirectory.resolve(fileId + ".metadata.json").normalize();
        // 规范化后再次验证路径不能逃逸根目录。
        if (!path.startsWith(rootDirectory)) {
            // 拒绝非法文件 ID 形成的路径穿越。
            throw new IllegalArgumentException("Invalid medical file storage key.");
        }
        // 返回已经通过根目录边界检查的路径。
        return path;
    }
}
