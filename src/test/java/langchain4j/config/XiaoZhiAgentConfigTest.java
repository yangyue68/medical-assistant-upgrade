package langchain4j.config;

import langchain4j.memory.ChatMemoryPolicy;
import langchain4j.rag.MedicalKnowledgeRetriever;
import langchain4j.store.MongoChatMemoryStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class XiaoZhiAgentConfigTest {

    @Test
    void shouldUseCurrentUserMessageFromWorkerInputForRagSearch() {
        XiaoZhiAgentConfig config = new XiaoZhiAgentConfig(
                mock(MongoChatMemoryStore.class),
                mock(MedicalKnowledgeRetriever.class),
                new ChatMemoryPolicy(20, 4),
                new ObjectMapper());

        String query = config.extractCurrentUserMessage("当前用户消息：介绍一下神经内科\n主会话记忆（只读）：用户：你好");

        assertThat(query).isEqualTo("介绍一下神经内科");
    }
}
