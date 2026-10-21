// 声明当前类所在的包，bean 表示它是项目中的数据对象。
package langchain4j.bean;

// AllArgsConstructor 会让 Lombok 生成全参构造方法。
import lombok.AllArgsConstructor;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;
// NoArgsConstructor 会让 Lombok 生成无参构造方法。
import lombok.NoArgsConstructor;
// ObjectId 是 MongoDB 默认文档 ID 类型。
import org.bson.types.ObjectId;
// Id 是 Spring Data 用来标记 MongoDB 文档主键的注解。
import org.springframework.data.annotation.Id;
// Document 用来声明当前类对应的 MongoDB 集合。
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yangyue
 * @date 2026/4/8 23:05
 * @desc 这个实体类是用来和MongoDB打交道的
 */

// 让 Lombok 自动生成常用样板代码。
@Data
// 让 Lombok 生成包含所有字段的构造方法，方便测试或批量创建对象。
@AllArgsConstructor
// 让 Lombok 生成无参构造方法，Spring Data MongoDB 反序列化时会用到。
@NoArgsConstructor
// 声明当前实体映射到 MongoDB 的 chat_messages 集合。
@Document("chat_messages")
// 定义聊天记忆 MongoDB 文档实体。
public class ChatMessages {
    // MongoDB 自己维护的文档 ID。
    @Id
    private ObjectId id;

    // 用于区分不同会话的 ID。
    /**
     * Main conversations use a Long id; worker memories use a namespaced String
     * such as conversation:123:worker:hospital. Object keeps both formats
     * compatible with existing Mongo documents.
     */
    private Object memoryId;

    // 保存 LangChain4j 聊天消息序列化后的 JSON 字符串。
    private String content;
}
