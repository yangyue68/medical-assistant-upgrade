// 声明当前接口所在的包，embedding 表示它负责文本向量化。
package langchain4j.rag.embedding;

// ArrayList 用于默认批量实现按顺序收集多个向量。
import java.util.ArrayList;
// List 表示一批待向量化文本或一批向量结果。
import java.util.List;

// 定义医学知识 embedding 模型接口，为在线 embedding、私有化 embedding 和测试 embedding 预留统一边界。
public interface MedicalKnowledgeEmbeddingModel {

    // 把输入文本转换成向量。
    double[] embed(String text);

    // 批量把文本转换成向量；实现类可以覆盖该方法以调用模型的批量 API。
    default List<double[]> embedAll(List<String> texts) {
        // 创建与输入顺序一致的结果列表。
        List<double[]> embeddings = new ArrayList<>();
        // 空输入直接返回空列表。
        if (texts == null || texts.isEmpty()) {
            // 返回空结果。
            return embeddings;
        }
        // 默认逐条调用单文本接口，保证现有测试模型仍然兼容。
        for (String text : texts) {
            // 生成并保存当前文本向量。
            embeddings.add(embed(text));
        }
        // 返回全部向量。
        return embeddings;
    }

    // 返回向量维度，便于向量存储做维度校验。
    int dimension();
}
