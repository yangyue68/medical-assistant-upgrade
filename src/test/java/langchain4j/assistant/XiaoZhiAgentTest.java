package langchain4j.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XiaoZhiAgentTest {
    @Test
    void leaderShouldOnlyRouteWithoutToolsOrMemory() {
        AiService aiService = LeaderAgent.class.getAnnotation(AiService.class);
        assertThat(aiService.tools()).isEmpty();
        assertThat(aiService.chatMemoryProvider()).isEmpty();
        assertThat(LeaderAgent.class.getDeclaredMethods()[0].getParameters()[1]
                .getAnnotation(V.class).value()).isEqualTo("mainConversationHistory");
        SystemMessage system = LeaderAgent.class.getDeclaredMethods()[0].getAnnotation(SystemMessage.class);
        assertThat(system.fromResource()).isEqualTo("prompts/leader_prompt.md");
    }

    @Test
    void workersReceiveCurrentMessageAndMainHistory() {
        assertThat(HospitalWorkerAgent.class.getDeclaredMethods()[0].getParameters()[1].isAnnotationPresent(UserMessage.class)).isTrue();
        assertThat(RagWorkerAgent.class.getDeclaredMethods()[0].getParameters()[1].isAnnotationPresent(UserMessage.class)).isTrue();
        assertThat(GeneralWorkerAgent.class.getDeclaredMethods()[0].getParameters()[1].isAnnotationPresent(UserMessage.class)).isTrue();
        assertThat(resource(HospitalWorkerAgent.class)).isEqualTo("prompts/hospital_agent.md");
        assertThat(resource(RagWorkerAgent.class)).isEqualTo("prompts/rag_prompt.md");
        assertThat(resource(GeneralWorkerAgent.class)).isEqualTo("prompts/general_agent.md");
    }

    private String resource(Class<?> type) {
        return type.getDeclaredMethods()[0].getAnnotation(SystemMessage.class).fromResource();
    }
}
