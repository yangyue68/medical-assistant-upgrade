// 声明当前类所在的包，ingestion 表示它属于知识导入和解析流程。
package langchain4j.rag.ingestion;

// MedicalKnowledgeDocument 是项目内部统一医学知识文档模型。
import langchain4j.rag.MedicalKnowledgeDocument;
// MedicalKnowledgeDocumentType 是医学知识文档类型枚举。
import langchain4j.rag.MedicalKnowledgeDocumentType;
// IOException 表示读取本地 HTML 文件失败。
import java.io.IOException;
// StandardCharsets 提供 UTF-8 常量。
import java.nio.charset.StandardCharsets;
// Files 提供本地文件读取和校验能力。
import java.nio.file.Files;
// Path 表示本地 HTML 文件路径。
import java.nio.file.Path;
// LocalDate 表示本项目抓取或整理资料的日期。
import java.time.LocalDate;
// Optional 表示部分协和页面可能没有标准科室介绍正文。
import java.util.Optional;
// Matcher 用于从原始 HTML 注释中的 title 兜底提取科室名。
import java.util.regex.Matcher;
// Pattern 用于编译科室名兜底提取规则。
import java.util.regex.Pattern;
// Jsoup 用于可靠解析 HTML。
import org.jsoup.Jsoup;
// Document 表示 Jsoup 解析后的 HTML 文档。
import org.jsoup.nodes.Document;
// Element 表示 HTML 节点。
import org.jsoup.nodes.Element;

/** 只从已下载的协和科室页面中提取完整“科室简介”区域。 */
public class PumchDepartmentPageHtmlParser {

    // 协和医院来源名称。
    private static final String SOURCE_NAME = "北京协和医院";
    // 协和医院官网根地址，用于拼接科室页面来源 URL。
    private static final String SOURCE_HOST = "https://www.pumch.cn/";
    // 协和旧页面可能把真实 title 注释掉，这里用于兜底提取。
    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>\\s*([^<]+?)\\s*</title>", Pattern.CASE_INSENSITIVE);
    // 少数页面 meta keywords 是全站 SEO 词，需要从简介正文兜底推断科室名。
    private static final Pattern OVERVIEW_DEPARTMENT_PATTERN = Pattern.compile("北京协和医院([^，。\\s]+?)(?:自|是|成立|创建|始建)");

    // 解析一个本地保存的协和科室 HTML 页面。
    public Optional<MedicalKnowledgeDocument> parse(Path htmlFile, LocalDate retrievedAt) {
        // HTML 文件必须存在且是普通文件。
        if (htmlFile == null || !Files.isRegularFile(htmlFile)) {
            // 抛出非法参数异常，提示调用方检查下载目录。
            throw new IllegalArgumentException("PUMCH department page must be an existing regular file.");
        }
        // try 块中读取和解析 HTML，统一包装 IO 异常。
        try {
            // 使用 UTF-8 读取本地页面并交给 Jsoup 解析。
            String rawHtml = Files.readString(htmlFile, StandardCharsets.UTF_8);
            // 使用 UTF-8 读取本地页面并交给 Jsoup 解析。
            Document html = Jsoup.parse(rawHtml);
            // div.ksjj 是协和页面中的科室简介正文区域。
            Element overview = html.selectFirst("div.ksjj");
            // keywords meta 中通常保存科室名称。
            Element departmentMeta = html.selectFirst("meta[name=keywords]");
            // catebynav hidden input 中保存页面分类路径。
            Element categoryPath = html.selectFirst("input#catebynav");
            // 三个关键节点缺一就无法形成可追溯文档。
            if (overview == null || departmentMeta == null || categoryPath == null) {
                // 返回空，表示该页面跳过导入。
                return Optional.empty();
            }
            // 提取科室简介正文。
            String text = overview.text().trim();
            // 提取科室名称，少数页面需要从隐藏 title 或正文兜底。
            String department = extractDepartment(rawHtml, departmentMeta.attr("content"), text);
            // 提取官网分类路径。
            String path = categoryPath.attr("value").trim();
            // 任一关键字段为空时跳过该页面。
            if (text.isEmpty() || department.isEmpty() || path.isEmpty()) {
                // 返回空，避免导入不完整文档。
                return Optional.empty();
            }
            // 转换成统一医学知识文档。
            return Optional.of(toDocument(path, department, text, retrievedAt));
        } catch (IOException exception) {
            // 包装读取失败原因。
            throw new IllegalStateException("Failed to read PUMCH department page: " + htmlFile, exception);
        }
    }

    // 从 meta keywords、隐藏 title 和正文中依次提取可信科室名。
    private String extractDepartment(String rawHtml, String metaKeywords, String overviewText) {
        // 大部分页面的 keywords 就是科室名，可以直接使用。
        String departmentFromMeta = normalizeDepartment(metaKeywords);
        if (!departmentFromMeta.isEmpty() && !looksLikeSeoKeywords(departmentFromMeta)) {
            return departmentFromMeta;
        }
        // department_neikxx.html 的真实 title 被页面注释掉，Jsoup title() 读不到。
        Matcher titleMatcher = TITLE_PATTERN.matcher(rawHtml);
        while (titleMatcher.find()) {
            String title = titleMatcher.group(1).trim();
            int separator = title.indexOf(" - ");
            if (separator > 0) {
                String candidate = normalizeDepartment(title.substring(0, separator));
                if (!candidate.isEmpty() && !looksLikeSeoKeywords(candidate) && !"北京协和医院".equals(candidate)) {
                    return candidate;
                }
            }
        }
        // 最后从科室简介第一句兜底，例如“北京协和医院内科学系自1921年...”
        Matcher overviewMatcher = OVERVIEW_DEPARTMENT_PATTERN.matcher(overviewText);
        if (overviewMatcher.find()) {
            return normalizeDepartment(overviewMatcher.group(1));
        }
        return departmentFromMeta;
    }

    // 归一化页面里抽到的科室名称。
    private String normalizeDepartment(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("科室介绍", "")
                .replace("科室简介", "")
                .trim();
    }

    // 判断 keywords 是否是全站 SEO 关键词，而不是单一科室名。
    private boolean looksLikeSeoKeywords(String value) {
        return value.contains(",")
                || value.contains("，")
                || value.length() > 20
                || value.contains("挂号")
                || value.contains("協和醫院")
                || value.startsWith("协和医院");
    }

    // 将 HTML 页面中的科室简介转换成 HOSPITAL_INFO 文档。
    private MedicalKnowledgeDocument toDocument(String path, String department, String text, LocalDate retrievedAt) {
        // 去掉路径开头斜杠，方便拼接 URL 和生成 ID。
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        // 创建统一文档对象。
        MedicalKnowledgeDocument document = new MedicalKnowledgeDocument();
        // 设置文档 ID，使用路径保证同一页面稳定唯一。
        document.setDocumentId("pumch:department-page:" + normalizedPath.replace('/', '-'));
        // 设置检索展示标题。
        document.setTitle("北京协和医院" + department + "科室介绍");
        // 设置科室简介正文。
        document.setText(text);
        // 设置语言。
        document.setLanguage("Chinese");
        // 设置来源名称。
        document.setSourceName(SOURCE_NAME);
        // 设置来源 URL。
        document.setSourceUrl(SOURCE_HOST + normalizedPath + ".html");
        // 设置文档类型为医院信息。
        document.setDocumentType(MedicalKnowledgeDocumentType.HOSPITAL_INFO);
        // 设置关联科室。
        document.setDepartment(department);
        // 医院信息没有疾病主题时，用科室名作为主题。
        document.setDiseaseOrTopic(department);
        // 设置本项目获取日期。
        document.setRetrievedAt(retrievedAt);
        // 返回文档。
        return document;
    }
}
