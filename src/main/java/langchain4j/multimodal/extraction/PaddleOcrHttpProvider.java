package langchain4j.multimodal.extraction;

import langchain4j.multimodal.file.StoredMedicalFile;
import java.time.Duration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Calls the locally hosted PaddleOCR service without exposing file paths. */
public class PaddleOcrHttpProvider implements MedicalOcrProvider {

    private final RestClient restClient;

    public PaddleOcrHttpProvider(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank() || connectTimeout == null || readTimeout == null
                || connectTimeout.isNegative() || connectTimeout.isZero()
                || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException("OCR base URL and positive timeouts are required.");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.trim())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String providerName() {
        return "paddleocr-http";
    }

    @Override
    public boolean available() {
        try {
            OcrHealthResponse response = restClient.get().uri("/health").retrieve().body(OcrHealthResponse.class);
            return response != null && "UP".equalsIgnoreCase(response.status());
        } catch (RestClientException exception) {
            return false;
        }
    }

    @Override
    public String recognize(StoredMedicalFile file) {
        if (file == null || file.path() == null || file.metadata() == null) {
            throw new IllegalArgumentException("Stored medical image is required.");
        }
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file.path()));
        try {
            OcrResponse response = restClient.post()
                    .uri("/ocr")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(OcrResponse.class);
            if (response == null) {
                throw new MedicalOcrException("OCR 服务返回了空响应，请稍后重试。");
            }
            return response.text() == null ? "" : response.text();
        } catch (RestClientException exception) {
            throw new MedicalOcrException("调用本地 OCR 服务失败，请确认 PaddleOCR 服务正常运行。", exception);
        }
    }

    private record OcrHealthResponse(String status, String provider) {
    }

    private record OcrResponse(String provider, String text) {
    }
}
