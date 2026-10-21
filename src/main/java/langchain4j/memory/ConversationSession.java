// 声明当前类所在的包，memory 表示它属于对话记忆和会话管理模块。
package langchain4j.memory;

// Instant 表示精确到时间线的创建和更新时间。
import java.time.Instant;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;
// ObjectId 是 MongoDB 默认文档 ID 类型。
import org.bson.types.ObjectId;
// Id 是 Spring Data 用来标记 MongoDB 文档主键的注解。
import org.springframework.data.annotation.Id;
// Document 用来声明当前类对应的 MongoDB 集合。
import org.springframework.data.mongodb.core.mapping.Document;

// 让 Lombok 自动生成常用样板代码。
@Data
// 声明当前实体映射到 MongoDB 的 conversation_sessions 集合。
@Document("conversation_sessions")
// 定义对话会话元数据。
public class ConversationSession {

    // MongoDB 文档 ID。
    @Id
    private ObjectId id;

    // 业务会话 ID，对应 LangChain4j ChatMemory 使用的 memoryId。
    private Long memoryId;
    private Long userId;

    // 会话标题，用于前端会话列表展示。
    private String title;

    // 会话创建时间。
    private Instant createdAt;

    // 会话最后更新时间。
    private Instant updatedAt;

    // 创建会话元数据对象。
    public static ConversationSession of(
            // 业务会话 ID。
            Long memoryId,
            // 会话标题。
            String title,
            // 会话创建时间。
            Instant createdAt,
            // 会话更新时间。
            Instant updatedAt
    ) {
        // 创建会话对象。
        ConversationSession session = new ConversationSession();
        // 设置业务会话 ID。
        session.setMemoryId(memoryId);
        // 设置会话标题。
        session.setTitle(title);
        // 设置创建时间。
        session.setCreatedAt(createdAt);
        // 设置更新时间。
        session.setUpdatedAt(updatedAt);
        // 返回会话对象。
        return session;
    }

    public static ConversationSession of(Long userId, Long memoryId, String title,
                                         Instant createdAt, Instant updatedAt) {
        ConversationSession session = of(memoryId, title, createdAt, updatedAt);
        session.setUserId(userId);
        return session;
    }
}
