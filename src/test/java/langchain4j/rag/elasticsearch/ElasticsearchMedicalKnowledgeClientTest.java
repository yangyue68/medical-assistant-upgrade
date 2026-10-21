package langchain4j.rag.elasticsearch;

import langchain4j.rag.MedicalKnowledgeChunk;
import langchain4j.rag.MedicalKnowledgeDocument;
import langchain4j.rag.MedicalKnowledgeDocumentType;
import langchain4j.rag.MedicalKnowledgeSearchRequest;
import langchain4j.rag.RetrievalMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchMedicalKnowledgeClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldSendBulkNdjsonAsUtf8Bytes() throws Exception {
        AtomicReference<byte[]> capturedBody = new AtomicReference<>();
        AtomicReference<String> capturedContentType = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/_bulk", exchange -> respondToBulk(exchange, capturedBody, capturedContentType));
        server.start();

        try {
            ElasticsearchMedicalKnowledgeClient client = new ElasticsearchMedicalKnowledgeClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "",
                    "",
                    "test-index",
                    "standard",
                    "standard",
                    1.0,
                    1.0
            );
            MedicalKnowledgeChunk chunk = MedicalKnowledgeChunk.from(
                    document(),
                    0,
                    "糖尿病患者需要复诊。Aborto quirúrgico también requiere consulta médica."
            );

            client.bulkIndex(List.of(chunk), List.of(new double[]{0.1, 0.2}));

            String requestBody = decodeStrictUtf8(capturedBody.get());
            assertThat(capturedContentType.get()).startsWith("application/x-ndjson");
            assertThat(requestBody).contains("糖尿病患者需要复诊");
            assertThat(requestBody).contains("quirúrgico");
            assertThat(requestBody).endsWith("\n");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldUseNativeKnnInsteadOfPainlessScripts() throws Exception {
        List<byte[]> capturedBodies = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/test-index/_search", exchange -> respondToSearch(exchange, capturedBodies));
        server.start();

        try {
            ElasticsearchMedicalKnowledgeClient client = new ElasticsearchMedicalKnowledgeClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "",
                    "",
                    "test-index",
                    "standard",
                    "standard",
                    1.5,
                    2.0
            );
            MedicalKnowledgeSearchRequest request = new MedicalKnowledgeSearchRequest();
            request.setQuery("糖尿病患者应该注意什么");
            request.setTopK(3);
            request.setLanguage("Chinese");
            double[] queryVector = new double[]{0.1, 0.2};

            client.search(request, RetrievalMode.VECTOR, queryVector);
            client.search(request, RetrievalMode.HYBRID, queryVector);

            JsonNode vectorBody = OBJECT_MAPPER.readTree(capturedBodies.get(0));
            JsonNode hybridBody = OBJECT_MAPPER.readTree(capturedBodies.get(1));
            assertThat(vectorBody.has("knn")).isTrue();
            assertThat(vectorBody.has("query")).isFalse();
            assertThat(vectorBody.at("/knn/k").asInt()).isEqualTo(3);
            assertThat(vectorBody.at("/knn/filter/term/language").asText()).isEqualTo("Chinese");
            assertThat(hybridBody.has("query")).isTrue();
            assertThat(hybridBody.has("knn")).isTrue();
            assertThat(hybridBody.at("/knn/boost").asDouble()).isEqualTo(2.0);
            assertThat(hybridBody.toString()).doesNotContain("script_score", "cosineSimilarity");
        } finally {
            server.stop(0);
        }
    }

    private static void respondToBulk(
            HttpExchange exchange,
            AtomicReference<byte[]> capturedBody,
            AtomicReference<String> capturedContentType
    ) throws IOException {
        capturedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
        capturedBody.set(exchange.getRequestBody().readAllBytes());
        byte[] response = "{\"errors\":false,\"items\":[]}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static void respondToSearch(HttpExchange exchange, List<byte[]> capturedBodies) throws IOException {
        capturedBodies.add(exchange.getRequestBody().readAllBytes());
        byte[] response = "{\"hits\":{\"hits\":[]}}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static String decodeStrictUtf8(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private static MedicalKnowledgeDocument document() {
        MedicalKnowledgeDocument document = new MedicalKnowledgeDocument();
        document.setDocumentId("test:utf8");
        document.setTitle("糖尿病与外科资料");
        document.setText("UTF-8 bulk request test");
        document.setLanguage("Chinese");
        document.setSourceName("测试来源");
        document.setSourceUrl("https://example.org/utf8");
        document.setDocumentType(MedicalKnowledgeDocumentType.HEALTH_TOPIC);
        document.setDiseaseOrTopic("糖尿病");
        document.setRetrievedAt(LocalDate.of(2026, 8, 2));
        return document;
    }
}
