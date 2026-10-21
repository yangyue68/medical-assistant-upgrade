package langchain4j.auth;

import langchain4j.multiagent.MultiAgentConversationState;
import langchain4j.memory.ConversationSession;
import langchain4j.entity.Appointment;
import langchain4j.mapper.AppointmentMapper;
import langchain4j.bean.ChatMessages;
import langchain4j.store.MongoChatMemoryStore;
import java.util.List;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDeletionService {
    private final AccountService accountService;
    private final MongoTemplate mongoTemplate;
    private final AppointmentMapper appointmentMapper;

    public AccountDeletionService(AccountService accountService, MongoTemplate mongoTemplate,
                                  AppointmentMapper appointmentMapper) {
        this.accountService = accountService;
        this.mongoTemplate = mongoTemplate;
        this.appointmentMapper = appointmentMapper;
    }

    @Transactional
    public void deleteAccount(Long userId) {
        accountService.findRequired(userId);
        List<ConversationSession> sessions = mongoTemplate.find(
                new Query(Criteria.where("userId").is(userId)), ConversationSession.class);
        for (ConversationSession session : sessions) {
            Long memoryId = session.getMemoryId();
            mongoTemplate.remove(new Query(Criteria.where("memoryId").is(memoryId)), ChatMessages.class);
            deleteWorkerMemory("conversation:" + memoryId + ":worker:hospital");
            deleteWorkerMemory("conversation:" + memoryId + ":worker:rag");
            deleteWorkerMemory("conversation:" + memoryId + ":worker:general");
            mongoTemplate.remove(new Query(Criteria.where("conversationId").is(memoryId)), MultiAgentConversationState.class);
        }
        mongoTemplate.remove(new Query(Criteria.where("userId").is(userId)), ConversationSession.class);
        appointmentMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getUserId, userId));
        accountService.delete(userId);
    }

    private void deleteWorkerMemory(String memoryId) {
        mongoTemplate.remove(new Query(Criteria.where("memoryId").is(memoryId)), ChatMessages.class);
    }
}
