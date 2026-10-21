// 声明当前类所在的包，application 表示它属于 RAG 应用编排层。
package langchain4j.rag.application;

// 定义 RAG prompt 构建器，负责把 RAG 上下文和用户问题组装成模型输入。
public class MedicalRagPromptBuilder {

    // 构建生成回答所需 prompt。
    public String build(MedicalRagContext context) {
        // 如果上下文为空，说明调用方没有先完成检索。
        if (context == null) {
            // 抛出非法参数异常。
            throw new IllegalArgumentException("context must not be null.");
        }
        // 创建 prompt 构建器。
        StringBuilder builder = new StringBuilder();
        // 添加角色要求。
        builder.append("你是一个严谨的医疗健康助手。\n");
        // 添加安全提示。
        builder.append("请遵守以下安全边界：").append(context.getSafetyInstruction()).append("\n\n");
        // 添加引用要求。
        builder.append("回答要求：\n");
        // 要求基于给定上下文回答。
        builder.append("1. 优先依据【RAG 检索上下文】回答，不要编造不存在的来源。\n");
        // 要求使用引用编号。
        builder.append("2. 如果使用某个片段的信息，请在相关句子后标注引用编号，例如 [1]。\n");
        // 要求资料不足时说明不足。
        builder.append("3. 如果上下文资料不足，请明确说明资料不足，并建议用户补充信息或咨询医生。\n");
        // 要求中文回答。
        builder.append("4. 请使用中文回答，语言要清楚、克制、面向普通用户。\n\n");
        // 添加检索上下文标题。
        builder.append("【RAG 检索上下文】\n");
        // 添加上下文文本。
        builder.append(context.getContextText()).append("\n\n");
        // 添加用户问题标题。
        builder.append("【用户问题】\n");
        // 添加用户问题。
        builder.append(context.getQuery()).append("\n\n");
        // 添加最终回答起始标记。
        builder.append("【请生成回答】");
        // 返回 prompt。
        return builder.toString();
    }
}
