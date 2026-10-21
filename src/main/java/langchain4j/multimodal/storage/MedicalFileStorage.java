// 声明多模态医疗文件存储功能所在的包。
package langchain4j.multimodal.storage;

// 引入存储层内部文件对象。
import langchain4j.multimodal.file.StoredMedicalFile;
// 引入可安全返回的上传文件元数据。
import langchain4j.multimodal.file.UploadedMedicalFile;
// 引入输入流类型，避免存储接口依赖具体上传组件。
import java.io.InputStream;
// 引入 Optional，显式表示查询结果可能不存在。
import java.util.Optional;

/** 医疗文件存储边界，后续可替换为 OSS、S3 或私有对象存储。 */
public interface MedicalFileStorage {

    // 将文件正文和对应元数据写入私有存储，并返回内部存储对象。
    StoredMedicalFile store(UploadedMedicalFile metadata, InputStream inputStream);

    // 根据随机文件 ID 查询文件，不向调用者暴露可猜测的原始文件名路径。
    Optional<StoredMedicalFile> find(String fileId);

    // 在提取状态变化后更新文件的元数据旁路文件。
    void updateMetadata(UploadedMedicalFile metadata);

    // 删除文件正文和元数据，返回文件是否真实存在并被删除。
    boolean delete(String fileId);
}
