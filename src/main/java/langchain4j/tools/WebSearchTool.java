package langchain4j.tools;

import langchain4j.util.SensitiveDataMasker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
public class WebSearchTool {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);

    @Value("${tavily.api-key:}")
    private String tavilyApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebSearchTool() {
        this(15_000, 45_000);
    }

    @Autowired
    public WebSearchTool(
            @Value("${tavily.connect-timeout-ms:15000}") int connectTimeoutMs,
            @Value("${tavily.read-timeout-ms:45000}") int readTimeoutMs) {
        // Keep an unavailable search provider from holding the chat request forever.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(1000, connectTimeoutMs));
        factory.setReadTimeout(Math.max(1000, readTimeoutMs));
        this.restTemplate = new RestTemplate(factory);
    }

    @Tool(name = "webSearch", value = "搜索互联网获取实时信息，如新闻、天气、最新医疗资讯等。当用户问及实时信息或知识库中没有的内容时，使用此工具。")
    public String search(@P("搜索关键词") String query) {
        log.info("Web search requested. query={}", SensitiveDataMasker.mask(query));

        if (tavilyApiKey == null || tavilyApiKey.isEmpty()) {
            return "网络搜索功能未配置，请检查 API Key";
        }

        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("api_key", tavilyApiKey);
            requestBody.put("query", query);
            requestBody.put("max_results", 5);

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.tavily.com/search",
                    request,
                    String.class
            );

            // 解析结果
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode results = root.get("results");

            if (results != null && results.isArray()) {
                StringBuilder sb = new StringBuilder();
                sb.append("搜索完成，找到以下结果：\n\n");
                int count = 0;
                for (JsonNode result : results) {
                    if (count++ >= 5) break;
                    String title = result.path("title").asText("");
                    String url = result.path("url").asText("");
                    String content = result.path("content").asText("");
                    // Keep tool output bounded so route/news searches cannot overflow
                    // the worker model context window.
                    if (content.length() > 1200) content = content.substring(0, 1200) + "…";
                    sb.append("• ").append(title).append("\n");
                    sb.append("  ").append(url).append("\n");
                    sb.append("  ").append(content).append("\n\n");
                }
                return sb.toString();
            }

            return "未找到相关结果";

        } catch (Exception e) {
            log.warn("Web search failed. query={}", SensitiveDataMasker.mask(query), e);
            return "搜索服务暂时不可用，请稍后重试。";
        }
    }
}
