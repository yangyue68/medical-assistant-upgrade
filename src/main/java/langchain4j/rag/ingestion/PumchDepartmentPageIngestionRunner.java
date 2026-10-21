// 声明当前类所在的包，ingestion 表示它属于知识导入流程。
package langchain4j.rag.ingestion;

// MedicalKnowledgeDocument 是解析后的统一医学知识文档模型。
import langchain4j.rag.MedicalKnowledgeDocument;
// IOException 表示遍历本地协和页面目录失败。
import java.io.IOException;
// Files 提供目录遍历和文件类型判断。
import java.nio.file.Files;
// Path 表示协和页面下载目录。
import java.nio.file.Path;
// LocalDate 表示本项目抓取或整理资料的日期。
import java.time.LocalDate;
// List 表示待导入文档列表。
import java.util.List;
// Optional 表示单个 HTML 页面可能解析不出有效文档。
import java.util.Optional;
// Stream 表示 Files.list 返回的路径流。
import java.util.stream.Stream;

// 批量导入已下载的协和医院科室 HTML 页面。
public class PumchDepartmentPageIngestionRunner {

    // 协和 HTML 页面解析器。
    private final PumchDepartmentPageHtmlParser parser;
    // 统一医学知识导入服务。
    private final MedicalKnowledgeIngestionService ingestionService;

    // 创建协和科室页面导入 runner。
    public PumchDepartmentPageIngestionRunner(
            // 注入 HTML 解析器。
            PumchDepartmentPageHtmlParser parser,
            // 注入导入服务。
            MedicalKnowledgeIngestionService ingestionService
    ) {
        // 保存解析器。
        this.parser = parser;
        // 保存导入服务。
        this.ingestionService = ingestionService;
    }

    // 导入指定目录下的协和科室页面。
    public MedicalKnowledgeIngestionResult ingest(Path directory, LocalDate retrievedAt) {
        // 目录必须存在且是目录。
        if (directory == null || !Files.isDirectory(directory)) {
            // 抛出非法参数异常，提示下载目录配置错误。
            throw new IllegalArgumentException("PUMCH department page directory must exist: " + directory);
        }
        // 使用 try-with-resources 关闭目录流。
        try (Stream<Path> paths = Files.list(directory)) {
            // 解析目录中的所有 HTML 页面。
            List<MedicalKnowledgeDocument> documents = paths
                    // 只处理普通文件。
                    .filter(Files::isRegularFile)
                    // 只处理 .html 文件。
                    .filter(path -> path.getFileName().toString().endsWith(".html"))
                    // 按路径排序，保证导入顺序稳定。
                    .sorted()
                    // 解析单个页面。
                    .map(path -> parser.parse(path, retrievedAt))
                    // 去掉解析失败或缺少正文的页面。
                    .flatMap(Optional::stream)
                    // 收集为文档列表。
                    .toList();
            // 调用统一导入服务完成切分和保存。
            return ingestionService.ingestDocuments("北京协和医院科室页面", documents);
        } catch (IOException exception) {
            // 包装目录遍历异常。
            throw new IllegalStateException("Failed to list PUMCH department pages: " + directory, exception);
        }
    }
}
