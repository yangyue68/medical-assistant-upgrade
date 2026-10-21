// 声明当前接口所在的包，application 表示它属于 RAG 应用编排层。
package langchain4j.rag.application;

// 定义 RAG 回答生成器接口，为本地抽取式回答、在线模型回答和私有化模型回答预留统一边界。
public interface MedicalRagAnswerGenerator {

    // 根据 prompt 和 RAG 上下文生成最终回答。
    String generate(String prompt, MedicalRagContext context);
}
