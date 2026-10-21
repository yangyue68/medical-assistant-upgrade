// 声明当前类所在的包，memory 表示它属于对话记忆和会话管理模块。
package langchain4j.memory;

// List 表示一组会话元数据。
import java.util.List;
// Optional 表示查询结果可能存在也可能不存在。
import java.util.Optional;
// Sort 用来指定 MongoDB 查询排序。
import org.springframework.data.domain.Sort;
// MongoTemplate 是 Spring Data MongoDB 的核心操作对象。
import org.springframework.data.mongodb.core.MongoTemplate;
// Criteria 用来构造 MongoDB 查询条件。
import org.springframework.data.mongodb.core.query.Criteria;
// Query 用来封装 MongoDB 查询。
import org.springframework.data.mongodb.core.query.Query;
// Component 表示把当前类注册成 Spring Bean。
import org.springframework.stereotype.Component;

// 标记当前类为 Spring 组件。
@Component
// 定义 MongoDB 版会话元数据存储。
public class MongoConversationSessionStore implements ConversationSessionStore {

    // 保存 MongoDB 操作对象。
    //MongoTemplate.java是官方提供的文件，不是自定义的文件
    private final MongoTemplate mongoTemplate;

    // 构造器注入 MongoTemplate。
    public MongoConversationSessionStore(
            // Spring 会自动注入 MongoTemplate Bean。
            MongoTemplate mongoTemplate
    ) {
        // 保存 MongoDB 操作对象。
        this.mongoTemplate = mongoTemplate;
    }

    // 保存或更新会话元数据。
    @Override
    public ConversationSession save(ConversationSession session) {
        // 调用 MongoTemplate 保存文档。
        return mongoTemplate.save(session);
    }

    // 查询所有会话元数据。
    @Override
    public List<ConversationSession> findAll() {
        // 创建按更新时间倒序排序的查询。
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "updatedAt"));
        // 查询所有会话元数据。
        return mongoTemplate.find(query, ConversationSession.class);
    }

    @Override
    public List<ConversationSession> findAllByUserId(Long userId) {
        Query query = new Query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "updatedAt"));
        return mongoTemplate.find(query, ConversationSession.class);
    }

    // 按 memoryId 查询单个会话元数据。
    @Override
    public Optional<ConversationSession> findByMemoryId(Long memoryId) {
        // 创建按 memoryId 精确匹配的查询条件。
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        // 创建 MongoDB 查询对象。
        Query query = new Query(criteria);
        // 查询匹配的单个会话元数据。
        ConversationSession session = mongoTemplate.findOne(query, ConversationSession.class);
        // 把可能为空的查询结果包装成 Optional。
        return Optional.ofNullable(session);
    }

    @Override
    public Optional<ConversationSession> findByMemoryIdAndUserId(Long memoryId, Long userId) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("memoryId").is(memoryId), Criteria.where("userId").is(userId)));
        return Optional.ofNullable(mongoTemplate.findOne(query, ConversationSession.class));
    }

    // 删除指定 memoryId 的会话元数据。
    @Override
    public void deleteByMemoryId(Long memoryId) {
        // 创建按 memoryId 精确匹配的查询条件。
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        // 创建 MongoDB 查询对象。
        Query query = new Query(criteria);
        // 删除匹配的会话元数据。
        mongoTemplate.remove(query, ConversationSession.class);
    }

    @Override
    public void deleteByUserId(Long userId) {
        mongoTemplate.remove(new Query(Criteria.where("userId").is(userId)), ConversationSession.class);
    }
}
