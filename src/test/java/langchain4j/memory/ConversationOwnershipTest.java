package langchain4j.memory;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import langchain4j.store.MongoChatMemoryStore;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConversationOwnershipTest {
    @Test
    void rejectsReadingAnotherUsersConversation() {
        MongoChatMemoryStore memoryStore = mock(MongoChatMemoryStore.class);
        ConversationSessionStore sessions = mock(ConversationSessionStore.class);
        when(sessions.findByMemoryIdAndUserId(99L, 2L)).thenReturn(Optional.empty());
        ConversationMemoryService service = new ConversationMemoryService(memoryStore, sessions);
        assertThatThrownBy(() -> service.listConversationMessages(2L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }
}
