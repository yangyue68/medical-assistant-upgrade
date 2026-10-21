// 声明测试类所在的包。
package langchain4j.rag.embedding;

// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// AssertJ 异常断言工具。
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 定义确定性 embedding 模型的单元测试。
class DeterministicMedicalKnowledgeEmbeddingModelTest {

    // 测试同一文本每次都会生成同一个向量。
    @Test
    // 方法名描述测试意图：确定性 embedding 必须稳定可复现。
    void shouldCreateStableEmbeddingForSameText() {
        // 创建确定性 embedding 模型。
        DeterministicMedicalKnowledgeEmbeddingModel embeddingModel = new DeterministicMedicalKnowledgeEmbeddingModel(64);

        // 对同一文本生成第一份向量。
        double[] firstVector = embeddingModel.embed("A1C blood sugar diabetes");
        // 对同一文本生成第二份向量。
        double[] secondVector = embeddingModel.embed("A1C blood sugar diabetes");

        // 验证向量维度正确。
        assertThat(firstVector).hasSize(64);
        // 验证两次向量完全一致。
        assertThat(firstVector).containsExactly(secondVector);
    }

    // 测试相近文本的相似度高于无关文本。
    @Test
    // 方法名描述测试意图：共享关键词越多，确定性向量越相似。
    void shouldMakeRelatedTextMoreSimilarThanUnrelatedText() {
        // 创建确定性 embedding 模型。
        DeterministicMedicalKnowledgeEmbeddingModel embeddingModel = new DeterministicMedicalKnowledgeEmbeddingModel(128);
        // 创建查询向量。
        double[] queryVector = embeddingModel.embed("A1C blood sugar");
        // 创建相关文本向量。
        double[] relatedVector = embeddingModel.embed("A1C is a blood test that measures average blood sugar.");
        // 创建无关文本向量。
        double[] unrelatedVector = embeddingModel.embed("Asthma can cause wheezing and shortness of breath.");

        // 验证相关文本相似度更高。
        assertThat(cosine(queryVector, relatedVector)).isGreaterThan(cosine(queryVector, unrelatedVector));
    }

    // 测试非法维度会被拒绝。
    @Test
    // 方法名描述测试意图：向量维度太小会导致哈希冲突严重。
    void shouldRejectTooSmallDimension() {
        // 验证过小维度会抛出非法参数异常。
        assertThatThrownBy(() -> new DeterministicMedicalKnowledgeEmbeddingModel(8))
                // 验证异常类型。
                .isInstanceOf(IllegalArgumentException.class)
                // 验证异常信息。
                .hasMessageContaining("dimension");
    }

    // 计算测试用余弦相似度。
    private static double cosine(double[] left, double[] right) {
        // 初始化点积。
        double dotProduct = 0.0;
        // 初始化左向量平方和。
        double leftSquaredSum = 0.0;
        // 初始化右向量平方和。
        double rightSquaredSum = 0.0;
        // 遍历向量维度。
        for (int i = 0; i < left.length; i++) {
            // 累加点积。
            dotProduct += left[i] * right[i];
            // 累加左向量平方和。
            leftSquaredSum += left[i] * left[i];
            // 累加右向量平方和。
            rightSquaredSum += right[i] * right[i];
        }
        // 返回余弦相似度。
        return dotProduct / (Math.sqrt(leftSquaredSum) * Math.sqrt(rightSquaredSum));
    }
}
