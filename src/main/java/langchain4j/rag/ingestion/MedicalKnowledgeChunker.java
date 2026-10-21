// 声明当前类所在的包，ingestion 表示它属于知识导入和预处理流程。
package langchain4j.rag.ingestion;

// MedicalKnowledgeChunk 是 RAG 检索阶段使用的知识片段模型。
import langchain4j.rag.MedicalKnowledgeChunk;
// MedicalKnowledgeDocument 是切分前的完整医学知识文档模型。
import langchain4j.rag.MedicalKnowledgeDocument;
// ArrayList 用来收集切分后的片段。
import java.util.ArrayList;
// Collections 提供空列表等集合工具。
import java.util.Collections;
// List 表示一组文档或片段。
import java.util.List;

// 定义医学知识文档切分器。
public class MedicalKnowledgeChunker {

    // 默认最大片段长度，当前按字符数估算，后续可升级为 tokenizer token 数。
    private static final int DEFAULT_MAX_CHUNK_LENGTH = 800;

    // 默认重叠长度，用于保留相邻片段之间的少量上下文。
    private static final int DEFAULT_OVERLAP_LENGTH = 120;

    // 单个片段最大字符数。
    private final int maxChunkLength;

    // 相邻片段重叠字符数。
    private final int overlapLength;

    // 使用默认参数创建切分器。
    public MedicalKnowledgeChunker() {
        // 委托给带参数构造器。
        this(DEFAULT_MAX_CHUNK_LENGTH, DEFAULT_OVERLAP_LENGTH);
    }

    // 使用自定义参数创建切分器，方便单元测试和后续配置化。
    public MedicalKnowledgeChunker(int maxChunkLength, int overlapLength) {
        // 如果最大长度过小，切出来的片段会失去语义，所以直接拒绝。
        if (maxChunkLength < 100) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("maxChunkLength must be at least 100.");
        }
        // 如果重叠长度小于 0，说明参数无效。
        if (overlapLength < 0) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("overlapLength must not be negative.");
        }
        // 如果重叠长度大于等于最大长度，切分时可能无法向前推进。
        if (overlapLength >= maxChunkLength) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("overlapLength must be smaller than maxChunkLength.");
        }
        // 保存最大片段长度。
        this.maxChunkLength = maxChunkLength;
        // 保存重叠长度。
        this.overlapLength = overlapLength;
    }

    // 批量切分多篇医学知识文档。
    public List<MedicalKnowledgeChunk> splitAll(List<MedicalKnowledgeDocument> documents) {
        // 如果文档列表为空，直接返回空列表。
        if (documents == null || documents.isEmpty()) {
            // 返回不可变空列表。
            return Collections.emptyList();
        }
        // 创建片段列表。
        List<MedicalKnowledgeChunk> chunks = new ArrayList<>();
        // 遍历每篇文档。
        for (MedicalKnowledgeDocument document : documents) {
            // 切分当前文档并追加到总列表。
            chunks.addAll(split(document));
        }
        // 返回所有片段。
        return chunks;
    }

    // 切分单篇医学知识文档。
    public List<MedicalKnowledgeChunk> split(MedicalKnowledgeDocument document) {
        // 如果文档为空，直接返回空列表。
        if (document == null) {
            // 返回不可变空列表。
            return Collections.emptyList();
        }
        // 规整正文，避免 null 和多余空白影响切分。
        String normalizedText = normalizeText(document.getText());
        // 如果正文为空，说明没有可入库内容。
        if (normalizedText.isEmpty()) {
            // 返回不可变空列表。
            return Collections.emptyList();
        }
        // 创建片段列表。
        List<MedicalKnowledgeChunk> chunks = new ArrayList<>();
        // 初始化片段序号。
        int chunkIndex = 0;
        // 初始化当前切分起点。
        int start = 0;
        // 只要起点还没到正文末尾，就继续切分。
        while (start < normalizedText.length()) {
            // 计算本轮理想结束位置。
            int idealEnd = Math.min(start + maxChunkLength, normalizedText.length());
            // 寻找更自然的结束位置。
            int end = chooseBoundary(normalizedText, start, idealEnd);
            // 截取片段文本。
            String chunkText = normalizedText.substring(start, end).trim();
            // 如果片段文本不为空，就创建片段对象。
            if (!chunkText.isEmpty()) {
                // 把片段加入结果列表。
                chunks.add(MedicalKnowledgeChunk.from(document, chunkIndex, chunkText));
                // 片段序号递增。
                chunkIndex++;
            }
            // 如果已经切到正文末尾，就结束循环。
            if (end >= normalizedText.length()) {
                // 跳出循环。
                break;
            }
            // 下一段优先从句子边界开始，避免 overlap 把片段切成半句话。
            start = chooseNextStart(normalizedText, start, end);
        }
        // 返回切分后的片段列表。
        return chunks;
    }

    // 规整正文文本。
    private String normalizeText(String text) {
        // 如果文本为空，返回空字符串。
        if (text == null) {
            // 返回空文本。
            return "";
        }
        // 把连续空白压缩成一个空格，并去掉首尾空白。
        return text.replaceAll("\\s+", " ").trim();
    }

    // 在理想结束位置附近寻找自然边界。
    private int chooseBoundary(String text, int start, int idealEnd) {
        // 如果理想结束位置已经到达末尾，直接返回末尾。
        if (idealEnd >= text.length()) {
            // 返回文本长度。
            return text.length();
        }
        // 计算允许回看的最早位置，避免为了找标点导致片段过短。
        int minBoundary = Math.max(start + maxChunkLength / 2, start + 1);
        // 从理想结束位置向前寻找句号、问号、感叹号、分号等句子边界。
        for (int i = idealEnd; i >= minBoundary; i--) {
            // 读取当前位置前一个字符。
            char current = text.charAt(i - 1);
            // 如果当前字符是自然断句标点，就在这里结束。
            if (isSentenceBoundary(current)) {
                // 返回边界位置。
                return i;
            }
        }
        // 如果没找到句子边界，再寻找空格边界，避免切断英文单词。
        for (int i = idealEnd; i >= minBoundary; i--) {
            // 如果当前位置前一个字符是空白，就在这里结束。
            if (Character.isWhitespace(text.charAt(i - 1))) {
                // 返回边界位置。
                return i;
            }
        }
        // 如果没有更好的边界，就使用理想结束位置。
        return idealEnd;
    }

    // 在当前片段结束位置附近寻找下一片段的自然起点。
    private int chooseNextStart(String text, int previousStart, int previousEnd) {
        // 没有重叠时直接从上一片段结束处继续。
        if (overlapLength == 0) {
            return previousEnd;
        }
        // 先计算原始 overlap 起点。
        int rawStart = Math.max(previousEnd - overlapLength, previousStart + 1);
        // 优先向后找到句子结尾，这样下一段从完整新句子开始。
        for (int i = rawStart; i < previousEnd; i++) {
            if (isSentenceBoundary(text.charAt(i))) {
                return skipWhitespace(text, i + 1);
            }
        }
        // 如果向后找不到，再向前找上一个句子结尾，保留完整句子作为上下文。
        for (int i = rawStart - 1; i > previousStart; i--) {
            if (isSentenceBoundary(text.charAt(i))) {
                return skipWhitespace(text, i + 1);
            }
        }
        // 英文或列表文本中尽量从空格后开始，避免切断单词。
        for (int i = rawStart; i < previousEnd; i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return skipWhitespace(text, i + 1);
            }
        }
        // 实在找不到自然边界时才退回原始 overlap 起点。
        return rawStart;
    }

    // 跳过边界后的空白字符。
    private int skipWhitespace(String text, int index) {
        // 初始化当前位置。
        int current = index;
        // 跳过连续空白。
        while (current < text.length() && Character.isWhitespace(text.charAt(current))) {
            current++;
        }
        // 返回第一个非空白位置。
        return current;
    }

    // 判断字符是否是适合断句的标点。
    private boolean isSentenceBoundary(char current) {
        // 返回当前字符是否属于中英文常见句子边界。
        return current == '.'
                || current == '!'
                || current == '?'
                || current == ';'
                || current == '。'
                || current == '！'
                || current == '？'
                || current == '；';
    }
}
