package langchain4j.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Worker memory that keeps the runtime system message first.
 * LangChain4j beta3 appends a newly supplied system message after persisted
 * messages; DashScope then drops it because its API requires system first.
 */
public final class OrderedWorkerChatMemory implements ChatMemory {
    private final Object id;
    private final int maxMessages;
    private final ChatMemoryStore store;
    private final List<ChatMessage> messages;

    public OrderedWorkerChatMemory(Object id, int maxMessages, ChatMemoryStore store) {
        this.id = id;
        this.maxMessages = maxMessages;
        this.store = store;
        this.messages = new ArrayList<>(store.getMessages(id));
    }

    @Override
    public Object id() {
        return id;
    }

    @Override
    public synchronized void add(ChatMessage message) {
        if (message instanceof SystemMessage) {
            messages.removeIf(existing -> existing instanceof SystemMessage);
            messages.add(0, message);
        } else {
            messages.add(message);
        }
        trim();
        store.updateMessages(id, new ArrayList<>(messages));
    }

    @Override
    public synchronized List<ChatMessage> messages() {
        return new ArrayList<>(messages);
    }

    @Override
    public synchronized void clear() {
        messages.clear();
        store.deleteMessages(id);
    }

    private void trim() {
        while (messages.size() > maxMessages) {
            int index = messages.get(0) instanceof SystemMessage ? 1 : 0;
            if (index >= messages.size()) {
                messages.remove(0);
            } else {
                messages.remove(index);
            }
        }
    }
}
