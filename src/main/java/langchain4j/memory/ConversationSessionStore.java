// 声明当前接口所在的包，memory 表示它属于对话记忆和会话管理模块。
package langchain4j.memory;

// List 表示一组会话元数据。
import java.util.List;
// Optional 表示查询结果可能存在也可能不存在。
import java.util.Optional;

// 定义会话元数据存储接口。
public interface ConversationSessionStore {

    // 保存或更新会话元数据。
    ConversationSession save(ConversationSession session);

    // 查询所有会话元数据。
    List<ConversationSession> findAll();

    default List<ConversationSession> findAllByUserId(Long userId) { return findAll(); }

    // 按 memoryId 查询单个会话元数据。
    Optional<ConversationSession> findByMemoryId(Long memoryId);

    default Optional<ConversationSession> findByMemoryIdAndUserId(Long memoryId, Long userId) {
        return findByMemoryId(memoryId);
    }

    // 删除指定 memoryId 的会话元数据。
    void deleteByMemoryId(Long memoryId);

    default void deleteByUserId(Long userId) { }
}
