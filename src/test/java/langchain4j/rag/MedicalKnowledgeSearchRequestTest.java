// 声明测试类所在的包。
package langchain4j.rag;

// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;

// 定义医学知识检索请求对象的单元测试。
class MedicalKnowledgeSearchRequestTest {

    // 测试默认检索参数。
    @Test
    // 方法名描述测试意图：缺省参数应该规整成安全默认值。
    void shouldNormalizeDefaults() {
        // 创建检索请求。
        MedicalKnowledgeSearchRequest request = new MedicalKnowledgeSearchRequest();
        // 设置查询问题。
        request.setQuery("What is A1C?");

        // 验证默认 topK 为 5。
        assertThat(request.normalizedTopK()).isEqualTo(5);
        // 验证默认检索模式为 HYBRID。
        assertThat(request.normalizedRetrievalMode()).isEqualTo(RetrievalMode.HYBRID);
        // 验证请求校验通过。
        assertThat(request.validate()).isEmpty();
    }

    // 测试 topK 边界。
    @Test
    // 方法名描述测试意图：topK 应该限制在 1 到 20。
    void shouldNormalizeTopKBounds() {
        // 创建检索请求。
        MedicalKnowledgeSearchRequest request = new MedicalKnowledgeSearchRequest();
        // 设置过小 topK。
        request.setTopK(0);
        // 验证会被规整成 1。
        assertThat(request.normalizedTopK()).isEqualTo(1);

        // 设置过大 topK。
        request.setTopK(100);
        // 验证会被规整成 20。
        assertThat(request.normalizedTopK()).isEqualTo(20);
    }

    // 测试空查询会被拒绝。
    @Test
    // 方法名描述测试意图：检索问题不能为空。
    void shouldRejectBlankQuery() {
        // 创建检索请求。
        MedicalKnowledgeSearchRequest request = new MedicalKnowledgeSearchRequest();
        // 设置空白查询。
        request.setQuery(" ");

        // 验证校验错误包含查询不能为空。
        assertThat(request.validate()).contains("检索问题不能为空。");
    }
}
