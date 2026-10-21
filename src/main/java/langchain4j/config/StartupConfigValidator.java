// 声明当前类位于 config 包，因为它负责应用启动配置检查。
package langchain4j.config;

// ArrayList 用来收集多个配置告警。
import java.util.ArrayList;
// List 用来对外暴露只关心顺序的一组检查结果。
import java.util.List;

// ApplicationArguments 表示 Spring Boot 启动参数。
import org.springframework.boot.ApplicationArguments;
// ApplicationRunner 会在 Spring Boot 启动完成后自动执行。
import org.springframework.boot.ApplicationRunner;
// Value 用于读取 application.properties 或环境变量中的配置。
import org.springframework.beans.factory.annotation.Value;
// Component 表示把当前类注册成 Spring Bean。
import org.springframework.stereotype.Component;
// StringUtils 提供字符串判空工具。
import org.springframework.util.StringUtils;
// Logger 是 SLF4J 日志接口。
import org.slf4j.Logger;
// LoggerFactory 用来创建日志对象。
import org.slf4j.LoggerFactory;

// 标记为 Spring 组件，应用启动时会自动创建这个 Bean。
@Component
// 定义启动配置校验器，集中检查关键配置是否缺失或不合理。
public class StartupConfigValidator implements ApplicationRunner {

    // 创建当前类的日志对象。
    private static final Logger log = LoggerFactory.getLogger(StartupConfigValidator.class);

    // 读取是否启用启动配置检查；默认启用。
    @Value("${xiaozhi.startup.validation.enabled:true}")
    // enabled=false 时，启动阶段不会执行配置检查。
    private boolean enabled;

    // 读取是否遇到配置问题就直接启动失败；默认 false，方便本地开发。
    @Value("${xiaozhi.startup.validation.fail-fast:false}")
    // failFast=true 适合生产环境，能避免服务带病启动。
    private boolean failFast;

    // 读取在线模型 API Key；这里兼容新旧两个环境变量名。
    @Value("${QWEN_API_KEY:${QWEN_APIKEY:}}")
    // qwenApiKey 缺失时，聊天和 embedding 能力通常不可用。
    private String qwenApiKey;

    // 读取阿里云 Elasticsearch 地址。
    @Value("${xiaozhi.elasticsearch.endpoint:}")
    // elasticsearchEndpoint 为空时，真实 RAG 知识库无法访问。
    private String elasticsearchEndpoint;

    // 读取阿里云 Elasticsearch 索引名。
    @Value("${xiaozhi.elasticsearch.index:}")
    // elasticsearchIndex 为空时，知识库索引无法创建或查询。
    private String elasticsearchIndex;

    // 读取阿里云 Elasticsearch 密码。
    @Value("${xiaozhi.elasticsearch.password:}")
    // elasticsearchPassword 为空时，带认证的阿里云 Elasticsearch 无法访问。
    private String elasticsearchPassword;

    // 读取 MySQL 密码；本地没有密码时可能为空，生产环境不建议为空。
    @Value("${spring.datasource.password:}")
    // mysqlPassword 用于检查业务数据库配置是否像生产配置。
    private String mysqlPassword;

    // 读取 RAG 回答生成器模式；默认 extractive，只有 online 才调用在线模型。
    @Value("${xiaozhi.rag.answer-generator.mode:extractive}")
    // ragAnswerGeneratorMode 控制 RAG answer 接口使用本地抽取式生成还是在线模型生成。
    private String ragAnswerGeneratorMode;

    // Spring Boot 启动完成后会调用 run 方法。
    @Override
    // ApplicationArguments 参数包含命令行启动参数，本方法暂时不需要使用它。
    public void run(ApplicationArguments args) {
        // 如果配置关闭了启动检查，就直接返回。
        if (!enabled) {
            // debug 级别说明检查被跳过，不污染正常启动日志。
            log.debug("Startup config validation is disabled.");
            // 结束启动检查。
            return;
        }

        // 执行配置检查并拿到告警列表。
        List<String> warnings = validate();
        // 如果没有告警，说明关键配置看起来正常。
        if (warnings.isEmpty()) {
            // info 日志告诉开发者启动配置检查通过。
            log.info("Startup config validation passed.");
            // 结束启动检查。
            return;
        }

        // 遍历所有告警，逐条输出到日志。
        for (String warning : warnings) {
            // warn 日志表示当前配置可能影响部分功能，但默认不阻塞启动。
            log.warn("Startup config warning: {}", warning);
        }

        // 如果开启 fail-fast，发现告警后直接抛异常阻止应用继续运行。
        if (failFast) {
            // 抛异常让启动失败，避免生产环境缺关键配置还继续提供服务。
            throw new IllegalStateException("Startup config validation failed: " + String.join("; ", warnings));
        }
    }

    // 对外暴露配置检查逻辑，Controller 和测试都可以复用。
    public List<String> validate() {
        // 创建可变列表，用于收集所有检查出来的问题。
        List<String> warnings = new ArrayList<>();

        // 检查在线模型 API Key 是否为空。
        if (!StringUtils.hasText(qwenApiKey)) {
            // 添加模型 Key 缺失告警。
            warnings.add("QWEN_API_KEY is missing. Online chat and embedding calls may fail.");
        }

        // 检查 Elasticsearch endpoint 是否为空。
        if (!StringUtils.hasText(elasticsearchEndpoint)) {
            // 添加 Elasticsearch endpoint 缺失告警。
            warnings.add("ALIYUN_ELASTICSEARCH_ENDPOINT is missing. Medical RAG keyword/vector/hybrid retrieval cannot use Elasticsearch.");
        }

        // 检查 Elasticsearch 索引名是否为空。
        if (!StringUtils.hasText(elasticsearchIndex)) {
            // 添加 Elasticsearch 索引名缺失告警。
            warnings.add("ALIYUN_ELASTICSEARCH_INDEX is missing. Medical knowledge index name must be configured.");
        }

        // 检查 Elasticsearch 密码是否为空。
        if (!StringUtils.hasText(elasticsearchPassword)) {
            // 添加 Elasticsearch 密码提醒。
            warnings.add("ALIYUN_ELASTICSEARCH_PASSWORD is empty. This is acceptable only if your Elasticsearch instance allows unauthenticated access.");
        }

        // 把 RAG 回答生成器模式规整成小写。
        String answerMode = ragAnswerGeneratorMode == null ? "extractive" : ragAnswerGeneratorMode.trim().toLowerCase();
        // 检查 RAG 回答生成器模式是否属于支持范围。
        if (!"extractive".equals(answerMode) && !"online".equals(answerMode)) {
            // 添加 RAG 回答生成器模式非法告警。
            warnings.add("Unsupported RAG answer generator mode: " + ragAnswerGeneratorMode + ".");
        }

        // 如果使用在线 RAG 回答生成器，就必须检查在线模型 API Key。
        if ("online".equals(answerMode) && !StringUtils.hasText(qwenApiKey)) {
            // 添加在线 RAG 回答生成器缺少 API Key 的告警。
            warnings.add("QWEN_API_KEY is required when RAG_ANSWER_GENERATOR_MODE is online.");
        }

        // 检查 MySQL 密码是否为空。
        if (!StringUtils.hasText(mysqlPassword)) {
            // 添加数据库密码为空提醒，本地可以为空，但生产环境不推荐。
            warnings.add("MYSQL_PASSWORD is empty. This is acceptable locally but unsafe for production.");
        }

        // 返回收集到的所有告警。
        return warnings;
    }

    // 返回是否启用了启动检查，方便 readiness 接口展示。
    public boolean isEnabled() {
        // 返回 enabled 字段当前值。
        return enabled;
    }

    // 返回是否开启 fail-fast，方便 readiness 接口展示。
    public boolean isFailFast() {
        // 返回 failFast 字段当前值。
        return failFast;
    }
}
