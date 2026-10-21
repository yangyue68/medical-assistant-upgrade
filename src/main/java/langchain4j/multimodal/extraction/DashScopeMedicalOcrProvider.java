package langchain4j.multimodal.extraction;

import langchain4j.multimodal.file.StoredMedicalFile;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Extracts report text with the DashScope OpenAI-compatible multimodal API. */
public class DashScopeMedicalOcrProvider implements MedicalOcrProvider {

    private static final String OCR_PROMPT = "请准确提取这张医学报告中的全部可读文字。"
            + "每个化验指标单独一行，尽量整理为：项目名称 数值 单位 参考下限-参考上限。"
            + "保留报告名称和标本类型；不要解释、不要诊断、不要使用 Markdown；无法辨认的文字使用问号。";

    private final RestClient restClient;
    private final String modelName;

    public DashScopeMedicalOcrProvider(String endpoint, String apiKey, String modelName,
                                       Duration connectTimeout, Duration readTimeout) {
        if (isBlank(endpoint) || isBlank(apiKey) || isBlank(modelName)
                || connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()
                || readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("DashScope OCR endpoint, API key, model and positive timeouts are required.");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder()
                .baseUrl(endpoint.trim())
                .defaultHeader("Authorization", "Bearer " + apiKey.trim())
                .requestFactory(requestFactory)
                .build();
        this.modelName = modelName.trim();
    }

    @Override
    public String providerName() {
        return "dashscope-ocr";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String recognize(StoredMedicalFile file) {
        if (file == null || file.path() == null || file.metadata() == null) {
            throw new IllegalArgumentException("Stored medical image is required.");
        }
        try {
            String dataUrl = dataUrl(file);
            JsonNode response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(dataUrl))
                    .retrieve()
                    .body(JsonNode.class);
            String text = responseText(response);
            if (text.isBlank()) {
                throw new MedicalOcrException("OCR 服务未返回可识别文本，请上传更清晰的报告图片。");
            }
            return text;
        } catch (RestClientResponseException exception) {
            throw new MedicalOcrException("OCR 服务调用失败（HTTP " + exception.getStatusCode().value()
                    + "），请检查 OCR 模型、工作空间和 API Key 配置。", exception);
        } catch (RestClientException exception) {
            throw new MedicalOcrException("无法连接 OCR 服务，请检查网络和 OCR 接口地址。", exception);
        }
    }

    private Map<String, Object> requestBody(String dataUrl) {
        Map<String, Object> image = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUrl)
        );
        Map<String, Object> prompt = Map.of("type", "text", "text", OCR_PROMPT);
        Map<String, Object> message = Map.of("role", "user", "content", List.of(image, prompt));
        return Map.of(
                "model", modelName,
                "messages", List.of(message),
                "temperature", 0
        );
    }

    private String dataUrl(StoredMedicalFile file) {
        try {
            byte[] bytes = Files.readAllBytes(file.path());
            String contentType = file.metadata().getContentType();
            if (isBlank(contentType) || !contentType.startsWith("image/")) {
                contentType = ".png".equals(file.metadata().getExtension()) ? "image/png" : "image/jpeg";
            }
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException exception) {
            throw new MedicalOcrException("读取待识别的医学报告图片失败。", exception);
        }
    }

    private String responseText(JsonNode response) {
        if (response == null) {
            return "";
        }
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (content.isTextual()) {
            return content.asText().trim();
        }
        if (content.isArray()) {
            StringBuilder text = new StringBuilder();
            for (JsonNode item : content) {
                String value = item.path("text").asText("").trim();
                if (!value.isEmpty()) {
                    if (text.length() > 0) text.append('\n');
                    text.append(value);
                }
            }
            return text.toString();
        }
        return "";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
