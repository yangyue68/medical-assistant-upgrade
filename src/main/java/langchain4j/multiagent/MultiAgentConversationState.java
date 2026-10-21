package langchain4j.multiagent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("multi_agent_conversation_state")
public class MultiAgentConversationState {
    @Id
    private Long conversationId;
    private WorkerType previousWorker;
    private List<String> facts = new ArrayList<>();
    private List<String> openQuestions = new ArrayList<>();
    private Instant updatedAt;

    public static MultiAgentConversationState empty(Long conversationId) {
        MultiAgentConversationState state = new MultiAgentConversationState();
        state.setConversationId(conversationId);
        state.setUpdatedAt(Instant.now());
        return state;
    }
}
