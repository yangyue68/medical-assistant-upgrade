// 声明测试类所在的包。
package langchain4j.config;

// List 用于接收配置检查返回的告警列表。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;
// ReflectionTestUtils 可以在测试里给 private 字段赋值，避免为了测试暴露 setter。
import org.springframework.test.util.ReflectionTestUtils;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;

// 定义 StartupConfigValidator 的单元测试。
class StartupConfigValidatorTest {

    // 测试关键配置都存在时不应该产生告警。
    @Test
    // 方法名描述测试意图：配置完整时应该返回空告警列表。
    void shouldReturnNoWarningsWhenRequiredConfigExists() {
        // 创建被测配置校验器。
        StartupConfigValidator validator = new StartupConfigValidator();
        // 注入在线模型 API Key。
        ReflectionTestUtils.setField(validator, "qwenApiKey", "qwen-key");
        // 注入 Elasticsearch 地址。
        ReflectionTestUtils.setField(validator, "elasticsearchEndpoint", "https://example.elasticsearch.aliyuncs.com:9200");
        // 注入 Elasticsearch 索引名。
        ReflectionTestUtils.setField(validator, "elasticsearchIndex", "xiaozhi-medical-knowledge");
        // 注入 Elasticsearch 密码。
        ReflectionTestUtils.setField(validator, "elasticsearchPassword", "elastic-password");
        // 注入 MySQL 密码。
        ReflectionTestUtils.setField(validator, "mysqlPassword", "mysql-password");
        // 设置 RAG 回答生成器模式为默认本地抽取式。
        ReflectionTestUtils.setField(validator, "ragAnswerGeneratorMode", "extractive");

        // 执行配置检查。
        List<String> warnings = validator.validate();

        // 验证没有任何告警。
        assertThat(warnings).isEmpty();
    }

    // 测试缺少在线模型 API Key 时会产生告警。
    @Test
    // 方法名描述测试意图：缺 QWEN_API_KEY 时应该提醒。
    void shouldWarnWhenQwenApiKeyIsMissing() {
        // 创建被测配置校验器。
        StartupConfigValidator validator = new StartupConfigValidator();
        // 设置在线模型 API Key 为空。
        ReflectionTestUtils.setField(validator, "qwenApiKey", "");
        // 设置 Elasticsearch 地址，避免这个测试关心知识库告警。
        ReflectionTestUtils.setField(validator, "elasticsearchEndpoint", "https://example.elasticsearch.aliyuncs.com:9200");
        // 设置 Elasticsearch 索引名，避免这个测试关心知识库告警。
        ReflectionTestUtils.setField(validator, "elasticsearchIndex", "xiaozhi-medical-knowledge");
        // 设置 Elasticsearch 密码，避免这个测试关心知识库告警。
        ReflectionTestUtils.setField(validator, "elasticsearchPassword", "elastic-password");
        // 设置 MySQL 密码，避免这个测试关心数据库密码告警。
        ReflectionTestUtils.setField(validator, "mysqlPassword", "mysql-password");
        // 设置 RAG 回答生成器模式为默认本地抽取式。
        ReflectionTestUtils.setField(validator, "ragAnswerGeneratorMode", "extractive");

        // 执行配置检查。
        List<String> warnings = validator.validate();

        // 验证告警中包含 QWEN_API_KEY 缺失信息。
        assertThat(warnings).anyMatch(warning -> warning.contains("QWEN_API_KEY"));
    }

    // 测试缺少 Elasticsearch endpoint 时会提醒开发者真实知识库不可用。
    @Test
    // 方法名描述测试意图：缺 Elasticsearch endpoint 时应该出现知识库告警。
    void shouldWarnWhenElasticsearchEndpointIsMissing() {
        // 创建被测配置校验器。
        StartupConfigValidator validator = new StartupConfigValidator();
        // 设置在线模型 API Key，避免这个测试关心模型告警。
        ReflectionTestUtils.setField(validator, "qwenApiKey", "qwen-key");
        // 设置 Elasticsearch endpoint 为空。
        ReflectionTestUtils.setField(validator, "elasticsearchEndpoint", "");
        // 设置 Elasticsearch 索引名，避免这个测试关心索引名告警。
        ReflectionTestUtils.setField(validator, "elasticsearchIndex", "xiaozhi-medical-knowledge");
        // 设置 Elasticsearch 密码，避免这个测试关心密码告警。
        ReflectionTestUtils.setField(validator, "elasticsearchPassword", "elastic-password");
        // 设置 MySQL 密码，避免这个测试关心数据库密码告警。
        ReflectionTestUtils.setField(validator, "mysqlPassword", "mysql-password");
        // 设置 RAG 回答生成器模式为默认本地抽取式。
        ReflectionTestUtils.setField(validator, "ragAnswerGeneratorMode", "extractive");

        // 执行配置检查。
        List<String> warnings = validator.validate();

        // 验证告警中包含 Elasticsearch endpoint 缺失信息。
        assertThat(warnings).anyMatch(warning -> warning.contains("ALIYUN_ELASTICSEARCH_ENDPOINT"));
    }

    // 测试非法 RAG 回答生成器模式会产生告警。
    @Test
    // 方法名描述测试意图：配置写错时启动检查应该尽早提醒。
    void shouldWarnWhenRagAnswerGeneratorModeIsUnsupported() {
        // 创建被测配置校验器。
        StartupConfigValidator validator = new StartupConfigValidator();
        // 设置在线模型 API Key，避免这个测试关心模型告警。
        ReflectionTestUtils.setField(validator, "qwenApiKey", "qwen-key");
        // 设置 Elasticsearch 地址，避免这个测试关心知识库告警。
        ReflectionTestUtils.setField(validator, "elasticsearchEndpoint", "https://example.elasticsearch.aliyuncs.com:9200");
        // 设置 Elasticsearch 索引名，避免这个测试关心知识库告警。
        ReflectionTestUtils.setField(validator, "elasticsearchIndex", "xiaozhi-medical-knowledge");
        // 设置 Elasticsearch 密码，避免这个测试关心知识库告警。
        ReflectionTestUtils.setField(validator, "elasticsearchPassword", "elastic-password");
        // 设置 MySQL 密码，避免这个测试关心数据库密码告警。
        ReflectionTestUtils.setField(validator, "mysqlPassword", "mysql-password");
        // 设置非法 RAG 回答生成器模式。
        ReflectionTestUtils.setField(validator, "ragAnswerGeneratorMode", "wrong-mode");

        // 执行配置检查。
        List<String> warnings = validator.validate();

        // 验证告警中包含非法模式信息。
        assertThat(warnings).anyMatch(warning -> warning.contains("Unsupported RAG answer generator mode"));
    }

    // 测试在线 RAG 回答生成器缺少 Qwen API Key 时会产生告警。
    @Test
    // 方法名描述测试意图：online 模式必须显式配置在线模型密钥。
    void shouldWarnWhenOnlineRagAnswerGeneratorHasNoQwenApiKey() {
        // 创建被测配置校验器。
        StartupConfigValidator validator = new StartupConfigValidator();
        // 设置在线模型 API Key 为空。
        ReflectionTestUtils.setField(validator, "qwenApiKey", "");
        // 设置 Elasticsearch 地址，避免这个测试关心知识库告警。
        ReflectionTestUtils.setField(validator, "elasticsearchEndpoint", "https://example.elasticsearch.aliyuncs.com:9200");
        // 设置 Elasticsearch 索引名，避免这个测试关心知识库告警。
        ReflectionTestUtils.setField(validator, "elasticsearchIndex", "xiaozhi-medical-knowledge");
        // 设置 Elasticsearch 密码，避免这个测试关心知识库告警。
        ReflectionTestUtils.setField(validator, "elasticsearchPassword", "elastic-password");
        // 设置 MySQL 密码，避免这个测试关心数据库密码告警。
        ReflectionTestUtils.setField(validator, "mysqlPassword", "mysql-password");
        // 设置 RAG 回答生成器为在线模式。
        ReflectionTestUtils.setField(validator, "ragAnswerGeneratorMode", "online");

        // 执行配置检查。
        List<String> warnings = validator.validate();

        // 验证告警中包含 online 模式缺少 QWEN_API_KEY。
        assertThat(warnings).anyMatch(warning -> warning.contains("RAG_ANSWER_GENERATOR_MODE is online"));
    }
}
