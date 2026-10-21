// 声明当前类所在的包，embedding 表示它属于向量化模型适配层。
package langchain4j.rag.embedding;

// Embedding 是 LangChain4j 返回的向量结果对象。
import dev.langchain4j.data.embedding.Embedding;
// TextSegment 是 LangChain4j 批量 embedding 接口接收的文本片段。
import dev.langchain4j.data.segment.TextSegment;
// EmbeddingModel 是 LangChain4j 的通用 embedding 模型接口。
import dev.langchain4j.model.embedding.EmbeddingModel;
// ArrayList 用于构建批量请求和返回结果。
import java.util.ArrayList;
// List 表示一批文本、片段或向量。
import java.util.List;

/**
 * 将 LangChain4j 的在线 embedding 模型适配成本项目自己的医学知识 embedding 接口。
 */
public class LangChain4jMedicalKnowledgeEmbeddingModel implements MedicalKnowledgeEmbeddingModel {

    // delegate 保存真正执行远程或在线 embedding 的 LangChain4j 模型。
    private final EmbeddingModel delegate;

    // 构造适配器时注入底层 LangChain4j embedding 模型。
    public LangChain4jMedicalKnowledgeEmbeddingModel(EmbeddingModel delegate) {
        // 如果底层模型为空，后续无法生成向量。
        if (delegate == null) {
            // 抛出非法参数异常，尽早暴露配置错误。
            throw new IllegalArgumentException("delegate embeddingModel must not be null.");
        }
        // 保存底层模型引用。
        this.delegate = delegate;
    }

    // 根据输入文本生成 double[] 向量，满足项目内部接口约定。
    @Override
    public double[] embed(String text) {
        // 空文本没有检索语义，不能提交给 embedding 模型。
        if (text == null || text.trim().isEmpty()) {
            // 抛出非法参数异常，避免产生无意义向量。
            throw new IllegalArgumentException("embedding text must not be blank.");
        }
        // 调用 LangChain4j 模型并取得向量内容。
        Embedding embedding = delegate.embed(text.trim()).content();
        // 校验远程模型确实返回了非空向量。
        if (embedding == null || embedding.vector() == null || embedding.vector().length == 0) {
            // 抛出状态异常，说明外部模型返回结果异常。
            throw new IllegalStateException("Online embedding model returned an empty vector.");
        }
        // 读取 LangChain4j 使用的 float 向量。
        float[] source = embedding.vector();
        // 创建项目内部使用的 double 向量。
        double[] vector = new double[source.length];
        // 遍历每个维度完成 float 到 double 的转换。
        for (int index = 0; index < source.length; index++) {
            // 将当前维度复制到目标数组。
            vector[index] = source[index];
        }
        // 返回转换后的向量。
        return vector;
    }

    // 使用底层模型的批量 API 一次处理多条文本，减少远程请求次数。
    @Override
    public List<double[]> embedAll(List<String> texts) {
        // 空列表没有需要处理的内容。
        if (texts == null || texts.isEmpty()) {
            // 返回不可变空列表。
            return List.of();
        }
        // 创建 LangChain4j 批量请求片段。
        List<TextSegment> segments = new ArrayList<>();
        // 遍历并校验每条文本。
        for (String text : texts) {
            // 空文本不能提交给在线模型。
            if (text == null || text.trim().isEmpty()) {
                // 抛出参数异常，避免输入与输出顺序错位。
                throw new IllegalArgumentException("embedding text must not be blank.");
            }
            // 添加规整后的文本片段。
            segments.add(TextSegment.from(text.trim()));
        }
        // 一次调用底层批量 embedding API。
        List<Embedding> sourceEmbeddings = delegate.embedAll(segments).content();
        // 模型必须为每条输入返回一条向量。
        if (sourceEmbeddings == null || sourceEmbeddings.size() != texts.size()) {
            // 抛出状态异常，防止向量与 chunk 错配。
            throw new IllegalStateException("Online embedding model returned an unexpected batch size.");
        }
        // 创建项目内部 double[] 结果列表。
        List<double[]> embeddings = new ArrayList<>();
        // 按返回顺序转换每一条向量。
        for (Embedding sourceEmbedding : sourceEmbeddings) {
            // 校验当前向量非空。
            if (sourceEmbedding == null || sourceEmbedding.vector() == null || sourceEmbedding.vector().length == 0) {
                // 拒绝空向量，避免写入无效索引。
                throw new IllegalStateException("Online embedding model returned an empty vector.");
            }
            // 读取 float 向量。
            float[] source = sourceEmbedding.vector();
            // 创建 double 向量。
            double[] vector = new double[source.length];
            // 逐维复制。
            for (int index = 0; index < source.length; index++) {
                // 保存当前维度。
                vector[index] = source[index];
            }
            // 添加转换后的向量。
            embeddings.add(vector);
        }
        // 返回批量向量。
        return embeddings;
    }

    // 返回底层 embedding 模型的向量维度，供 Elasticsearch 建索引和校验使用。
    @Override
    public int dimension() {
        // 委托给 LangChain4j 模型。
        return delegate.dimension();
    }
}
