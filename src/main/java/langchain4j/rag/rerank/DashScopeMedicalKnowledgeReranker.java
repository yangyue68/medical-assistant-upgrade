// 声明当前类所在的包，rerank 表示它属于检索结果重排层。
package langchain4j.rag.rerank;

// MedicalKnowledgeSearchHit 是医学知识检索命中对象。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// JsonNode 用于读取 DashScope rerank 接口返回的 JSON。
import com.fasterxml.jackson.databind.JsonNode;
// ArrayList 用来按 rerank 返回顺序组装结果。
import java.util.ArrayList;
// LinkedHashMap 保持请求 JSON 字段顺序稳定，便于排查问题。
import java.util.LinkedHashMap;
// List 表示候选命中集合。
import java.util.List;
// Map 表示提交给 DashScope 的请求体。
import java.util.Map;
// RestClient 是 Spring 的同步 HTTP 客户端。
import org.springframework.web.client.RestClient;
// RestClientException 表示调用 rerank 服务失败。
import org.springframework.web.client.RestClientException;

// DashScope 真实重排实现，用于把向量召回结果再交给 rerank 模型排序。
public class DashScopeMedicalKnowledgeReranker implements MedicalKnowledgeReranker {

    // restClient 负责调用 DashScope 兼容 rerank API。
    private final RestClient restClient;
    // modelName 保存本次使用的 rerank 模型名。
    private final String modelName;

    // 构造 DashScope rerank 客户端。
    public DashScopeMedicalKnowledgeReranker(String endpoint, String apiKey, String modelName) {
        // 端点、密钥、模型名任一为空都会导致外部调用失败。
        if (isBlank(endpoint) || isBlank(apiKey) || isBlank(modelName)) {
            // 抛出非法参数异常，提示配置不完整。
            throw new IllegalArgumentException("DashScope rerank endpoint, apiKey and modelName must not be blank.");
        }
        // 构建带 baseUrl 和 Bearer token 的 HTTP 客户端。
        this.restClient = RestClient.builder()
                // 设置 DashScope rerank 接口地址。
                .baseUrl(endpoint)
                // 设置授权头。
                .defaultHeader("Authorization", "Bearer " + apiKey)
                // 创建 RestClient 实例。
                .build();
        // 保存模型名，后续组装请求体时使用。
        this.modelName = modelName;
    }

    // DashScope 实现表示真实重排已启用。
    @Override
    public boolean isEnabled() {
        // 返回 true，让服务层扩大召回候选池后再重排。
        return true;
    }

    // 调用 DashScope rerank 服务对候选命中重新排序。
    @Override
    public List<MedicalKnowledgeSearchHit> rerank(String query, List<MedicalKnowledgeSearchHit> candidates, int topK) {
        // 查询为空或候选为空时，没有可重排内容。
        if (isBlank(query) || candidates == null || candidates.isEmpty()) {
            // 返回空列表。
            return List.of();
        }
        // 外部服务可能不可用，因此用 try/catch 做降级。
        try {
            // 发送 POST 请求并把响应解析为 JsonNode。
            JsonNode response = restClient.post()
                    // 指定请求体为 JSON。
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    // 构造 rerank 请求体。
                    .body(requestBody(query, candidates, topK))
                    // 执行请求。
                    .retrieve()
                    // 读取 JSON 响应。
                    .body(JsonNode.class);
            // 将 DashScope 响应转换回项目内部 hit。
            return rankedHits(response, candidates, topK);
        } catch (RestClientException exception) {
            // rerank 服务失败时不影响主流程，回退为原始召回顺序。
            return candidates.stream().limit(topK).toList();
        }
    }

    // 组装 DashScope rerank 请求体。
    private Map<String, Object> requestBody(String query, List<MedicalKnowledgeSearchHit> candidates, int topK) {
        // 把每条候选拼成“标题 + 摘要”的文档文本。
        List<String> documents = candidates.stream()
                // safe 避免 null 拼接成异常。
                .map(candidate -> (safe(candidate.getTitle()) + "\n" + safe(candidate.getSnippet())).trim())
                // 收集为列表。
                .toList();
        // 创建有序请求体。
        Map<String, Object> body = new LinkedHashMap<>();
        // 设置 rerank 模型名。
        body.put("model", modelName);
        // 设置用户问题。
        body.put("query", query.trim());
        // 设置候选文档。
        body.put("documents", documents);
        // 设置希望返回的最大条数。
        body.put("top_n", Math.min(topK, candidates.size()));
        // 给模型一个医疗检索场景指令，强调检索而不是诊断。
        body.put("instruct", "Given a medical information question, retrieve passages that directly answer it without making a diagnosis.");
        // 返回请求体。
        return body;
    }

    // 将 DashScope 响应中的索引和分数映射回原候选命中。
    private List<MedicalKnowledgeSearchHit> rankedHits(
            // DashScope 响应 JSON。
            JsonNode response,
            // 原始候选列表。
            List<MedicalKnowledgeSearchHit> candidates,
            // 最多返回条数。
            int topK
    ) {
        // 响应为空或没有 results 数组时，回退为原始召回顺序。
        if (response == null || !response.path("results").isArray()) {
            // 返回 topK 条原始候选。
            return candidates.stream().limit(topK).toList();
        }
        // 创建重排结果列表。
        List<MedicalKnowledgeSearchHit> ranked = new ArrayList<>();
        // 遍历 rerank 返回的每一项。
        for (JsonNode result : response.path("results")) {
            // 读取候选在原列表中的下标。
            int index = result.path("index").asInt(-1);
            // 下标越界时跳过，避免外部服务异常数据污染结果。
            if (index < 0 || index >= candidates.size()) {
                // 继续下一项。
                continue;
            }
            // 取出对应原始命中。
            MedicalKnowledgeSearchHit hit = candidates.get(index);
            // 读取 rerank 分数，缺失时沿用原召回分数。
            double relevanceScore = result.path("relevance_score").asDouble(hit.getScore());
            // 保存独立 rerank 分数字段。
            hit.setRerankScore(relevanceScore);
            // 用 rerank 分数覆盖最终展示分数。
            hit.setScore(relevanceScore);
            // 标记结果经历过 RERANK，便于调试来源。
            hit.setRetrievalSource(safe(hit.getRetrievalSource()) + "+RERANK");
            // 加入重排结果。
            ranked.add(hit);
            // 达到 topK 后提前停止。
            if (ranked.size() >= topK) {
                // 跳出循环。
                break;
            }
        }
        // 如果外部响应无法映射出任何结果，回退为原候选。
        return ranked.isEmpty() ? candidates.stream().limit(topK).toList() : ranked;
    }

    // 将 null 字符串安全转换为空字符串。
    private String safe(String value) {
        // null 返回空串，非 null 原样返回。
        return value == null ? "" : value;
    }

    // 判断字符串是否为空白。
    private boolean isBlank(String value) {
        // null、空串、纯空白都视为空。
        return value == null || value.trim().isEmpty();
    }
}
