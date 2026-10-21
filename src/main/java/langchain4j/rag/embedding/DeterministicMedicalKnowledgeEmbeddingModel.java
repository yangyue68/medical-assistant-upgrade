// 声明当前类所在的包，embedding 表示它负责文本向量化。
package langchain4j.rag.embedding;

// Locale 用来做稳定的小写转换。
import java.util.Locale;
// Pattern 用来编译查询词切分正则。
import java.util.regex.Pattern;

// 定义确定性医学知识 embedding 模型，主要用于本地测试和离线开发。
public class DeterministicMedicalKnowledgeEmbeddingModel implements MedicalKnowledgeEmbeddingModel {

    // 默认向量维度。
    private static final int DEFAULT_DIMENSION = 128;

    // 查询词切分正则；保留字母、数字和中日韩字符，其余符号都作为分隔符。
    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHan}]+");

    // 当前模型输出的向量维度。
    private final int dimension;

    // 使用默认维度创建确定性 embedding 模型。
    public DeterministicMedicalKnowledgeEmbeddingModel() {
        // 委托给带维度的构造器。
        this(DEFAULT_DIMENSION);
    }

    // 使用自定义维度创建确定性 embedding 模型。
    public DeterministicMedicalKnowledgeEmbeddingModel(int dimension) {
        // 如果维度太小，哈希冲突会很严重，所以直接拒绝。
        if (dimension < 16) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("dimension must be at least 16.");
        }
        // 保存向量维度。
        this.dimension = dimension;
    }

    // 把文本转换成确定性向量。
    @Override
    public double[] embed(String text) {
        // 创建零向量。
        double[] vector = new double[dimension];
        // 规整输入文本。
        String normalizedText = normalize(text);
        // 如果文本为空，直接返回零向量。
        if (normalizedText.isEmpty()) {
            // 返回零向量。
            return vector;
        }
        // 按分隔符切分文本。
        String[] tokens = TOKEN_SPLITTER.split(normalizedText);
        // 遍历英文、数字和连续中文 token。
        for (String token : tokens) {
            // 添加 token 到向量中。
            addToken(vector, token);
        }
        // 为中文文本额外加入单字 token，避免没有中文分词器时完全没有细粒度匹配。
        addChineseCharacters(vector, normalizedText);
        // 对向量做 L2 归一化，让余弦相似度更稳定。
        normalizeVector(vector);
        // 返回归一化后的向量。
        return vector;
    }

    // 返回当前向量维度。
    @Override
    public int dimension() {
        // 返回维度字段。
        return dimension;
    }

    // 规整输入文本。
    private String normalize(String text) {
        // 如果文本为空，返回空字符串。
        if (text == null) {
            // 返回空文本。
            return "";
        }
        // 转小写、压缩空白、去掉首尾空白。
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    // 把一个 token 写入向量。
    private void addToken(double[] vector, String rawToken) {
        // 如果 token 为空，直接跳过。
        if (rawToken == null) {
            // 结束当前 token 处理。
            return;
        }
        // 去掉 token 首尾空白。
        String token = rawToken.trim();
        // 如果 token 太短，跳过常见噪声词。
        if (token.length() < 2) {
            // 结束当前 token 处理。
            return;
        }
        // 计算 token 映射到的向量下标。
        int index = Math.floorMod(token.hashCode(), dimension);
        // 在对应下标累加权重。
        vector[index] += 1.0;
    }

    // 为中文文本添加单字级特征。
    private void addChineseCharacters(double[] vector, String normalizedText) {
        // 遍历文本中的每个字符。
        for (int i = 0; i < normalizedText.length(); i++) {
            // 读取当前字符。
            char current = normalizedText.charAt(i);
            // 如果当前字符是汉字，就作为一个 token 加入向量。
            if (Character.UnicodeScript.of(current) == Character.UnicodeScript.HAN) {
                // 计算汉字映射到的向量下标。
                int index = Math.floorMod(Character.hashCode(current), dimension);
                // 在对应下标累加较小权重。
                vector[index] += 0.5;
            }
        }
    }

    // 对向量做 L2 归一化。
    private void normalizeVector(double[] vector) {
        // 初始化平方和。
        double squaredSum = 0.0;
        // 遍历向量中的每个值。
        for (double value : vector) {
            // 累加平方。
            squaredSum += value * value;
        }
        // 如果平方和为 0，说明是零向量。
        if (squaredSum == 0.0) {
            // 不需要归一化。
            return;
        }
        // 计算向量长度。
        double norm = Math.sqrt(squaredSum);
        // 遍历向量下标。
        for (int i = 0; i < vector.length; i++) {
            // 把每个维度除以向量长度。
            vector[i] = vector[i] / norm;
        }
    }
}
