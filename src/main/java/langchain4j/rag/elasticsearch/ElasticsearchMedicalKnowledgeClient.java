// 声明当前类所在的包，elasticsearch 表示它负责对接 Elasticsearch。
package langchain4j.rag.elasticsearch;

// MedicalKnowledgeChunk 是写入 Elasticsearch 的医学知识片段模型。
import langchain4j.rag.MedicalKnowledgeChunk;
// MedicalKnowledgeDocumentType 是文档类型枚举，写入和读取时都要转换。
import langchain4j.rag.MedicalKnowledgeDocumentType;
// MedicalKnowledgeSearchHit 是 Elasticsearch 检索后返回给业务层的统一命中对象。
import langchain4j.rag.MedicalKnowledgeSearchHit;
// MedicalKnowledgeSearchRequest 是检索请求，包含 query、topK、语言、科室和文档类型过滤。
import langchain4j.rag.MedicalKnowledgeSearchRequest;
// RetrievalMode 表示关键词、向量或混合检索模式。
import langchain4j.rag.RetrievalMode;
// JsonProcessingException 表示 Jackson 序列化或反序列化失败。
import com.fasterxml.jackson.core.JsonProcessingException;
// ObjectMapper 负责把 Java Map 和 Elasticsearch JSON 互相转换。
import com.fasterxml.jackson.databind.ObjectMapper;
// IOException 表示 Elasticsearch 响应 JSON 解析失败。
import java.io.IOException;
// StandardCharsets 提供 UTF-8 编码常量。
import java.nio.charset.StandardCharsets;
// LocalDate 用于还原 chunk 的 retrievedAt 日期。
import java.time.LocalDate;
// ArrayList 用于构建可变过滤条件和命中列表。
import java.util.ArrayList;
// Base64 用于生成 HTTP Basic Auth 请求头。
import java.util.Base64;
// LinkedHashMap 用于构造字段顺序稳定的 JSON Map。
import java.util.LinkedHashMap;
// List 表示一组过滤条件、文档类型、向量值或命中结果。
import java.util.List;
// Map 表示 Elasticsearch 请求体和响应体中的 JSON 对象。
import java.util.Map;
// Logger 是 SLF4J 日志接口。
import org.slf4j.Logger;
// LoggerFactory 用于创建当前类的日志对象。
import org.slf4j.LoggerFactory;
// HttpHeaders 提供标准 HTTP 头名称。
import org.springframework.http.HttpHeaders;
// MediaType 表示 Elasticsearch JSON 和 NDJSON 请求体类型。
import org.springframework.http.MediaType;
// HttpClientErrorException 用于识别 Elasticsearch 返回的 404 等客户端错误。
import org.springframework.web.client.HttpClientErrorException;
// RestClient 是 Spring 6 自带的同步 HTTP 客户端。
import org.springframework.web.client.RestClient;
// StringUtils 提供字符串判空工具。
import org.springframework.util.StringUtils;

// 定义 Elasticsearch 医学知识客户端，封装索引创建、批量写入和检索请求。
public class ElasticsearchMedicalKnowledgeClient {

    // 为当前类创建日志对象。
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchMedicalKnowledgeClient.class);

    // 定义 Elasticsearch bulk API 需要的 NDJSON 媒体类型。
    private static final MediaType NDJSON = MediaType.parseMediaType("application/x-ndjson");

    // 保存 Elasticsearch HTTP 客户端。
    private final RestClient restClient;

    // 保存 Jackson JSON 工具。
    private final ObjectMapper objectMapper;

    // 保存 Elasticsearch 索引名。
    private final String indexName;

    // 保存正文和标题字段使用的索引分词器。
    private final String textAnalyzer;

    // 保存正文和标题字段使用的查询分词器。
    private final String searchAnalyzer;

    // 保存混合检索中关键词分数权重。
    private final double keywordWeight;

    // 保存混合检索中向量相似度权重。
    private final double vectorWeight;

    // 创建 Elasticsearch 医学知识客户端。
    public ElasticsearchMedicalKnowledgeClient(
            // Elasticsearch 地址，例如 https://xxx.public.elasticsearch.aliyuncs.com:9200。
            String endpoint,
            // Elasticsearch 用户名。
            String username,
            // Elasticsearch 密码。
            String password,
            // Elasticsearch 索引名。
            String indexName,
            // 索引分词器。
            String textAnalyzer,
            // 查询分词器。
            String searchAnalyzer,
            // 混合检索关键词权重。
            double keywordWeight,
            // 混合检索向量权重。
            double vectorWeight
    ) {
        // Elasticsearch 地址不能为空。
        if (!StringUtils.hasText(endpoint)) {
            // 抛出配置错误。
            throw new IllegalArgumentException("Elasticsearch endpoint must not be blank.");
        }
        // Elasticsearch 索引名不能为空。
        if (!StringUtils.hasText(indexName)) {
            // 抛出配置错误。
            throw new IllegalArgumentException("Elasticsearch index name must not be blank.");
        }
        // 创建 RestClient 构建器。
        RestClient.Builder builder = RestClient.builder().baseUrl(endpoint.trim());
        // 如果配置了用户名或密码，就添加 Basic Auth。
        if (StringUtils.hasText(username) || StringUtils.hasText(password)) {
            // 组装用户名和密码明文片段。
            String credential = (username == null ? "" : username.trim()) + ":" + (password == null ? "" : password);
            // 使用 UTF-8 编码后做 Base64。
            String encodedCredential = Base64.getEncoder().encodeToString(credential.getBytes(StandardCharsets.UTF_8));
            // 写入 Authorization 请求头。
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredential);
        }
        // 构建 RestClient。
        this.restClient = builder.build();
        // 创建 ObjectMapper。
        this.objectMapper = new ObjectMapper();
        // 保存索引名。
        this.indexName = indexName.trim();
        // 保存索引分词器；空值退回 standard，避免映射创建失败。
        this.textAnalyzer = StringUtils.hasText(textAnalyzer) ? textAnalyzer.trim() : "standard";
        // 保存查询分词器；空值退回索引分词器。
        this.searchAnalyzer = StringUtils.hasText(searchAnalyzer) ? searchAnalyzer.trim() : this.textAnalyzer;
        // 保存关键词权重。
        this.keywordWeight = keywordWeight;
        // 保存向量权重。
        this.vectorWeight = vectorWeight;
    }

    // 判断索引是否存在。
    public boolean indexExists() {
        // try 块中执行 HEAD 请求。
        try {
            // 调用 Elasticsearch HEAD /{index}。
            restClient.head().uri("/{index}", indexName).retrieve().toBodilessEntity();
            // 没有异常说明索引存在。
            return true;
        } catch (HttpClientErrorException.NotFound exception) {
            // 404 说明索引不存在。
            return false;
        }
    }

    // 删除索引；显式重建时使用。
    public void deleteIndexIfExists() {
        // 如果索引不存在，直接返回。
        if (!indexExists()) {
            // 结束删除流程。
            return;
        }
        // 调用 Elasticsearch DELETE /{index}。
        restClient.delete().uri("/{index}", indexName).retrieve().toBodilessEntity();
    }

    // 确保索引存在；不存在时创建带 text 和 dense_vector 映射的索引。
    public void ensureIndex(int embeddingDimension) {
        // 如果索引已经存在，直接返回。
        if (indexExists()) {
            // 结束创建流程。
            return;
        }
        // 创建索引请求体。
        Map<String, Object> body = indexCreationBody(embeddingDimension);
        // 调用 Elasticsearch PUT /{index} 创建索引。
        restClient.put()
                // 指定索引路径。
                .uri("/{index}", indexName)
                // 指定 JSON 请求体。
                .contentType(MediaType.APPLICATION_JSON)
                // 写入创建索引请求体。
                .body(body)
                // 发送请求并要求无响应体。
                .retrieve()
                // 转成空响应实体。
                .toBodilessEntity();
    }

    // 批量写入 chunk 和 embedding。
    public void bulkIndex(List<MedicalKnowledgeChunk> chunks, List<double[]> embeddings) {
        // chunk 列表和向量列表必须一一对应。
        if (chunks == null || embeddings == null || chunks.size() != embeddings.size()) {
            // 抛出调用错误。
            throw new IllegalArgumentException("chunks and embeddings must have the same size.");
        }
        // 如果没有数据，就不用调用 bulk API。
        if (chunks.isEmpty()) {
            // 结束写入。
            return;
        }
        // 创建 NDJSON 构建器。
        StringBuilder ndjson = new StringBuilder();
        // 遍历所有 chunk。
        for (int i = 0; i < chunks.size(); i++) {
            // 读取当前 chunk。
            MedicalKnowledgeChunk chunk = chunks.get(i);
            // 读取当前 embedding。
            double[] embedding = embeddings.get(i);
            // 空 chunk 或空正文不写入索引。
            if (chunk == null || !StringUtils.hasText(chunk.getText())) {
                // 继续下一个 chunk。
                continue;
            }
            // 写入 bulk index action 行。
            appendJsonLine(ndjson, Map.of("index", Map.of("_index", indexName, "_id", chunk.getChunkId())));
            // 写入 bulk document 行。
            appendJsonLine(ndjson, documentBody(chunk, embedding));
        }
        // 如果全部数据都被跳过，就不用调用 bulk API。
        if (ndjson.isEmpty()) {
            // 结束写入。
            return;
        }
        // 显式转换为 UTF-8 字节，避免 StringHttpMessageConverter 按 ISO-8859-1
        // 编码 NDJSON，导致中文变成问号、重音字符变成非法 UTF-8 字节。
        byte[] requestBody = ndjson.toString().getBytes(StandardCharsets.UTF_8);
        // 调用 Elasticsearch POST /_bulk。
        String response = restClient.post()
                // bulk API 路径。
                .uri("/_bulk")
                // bulk API 要求 NDJSON。
                .contentType(NDJSON)
                // 直接写入已经按 UTF-8 编码的 NDJSON 字节。
                .body(requestBody)
                // 发送请求。
                .retrieve()
                // 读取响应字符串。
                .body(String.class);
        // 如果 bulk 响应包含错误，就抛出异常。
        failIfBulkHasErrors(response);
        // 记录写入完成日志。
        log.info("Indexed {} medical knowledge chunks into Elasticsearch index {}.", chunks.size(), indexName);
    }

    // 执行关键词、向量或混合检索。
    public List<MedicalKnowledgeSearchHit> search(
            // 检索请求。
            MedicalKnowledgeSearchRequest request,
            // 实际检索模式。
            RetrievalMode retrievalMode,
            // 查询向量；KEYWORD 模式下可以为 null。
            double[] queryVector
    ) {
        // 创建搜索请求体。
        Map<String, Object> body = searchBody(request, retrievalMode, queryVector);
        // 调用 Elasticsearch POST /{index}/_search。
        // 捕获索引尚未创建的短暂窗口，避免 RAG 不可用拖垮整个聊天接口。
        try {
            // 发送 Elasticsearch 搜索请求。
            String response = restClient.post()
                    // 指定搜索路径。
                    .uri("/{index}/_search", indexName)
                    // 指定 JSON 请求体。
                    .contentType(MediaType.APPLICATION_JSON)
                    // 写入搜索请求体。
                    .body(body)
                    // 发送请求。
                    .retrieve()
                    // 读取响应字符串。
                    .body(String.class);
            // 把 Elasticsearch 响应转换为统一命中列表。
            return toHits(response, retrievalMode);
        } catch (HttpClientErrorException.NotFound exception) {
            // 后台首次导入尚未创建索引时，返回空知识结果，让聊天模型继续回答。
            log.warn("Elasticsearch medical knowledge index is not ready yet: {}", indexName);
            // 返回空命中列表。
            return List.of();
        }
    }

    // 创建索引请求体。
    private Map<String, Object> indexCreationBody(int embeddingDimension) {
        // 创建根对象。
        Map<String, Object> root = new LinkedHashMap<>();
        // 创建 mappings 对象。
        Map<String, Object> mappings = new LinkedHashMap<>();
        // 创建 properties 对象。
        Map<String, Object> properties = new LinkedHashMap<>();
        // 添加 chunkId keyword 字段。
        properties.put("chunkId", keywordField());
        // 添加 documentId keyword 字段。
        properties.put("documentId", keywordField());
        // 添加 chunkIndex integer 字段。
        properties.put("chunkIndex", Map.of("type", "integer"));
        // 添加 title text 字段。
        properties.put("title", textField());
        // 添加 text text 字段。
        properties.put("text", textField());
        // 添加 language keyword 字段。
        properties.put("language", keywordField());
        // 添加 sourceName keyword 字段。
        properties.put("sourceName", keywordField());
        // 添加 sourceUrl keyword 字段。
        properties.put("sourceUrl", keywordField());
        // 添加 documentType keyword 字段。
        properties.put("documentType", keywordField());
        // 添加 department keyword 字段。
        properties.put("department", keywordField());
        // 添加 diseaseOrTopic text 字段。
        properties.put("diseaseOrTopic", textField());
        // 添加 sourceCreatedAt keyword 字段。
        properties.put("sourceCreatedAt", keywordField());
        // 添加 sourceGeneratedAt keyword 字段。
        properties.put("sourceGeneratedAt", keywordField());
        // 添加 retrievedAt keyword 字段。
        properties.put("retrievedAt", keywordField());
        // 添加 embedding dense_vector 字段。
        properties.put("embedding", Map.of("type", "dense_vector", "dims", embeddingDimension));
        // 把 properties 放入 mappings。
        mappings.put("properties", properties);
        // 把 mappings 放入根对象。
        root.put("mappings", mappings);
        // 返回创建索引请求体。
        return root;
    }

    // 创建 keyword 字段映射。
    private Map<String, Object> keywordField() {
        // 返回 keyword 字段定义。
        return Map.of("type", "keyword");
    }

    // 创建 text 字段映射。
    private Map<String, Object> textField() {
        // 创建字段映射。
        Map<String, Object> field = new LinkedHashMap<>();
        // 设置字段类型为 text。
        field.put("type", "text");
        // 设置索引分词器。
        field.put("analyzer", textAnalyzer);
        // 设置查询分词器。
        field.put("search_analyzer", searchAnalyzer);
        // 返回字段映射。
        return field;
    }

    // 创建单个文档请求体。
    private Map<String, Object> documentBody(MedicalKnowledgeChunk chunk, double[] embedding) {
        // 创建文档对象。
        Map<String, Object> document = new LinkedHashMap<>();
        // 写入 chunkId。
        document.put("chunkId", chunk.getChunkId());
        // 写入 documentId。
        document.put("documentId", chunk.getDocumentId());
        // 写入 chunkIndex。
        document.put("chunkIndex", chunk.getChunkIndex());
        // 写入 title。
        document.put("title", chunk.getTitle());
        // 写入正文 text。
        document.put("text", chunk.getText());
        // 写入 language。
        document.put("language", chunk.getLanguage());
        // 写入 sourceName。
        document.put("sourceName", chunk.getSourceName());
        // 写入 sourceUrl。
        document.put("sourceUrl", chunk.getSourceUrl());
        // 写入 documentType。
        document.put("documentType", chunk.getDocumentType() == null ? null : chunk.getDocumentType().name());
        // 写入 department。
        document.put("department", chunk.getDepartment());
        // 写入 diseaseOrTopic。
        document.put("diseaseOrTopic", chunk.getDiseaseOrTopic());
        // 写入 sourceCreatedAt。
        document.put("sourceCreatedAt", chunk.getSourceCreatedAt());
        // 写入 sourceGeneratedAt。
        document.put("sourceGeneratedAt", chunk.getSourceGeneratedAt());
        // 写入 retrievedAt。
        document.put("retrievedAt", chunk.getRetrievedAt() == null ? null : chunk.getRetrievedAt().toString());
        // 写入 embedding。
        document.put("embedding", toList(embedding));
        // 返回文档对象。
        return document;
    }

    // 创建搜索请求体。
    private Map<String, Object> searchBody(MedicalKnowledgeSearchRequest request, RetrievalMode retrievalMode, double[] queryVector) {
        // 创建搜索请求体。
        Map<String, Object> body = new LinkedHashMap<>();
        // 设置返回数量。
        body.put("size", request.normalizedTopK());
        // 排除 embedding，避免响应体巨大。
        body.put("_source", Map.of("excludes", List.of("embedding")));
        // KEYWORD 模式使用普通 BM25 查询。
        if (retrievalMode == RetrievalMode.KEYWORD) {
            // 写入关键词查询。
            body.put("query", keywordQuery(request));
            // 返回请求体。
            return body;
        }
        // VECTOR 和 HYBRID 模式都需要查询向量。
        if (queryVector == null) {
            // 抛出调用错误。
            throw new IllegalArgumentException("queryVector must not be null for vector or hybrid search.");
        }
        // VECTOR 模式使用 Elasticsearch 原生 HNSW knn 查询。
        if (retrievalMode == RetrievalMode.VECTOR) {
            // 原生 knn 不依赖 Painless，兼容阿里云 Elasticsearch Serverless。
            body.put("knn", knnQuery(request, queryVector, 1.0));
            // 返回请求体。
            return body;
        }
        // HYBRID 使用顶层 BM25 query 与 knn 并列，Elasticsearch 会融合两路分数。
        body.put("query", keywordQuery(request));
        // 通过 boost 保留配置中的向量权重。
        body.put("knn", knnQuery(request, queryVector, vectorWeight));
        // 返回请求体。
        return body;
    }

    // 创建关键词查询。
    private Map<String, Object> keywordQuery(MedicalKnowledgeSearchRequest request) {
        // 创建 bool 查询。
        Map<String, Object> bool = new LinkedHashMap<>();
        // must 使用 multi_match 召回标题、正文、科室和主题。
        bool.put("must", List.of(multiMatch(request.getQuery())));
        // 添加过滤条件。
        addFilters(bool, request, false);
        // 返回 bool 查询对象。
        return Map.of("bool", bool);
    }

    // 创建 Elasticsearch 原生 knn 查询。
    private Map<String, Object> knnQuery(
            MedicalKnowledgeSearchRequest request,
            double[] queryVector,
            double boost
    ) {
        // 创建 knn 参数对象。
        Map<String, Object> knn = new LinkedHashMap<>();
        // 指定 dense_vector 字段。
        knn.put("field", "embedding");
        // 写入在线 embedding 模型生成的查询向量。
        knn.put("query_vector", toList(queryVector));
        // k 与最终返回数量一致。
        int topK = request.normalizedTopK();
        knn.put("k", topK);
        // 扩大候选集以提高 HNSW 近似检索召回率，同时控制请求成本。
        knn.put("num_candidates", Math.max(10, Math.min(1000, topK * 10)));
        // 混合检索通过 boost 调整向量分数权重。
        knn.put("boost", boost);
        // 原生 knn 的 filter 会在向量检索前执行语言、科室和文档类型过滤。
        Map<String, Object> filter = knnFilter(request);
        if (filter != null) {
            knn.put("filter", filter);
        }
        // 返回原生 knn 参数。
        return knn;
    }

    // 把业务过滤条件转换成 knn.filter 接受的查询对象。
    private Map<String, Object> knnFilter(MedicalKnowledgeSearchRequest request) {
        // 复用关键词检索的过滤条件构造逻辑。
        List<Map<String, Object>> filters = filters(request);
        // 没有过滤条件时省略 filter。
        if (filters.isEmpty()) {
            return null;
        }
        // 单个过滤条件可以直接传给 knn。
        if (filters.size() == 1) {
            return filters.get(0);
        }
        // 多个条件使用 bool.filter 表示全部满足。
        return Map.of("bool", Map.of("filter", filters));
    }

    // 创建多字段关键词查询。
    private Map<String, Object> multiMatch(String query) {
        // 创建 multi_match 参数。
        Map<String, Object> multiMatch = new LinkedHashMap<>();
        // 设置查询文本。
        multiMatch.put("query", query);
        // 设置参与关键词检索的字段和权重。
        multiMatch.put("fields", List.of("title^3", "text", "department^2", "diseaseOrTopic^2"));
        // 保留可配置的关键词分数权重。
        multiMatch.put("boost", keywordWeight);
        // 返回 multi_match 查询。
        return Map.of("multi_match", multiMatch);
    }

    // 添加过滤条件。
    private void addFilters(Map<String, Object> bool, MedicalKnowledgeSearchRequest request, boolean addMatchAllWhenEmpty) {
        // 创建过滤条件列表。
        List<Map<String, Object>> filters = filters(request);
        // 如果没有过滤条件且要求补 match_all，就添加 match_all 过滤。
        if (filters.isEmpty() && addMatchAllWhenEmpty) {
            // 添加 match_all。
            filters.add(Map.of("match_all", Map.of()));
        }
        // 如果过滤条件非空，就写入 bool.filter。
        if (!filters.isEmpty()) {
            // 写入过滤条件。
            bool.put("filter", filters);
        }
    }

    // 构造过滤条件列表。
    private List<Map<String, Object>> filters(MedicalKnowledgeSearchRequest request) {
        // 创建过滤条件列表。
        List<Map<String, Object>> filters = new ArrayList<>();
        // 如果配置了语言过滤，就添加 term。
        if (StringUtils.hasText(request.getLanguage())) {
            // 添加 language term 过滤。
            filters.add(Map.of("term", Map.of("language", request.getLanguage().trim())));
        }
        // 如果配置了科室过滤，就添加 term。
        if (StringUtils.hasText(request.getDepartment())) {
            // 添加 department term 过滤。
            filters.add(Map.of("term", Map.of("department", request.getDepartment().trim())));
        }
        // 如果配置了文档类型过滤，就添加 terms。
        if (request.getDocumentTypes() != null && !request.getDocumentTypes().isEmpty()) {
            // 转换枚举为字符串。
            List<String> documentTypes = request.getDocumentTypes().stream().map(Enum::name).toList();
            // 添加 documentType terms 过滤。
            filters.add(Map.of("terms", Map.of("documentType", documentTypes)));
        }
        // 返回过滤条件列表。
        return filters;
    }

    // 把 double[] 转成 List<Double>，便于 Jackson 输出 JSON 数组。
    private List<Double> toList(double[] vector) {
        // 创建列表。
        List<Double> values = new ArrayList<>();
        // 空向量返回空列表。
        if (vector == null) {
            // 返回空列表。
            return values;
        }
        // 遍历向量。
        for (double value : vector) {
            // 添加单个维度值。
            values.add(value);
        }
        // 返回列表。
        return values;
    }

    // 追加一行 JSON 到 NDJSON。
    private void appendJsonLine(StringBuilder ndjson, Map<String, Object> value) {
        // try 块中执行 JSON 序列化。
        try {
            // 写入 JSON 字符串。
            ndjson.append(objectMapper.writeValueAsString(value));
            // bulk API 要求每个 JSON 对象独占一行。
            ndjson.append('\n');
        } catch (JsonProcessingException exception) {
            // 包装序列化异常。
            throw new IllegalStateException("Failed to serialize Elasticsearch bulk request.", exception);
        }
    }

    // 检查 bulk 响应是否包含错误。
    @SuppressWarnings("unchecked")
    private void failIfBulkHasErrors(String response) {
        // try 块中解析响应。
        try {
            // 解析响应 JSON。
            Map<String, Object> body = objectMapper.readValue(response, Map.class);
            // 读取 errors 字段。
            Object errors = body.get("errors");
            // 如果 errors=true，说明至少一条写入失败。
            if (Boolean.TRUE.equals(errors)) {
                // 抛出异常并保留原始响应，便于定位失败条目。
                throw new IllegalStateException("Elasticsearch bulk index failed: " + response);
            }
        } catch (IOException exception) {
            // 包装解析异常。
            throw new IllegalStateException("Failed to parse Elasticsearch bulk response.", exception);
        }
    }

    // 把搜索响应转换成统一命中对象。
    @SuppressWarnings("unchecked")
    private List<MedicalKnowledgeSearchHit> toHits(String response, RetrievalMode retrievalMode) {
        // try 块中解析响应。
        try {
            // 解析根对象。
            Map<String, Object> root = objectMapper.readValue(response, Map.class);
            // 读取 hits 对象。
            Map<String, Object> hitsRoot = (Map<String, Object>) root.get("hits");
            // 读取 hits 数组。
            List<Map<String, Object>> rawHits = hitsRoot == null ? List.of() : (List<Map<String, Object>>) hitsRoot.get("hits");
            // 创建统一命中列表。
            List<MedicalKnowledgeSearchHit> hits = new ArrayList<>();
            // 遍历 Elasticsearch 命中。
            for (Map<String, Object> rawHit : rawHits) {
                // 读取 _source。
                Map<String, Object> source = (Map<String, Object>) rawHit.get("_source");
                // source 为空时跳过。
                if (source == null) {
                    // 继续下一个命中。
                    continue;
                }
                // 转换并添加统一命中。
                hits.add(toHit(source, number(rawHit.get("_score")), retrievalMode));
            }
            // 返回统一命中列表。
            return hits;
        } catch (IOException exception) {
            // 包装解析异常。
            throw new IllegalStateException("Failed to parse Elasticsearch search response.", exception);
        }
    }

    // 把单个 _source 转换成统一命中对象。
    private MedicalKnowledgeSearchHit toHit(Map<String, Object> source, double score, RetrievalMode retrievalMode) {
        // 创建命中对象。
        MedicalKnowledgeSearchHit hit = new MedicalKnowledgeSearchHit();
        // 设置 chunkId。
        hit.setChunkId(string(source.get("chunkId")));
        // 设置 documentId。
        hit.setDocumentId(string(source.get("documentId")));
        // 设置 chunkIndex。
        hit.setChunkIndex(integer(source.get("chunkIndex")));
        // 设置标题。
        hit.setTitle(string(source.get("title")));
        // 设置片段正文。
        hit.setSnippet(string(source.get("text")));
        // 设置来源名称。
        hit.setSourceName(string(source.get("sourceName")));
        // 设置来源 URL。
        hit.setSourceUrl(string(source.get("sourceUrl")));
        // 设置文档类型。
        hit.setDocumentType(documentType(string(source.get("documentType"))));
        // 设置总分。
        hit.setScore(score);
        // 如果是关键词模式，记录关键词分数。
        if (retrievalMode == RetrievalMode.KEYWORD) {
            // 设置关键词分数。
            hit.setKeywordScore(score);
        }
        // 如果是向量模式，记录向量分数。
        if (retrievalMode == RetrievalMode.VECTOR) {
            // 设置向量分数。
            hit.setVectorScore(score);
        }
        // 设置检索来源。
        hit.setRetrievalSource(retrievalMode == RetrievalMode.HYBRID ? "ELASTICSEARCH_HYBRID" : "ELASTICSEARCH_" + retrievalMode.name());
        // 返回命中对象。
        return hit;
    }

    // 把对象转成字符串。
    private String string(Object value) {
        // null 返回 null。
        if (value == null) {
            // 返回 null。
            return null;
        }
        // 返回字符串形式。
        return String.valueOf(value);
    }

    // 把对象转成 Integer。
    private Integer integer(Object value) {
        // null 返回 null。
        if (value == null) {
            // 返回 null。
            return null;
        }
        // Number 直接取 int。
        if (value instanceof Number number) {
            // 返回 int 值。
            return number.intValue();
        }
        // 字符串解析 int。
        return Integer.parseInt(String.valueOf(value));
    }

    // 把对象转成 double。
    private double number(Object value) {
        // null 分数按 0 处理。
        if (value == null) {
            // 返回 0。
            return 0.0;
        }
        // Number 直接取 double。
        if (value instanceof Number number) {
            // 返回 double 值。
            return number.doubleValue();
        }
        // 字符串解析 double。
        return Double.parseDouble(String.valueOf(value));
    }

    // 把字符串还原成文档类型枚举。
    private MedicalKnowledgeDocumentType documentType(String value) {
        // 空值返回 null。
        if (!StringUtils.hasText(value)) {
            // 返回 null。
            return null;
        }
        // 返回枚举值。
        return MedicalKnowledgeDocumentType.valueOf(value);
    }

    // 把 _source 还原成 chunk，当前主要预留给后续管理接口使用。
    public MedicalKnowledgeChunk toChunk(Map<String, Object> source) {
        // 创建 chunk 对象。
        MedicalKnowledgeChunk chunk = new MedicalKnowledgeChunk();
        // 设置 chunkId。
        chunk.setChunkId(string(source.get("chunkId")));
        // 设置 documentId。
        chunk.setDocumentId(string(source.get("documentId")));
        // 设置 chunkIndex。
        chunk.setChunkIndex(integer(source.get("chunkIndex")) == null ? 0 : integer(source.get("chunkIndex")));
        // 设置标题。
        chunk.setTitle(string(source.get("title")));
        // 设置正文。
        chunk.setText(string(source.get("text")));
        // 设置语言。
        chunk.setLanguage(string(source.get("language")));
        // 设置来源名称。
        chunk.setSourceName(string(source.get("sourceName")));
        // 设置来源 URL。
        chunk.setSourceUrl(string(source.get("sourceUrl")));
        // 设置文档类型。
        chunk.setDocumentType(documentType(string(source.get("documentType"))));
        // 设置科室。
        chunk.setDepartment(string(source.get("department")));
        // 设置疾病或主题。
        chunk.setDiseaseOrTopic(string(source.get("diseaseOrTopic")));
        // 设置来源创建日期。
        chunk.setSourceCreatedAt(string(source.get("sourceCreatedAt")));
        // 设置来源生成日期。
        chunk.setSourceGeneratedAt(string(source.get("sourceGeneratedAt")));
        // 设置获取日期。
        chunk.setRetrievedAt(StringUtils.hasText(string(source.get("retrievedAt"))) ? LocalDate.parse(string(source.get("retrievedAt"))) : null);
        // 返回 chunk。
        return chunk;
    }
}
