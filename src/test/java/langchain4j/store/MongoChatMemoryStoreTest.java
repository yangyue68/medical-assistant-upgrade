// 声明测试类所在的包。
package langchain4j.store;

// ChatMessages 是 MongoDB 中保存聊天记忆的文档实体。
import langchain4j.bean.ChatMessages;
// ChatMessage 是 LangChain4j 对聊天消息的统一抽象。
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.UserMessage;
// List 用来创建测试消息列表。
import java.util.List;
// Document 是 MongoDB Java 驱动中的 BSON 文档对象。
import org.bson.Document;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;
// MongoTemplate 是被测存储类依赖的 MongoDB 操作对象。
import org.springframework.data.mongodb.core.MongoTemplate;
// Query 表示 MongoDB 查询对象。
import org.springframework.data.mongodb.core.query.Query;
// Update 表示 MongoDB 更新对象。
import org.springframework.data.mongodb.core.query.Update;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// Mockito 的 any 用来匹配任意参数。
import static org.mockito.ArgumentMatchers.any;
// Mockito 的 eq 用来匹配指定参数。
import static org.mockito.ArgumentMatchers.eq;
// Mockito 的 ArgumentCaptor 用来捕获 mock 方法收到的参数。
import static org.mockito.ArgumentCaptor.forClass;
// Mockito 的 mock 用来创建模拟对象。
import static org.mockito.Mockito.mock;
// Mockito 的 verify 用来验证方法调用。
import static org.mockito.Mockito.verify;
// Mockito 的 when 用来指定 mock 方法返回值。
import static org.mockito.Mockito.when;

// 定义 MongoChatMemoryStore 的单元测试。
class MongoChatMemoryStoreTest {

    // 测试 MongoDB 没有聊天记忆文档时返回空列表。
    @Test
    // 方法名描述测试意图：没有历史文档时应该返回空列表。
    void shouldReturnEmptyListWhenMemoryDocumentDoesNotExist() {
        // 创建 MongoTemplate mock，避免测试连接真实 MongoDB。
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        // 创建被测 Mongo 聊天记忆存储。
        MongoChatMemoryStore store = new MongoChatMemoryStore(mongoTemplate);
        // 指定 MongoDB 查询不到任何文档。
        when(mongoTemplate.findOne(any(Query.class), eq(ChatMessages.class))).thenReturn(null);

        // 调用读取记忆方法。
        List<ChatMessage> messages = store.getMessages(1001L);

        // 验证返回空列表而不是 null。
        assertThat(messages).isEmpty();
    }

    // 测试读取聊天记忆时按 memoryId 查询。
    @Test
    // 方法名描述测试意图：读取时应该使用 memoryId 作为查询条件。
    void shouldQueryByMemoryIdWhenLoadingMessages() {
        // 创建 MongoTemplate mock。
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        // 创建被测 Mongo 聊天记忆存储。
        MongoChatMemoryStore store = new MongoChatMemoryStore(mongoTemplate);
        // 指定 MongoDB 查询不到文档，让测试专注于查询条件本身。
        when(mongoTemplate.findOne(any(Query.class), eq(ChatMessages.class))).thenReturn(null);

        // 调用读取记忆方法。
        store.getMessages(1001L);

        // 创建 Query 参数捕获器。
        var queryCaptor = forClass(Query.class);
        // 捕获 findOne 方法收到的 Query。
        verify(mongoTemplate).findOne(queryCaptor.capture(), eq(ChatMessages.class));
        // 验证 Query 中的 memoryId 条件正确。
        assertThat(queryCaptor.getValue().getQueryObject().get("memoryId")).isEqualTo(1001L);
    }

    // 测试更新聊天记忆时会执行 upsert。
    @Test
    // 方法名描述测试意图：更新时应该按 memoryId upsert。
    void shouldUpsertMessagesByMemoryId() {
        // 创建 MongoTemplate mock。
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        // 创建被测 Mongo 聊天记忆存储。
        MongoChatMemoryStore store = new MongoChatMemoryStore(mongoTemplate);

        // 调用更新记忆方法，传入空消息列表即可覆盖 upsert 行为。
        store.updateMessages(1001L, List.of());

        // 创建 Query 参数捕获器。
        var queryCaptor = forClass(Query.class);
        // 创建 Update 参数捕获器。
        var updateCaptor = forClass(Update.class);
        // 捕获 upsert 方法收到的 Query 和 Update。
        verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(ChatMessages.class));
        // 验证 Query 中的 memoryId 条件正确。
        assertThat(queryCaptor.getValue().getQueryObject().get("memoryId")).isEqualTo(1001L);
        // 读取 Update 内部的 $set 文档。
        Document setDocument = updateCaptor.getValue().getUpdateObject().get("$set", Document.class);
        // 验证首次 upsert 新文档时也会写入 memoryId。
        assertThat(setDocument.get("memoryId")).isEqualTo(1001L);
        // 验证 content 字段已经被写入。
        assertThat(setDocument.getString("content")).isNotBlank();
    }

    @Test
    void shouldKeepWorkerMemorySeparateFromMainUserMessages() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        MongoChatMemoryStore store = new MongoChatMemoryStore(mongoTemplate);
        store.updateMessages("conversation:1001:worker:rag", List.of(
                AiMessage.from("第一次回答"),
                AiMessage.from("修复后的回答")
        ));

        var updateCaptor = forClass(Update.class);
        verify(mongoTemplate).upsert(any(Query.class), updateCaptor.capture(), eq(ChatMessages.class));
        Document setDocument = updateCaptor.getValue().getUpdateObject().get("$set", Document.class);
        List<ChatMessage> saved = ChatMessageDeserializer.messagesFromJson(setDocument.getString("content"));
        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(message -> message instanceof AiMessage);
    }

    // 测试删除聊天记忆时按 memoryId 删除。
    @Test
    // 方法名描述测试意图：删除时应该使用 memoryId 作为查询条件。
    void shouldDeleteMessagesByMemoryId() {
        // 创建 MongoTemplate mock。
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        // 创建被测 Mongo 聊天记忆存储。
        MongoChatMemoryStore store = new MongoChatMemoryStore(mongoTemplate);

        // 调用删除记忆方法。
        store.deleteMessages(1001L);

        // 创建 Query 参数捕获器。
        var queryCaptor = forClass(Query.class);
        // 捕获 remove 方法收到的 Query。
        verify(mongoTemplate).remove(queryCaptor.capture(), eq(ChatMessages.class));
        // 验证 Query 中的 memoryId 条件正确。
        assertThat(queryCaptor.getValue().getQueryObject().get("memoryId")).isEqualTo(1001L);
    }
}
