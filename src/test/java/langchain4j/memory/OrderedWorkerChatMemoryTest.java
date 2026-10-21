package langchain4j.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderedWorkerChatMemoryTest {
    @Test
    void systemMessageIsAlwaysFirstAndIsNotPersisted() {
        Store store = new Store(List.of(UserMessage.from("历史问题"), AiMessage.from("历史回答")));
        OrderedWorkerChatMemory memory = new OrderedWorkerChatMemory("conversation:1:worker:hospital", 10, store);

        memory.add(SystemMessage.from("worker prompt"));
        memory.add(UserMessage.from("当前问题"));

        assertThat(memory.messages()).extracting(ChatMessage::type)
                .containsExactly(ChatMessageType.SYSTEM, ChatMessageType.USER,
                        ChatMessageType.AI, ChatMessageType.USER);
        assertThat(store.saved).noneMatch(message -> message instanceof SystemMessage);
    }

    private static final class Store implements ChatMemoryStore {
        private final List<ChatMessage> saved = new ArrayList<>();

        private Store(List<ChatMessage> initial) {
            saved.addAll(initial);
        }

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            return new ArrayList<>(saved);
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            saved.clear();
            saved.addAll(messages.stream().filter(message -> !(message instanceof SystemMessage)).toList());
        }

        @Override
        public void deleteMessages(Object memoryId) {
            saved.clear();
        }
    }
}
