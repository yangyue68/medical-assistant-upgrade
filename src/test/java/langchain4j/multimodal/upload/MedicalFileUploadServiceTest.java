// 声明医疗文件上传服务测试所在的包。
package langchain4j.multimodal.upload;

// 引入医疗文件业务分类。
import langchain4j.multimodal.file.MedicalFileCategory;
// 引入医疗文件处理状态。
import langchain4j.multimodal.file.MedicalFileStatus;
// 引入可安全返回的上传文件元数据。
import langchain4j.multimodal.file.UploadedMedicalFile;
// 引入真实基础文件安全扫描器。
import langchain4j.multimodal.security.BasicMedicalFileSecurityScanner;
// 引入本地私有文件存储实现。
import langchain4j.multimodal.storage.LocalMedicalFileStorage;
// 引入医疗文件存储边界。
import langchain4j.multimodal.storage.MedicalFileStorage;
// 引入 Jackson 对象映射器接口。
import com.fasterxml.jackson.databind.ObjectMapper;
// 引入支持 Java 时间模块的 Jackson 构建器。
import com.fasterxml.jackson.databind.json.JsonMapper;
// 引入 UTF-8 字符集，用于构造中文上传正文。
import java.nio.charset.StandardCharsets;
// 引入文件系统工具类，用于验证文件真实落盘。
import java.nio.file.Files;
// 引入路径类型，用于接收临时目录。
import java.nio.file.Path;
// 引入 JUnit 测试注解。
import org.junit.jupiter.api.Test;
// 引入 JUnit 临时目录注解。
import org.junit.jupiter.api.io.TempDir;
// 引入 Spring 模拟上传文件类型。
import org.springframework.mock.web.MockMultipartFile;

// 静态引入 AssertJ 普通断言入口。
import static org.assertj.core.api.Assertions.assertThat;
// 静态引入 AssertJ 异常断言入口。
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 验证医疗文件上传的持久化、恢复和安全拒绝规则。
class MedicalFileUploadServiceTest {

    // 要求 JUnit 为测试创建隔离的临时存储目录。
    @TempDir
    // 保存当前测试使用的临时目录。
    Path tempDir;

    // 标记合法文本文件存储和重启恢复测试。
    @Test
    void shouldStoreValidTextFileWithSafeMetadataAndRestoreAfterRestart() throws Exception {
        // 创建支持 Instant 序列化的 ObjectMapper。
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        // 使用临时目录创建首个本地存储实例。
        LocalMedicalFileStorage storage = new LocalMedicalFileStorage(tempDir.toString(), objectMapper);
        // 使用 1024 字节上限创建上传服务。
        MedicalFileUploadService service = service(storage, 1024);
        // 构造一份合法 UTF-8 化验报告文本文件。
        MockMultipartFile file = new MockMultipartFile(
                // 设置字段名、文件名和 MIME 类型。
                "file", "blood-test.txt", "text/plain",
                // 设置包含中文化验指标的正文。
                "报告名称：血常规\n白细胞 12.5 10^9/L 3.5-9.5".getBytes(StandardCharsets.UTF_8)
        );

        // 执行上传并取得安全元数据。
        UploadedMedicalFile uploaded = service.upload(file, MedicalFileCategory.LAB_REPORT);

        // 验证服务生成了非空随机文件 ID。
        assertThat(uploaded.getFileId()).isNotBlank();
        // 验证元数据只保留安全化后的展示文件名。
        assertThat(uploaded.getOriginalFilename()).isEqualTo("blood-test.txt");
        // 验证 SHA-256 十六进制摘要长度为 64。
        assertThat(uploaded.getSha256()).hasSize(64);
        // 验证新上传文件状态为已存储。
        assertThat(uploaded.getStatus()).isEqualTo(MedicalFileStatus.STORED);
        // 验证正文以随机文件 ID 落盘。
        assertThat(Files.isRegularFile(tempDir.resolve(uploaded.getFileId() + ".txt"))).isTrue();
        // 验证对应元数据 JSON 已落盘。
        assertThat(Files.isRegularFile(tempDir.resolve(uploaded.getFileId() + ".metadata.json"))).isTrue();

        // 创建新存储实例模拟应用重启和内存缓存丢失。
        LocalMedicalFileStorage restartedStorage = new LocalMedicalFileStorage(tempDir.toString(), objectMapper);
        // 通过元数据旁路文件恢复存储对象。
        UploadedMedicalFile restored = restartedStorage.find(uploaded.getFileId()).orElseThrow().metadata();
        // 验证重启后原始展示文件名保持一致。
        assertThat(restored.getOriginalFilename()).isEqualTo("blood-test.txt");
        // 验证 Instant 上传时间可以完整序列化和反序列化。
        assertThat(restored.getUploadedAt()).isEqualTo(uploaded.getUploadedAt());
    }

    // 标记超大、伪装和危险文件拒绝测试。
    @Test
    void shouldRejectOversizedDisguisedAndUnsafeFiles() {
        // 创建仅允许 16 字节文件的上传服务。
        MedicalFileUploadService service = service(
                // 使用隔离临时目录和完整 JsonMapper 创建存储。
                new LocalMedicalFileStorage(tempDir.toString(), JsonMapper.builder().findAndAddModules().build()),
                // 设置极小上限便于覆盖超大文件分支。
                16
        );

        // 验证 17 字节文件超过 16 字节限制时被拒绝。
        assertThatThrownBy(() -> service.upload(
                // 构造超过限制的模拟文本文件。
                new MockMultipartFile("file", "large.txt", "text/plain", new byte[17]),
                // 使用 OTHER 分类避免分类逻辑干扰测试。
                MedicalFileCategory.OTHER
        // 验证异常类型和用户提示。
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("最大大小");

        // 验证正文没有 PDF 魔数的伪装文件被拒绝。
        assertThatThrownBy(() -> service.upload(
                // 构造扩展名和 MIME 声称为 PDF、正文却是文本的文件。
                new MockMultipartFile("file", "fake.pdf", "application/pdf", "not-pdf".getBytes(StandardCharsets.UTF_8)),
                // 使用 OTHER 分类避免分类逻辑干扰测试。
                MedicalFileCategory.OTHER
        // 验证异常类型和扩展名不匹配提示。
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("扩展名");

        // 构造业界通用 EICAR 防病毒测试字符串。
        String eicar = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
        // 使用更大上限和独立子目录创建安全扫描测试服务。
        MedicalFileUploadService largerService = service(
                // 隔离危险文件测试的存储目录。
                new LocalMedicalFileStorage(tempDir.resolve("unsafe").toString(), JsonMapper.builder().findAndAddModules().build()),
                // 设置 1024 字节上限，确保测试进入安全扫描分支。
                1024
        );
        // 验证包含 EICAR 标记的文件被基础安全扫描器拒绝。
        assertThatThrownBy(() -> largerService.upload(
                // 将 EICAR 字符串包装为纯文本上传文件。
                new MockMultipartFile("file", "unsafe.txt", "text/plain", eicar.getBytes(StandardCharsets.UTF_8)),
                // 使用 OTHER 分类避免分类逻辑干扰测试。
                MedicalFileCategory.OTHER
        // 验证异常类型和安全扫描提示。
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("安全扫描");
    }

    // 创建测试复用的上传服务。
    private MedicalFileUploadService service(MedicalFileStorage storage, long maxBytes) {
        // 使用真实基础扫描器和调用方提供的存储与大小限制。
        return new MedicalFileUploadService(storage, new BasicMedicalFileSecurityScanner(), maxBytes);
    }
}
