// 声明当前类所在的包，application 表示它属于 RAG 应用编排层。
package langchain4j.rag.application;

// Logger 是 SLF4J 的日志接口。
import org.slf4j.Logger;
// LoggerFactory 用来创建当前类对应的日志对象。
import org.slf4j.LoggerFactory;

// 定义带兜底能力的 RAG 回答生成器。
public class FallbackMedicalRagAnswerGenerator implements MedicalRagAnswerGenerator {

    // 创建当前类的日志对象。
    private static final Logger log = LoggerFactory.getLogger(FallbackMedicalRagAnswerGenerator.class);

    // 保存优先使用的回答生成器。
    private final MedicalRagAnswerGenerator primaryGenerator;

    // 保存兜底回答生成器。
    private final MedicalRagAnswerGenerator fallbackGenerator;

    // 创建带兜底能力的回答生成器。
    public FallbackMedicalRagAnswerGenerator(
            // 优先生成器，在线模式下通常是 OnlineMedicalRagAnswerGenerator。
            MedicalRagAnswerGenerator primaryGenerator,
            // 兜底生成器，在线失败时通常退回 ExtractiveMedicalRagAnswerGenerator。
            MedicalRagAnswerGenerator fallbackGenerator
    ) {
        // 校验优先生成器不能为空。
        if (primaryGenerator == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("primaryGenerator must not be null.");
        }
        // 校验兜底生成器不能为空。
        if (fallbackGenerator == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("fallbackGenerator must not be null.");
        }
        // 保存优先生成器。
        this.primaryGenerator = primaryGenerator;
        // 保存兜底生成器。
        this.fallbackGenerator = fallbackGenerator;
    }

    // 先尝试优先生成器，失败时自动退回兜底生成器。
    @Override
    public String generate(String prompt, MedicalRagContext context) {
        // 使用 try 捕获在线模型调用、网络、限流等异常。
        try {
            // 调用优先生成器生成回答。
            return primaryGenerator.generate(prompt, context);
        } catch (RuntimeException exception) {
            // 记录 warn 日志，避免在线生成失败变成静默问题。
            log.warn("Primary RAG answer generator failed. Falling back to local generator. reason={}", exception.getMessage());
            // 调用兜底生成器生成回答。
            return fallbackGenerator.generate(prompt, context);
        }
    }
}
