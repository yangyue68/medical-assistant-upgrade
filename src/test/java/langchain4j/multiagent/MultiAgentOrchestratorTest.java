package langchain4j.multiagent;

import langchain4j.assistant.GeneralWorkerAgent;
import langchain4j.assistant.HospitalWorkerAgent;
import langchain4j.assistant.LeaderAgent;
import langchain4j.assistant.RagWorkerAgent;
import langchain4j.memory.ChatMemoryPolicy;
import langchain4j.store.MongoChatMemoryStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import langchain4j.store.MongoChatMemoryStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MultiAgentOrchestratorTest {
    @Test
    void deterministicHospitalRouteReturnsThroughLeader() {
        Fixture f = fixture();
        when(f.hospital.handle(eq("conversation:1001:worker:hospital"), eq("帮我查一下神经内科排班"), anyString())).thenReturn("{\"worker\":\"HOSPITAL\",\"status\":\"SUCCESS\",\"answer\":\"排班结果\"}");
        when(f.leader.handle(anyString(), anyString(), anyString())).thenReturn("{\"action\":\"FINAL\",\"answer\":\"排班结果\"}");
        assertThat(f.orchestrator.process(1001L, "帮我查一下神经内科排班")).isEqualTo("排班结果");
        verify(f.leader).handle(anyString(), anyString(), anyString());
        verify(f.memoryStore).updateMessages(eq(1001L), anyList());
    }

    @Test
    void leaderFallbackReceivesOnlyCurrentMessageAndMainHistory() {
        Fixture f = fixture();
        when(f.leader.handle(anyString(), anyString(), eq(""))).thenReturn("{\"action\":\"CALL_WORKER\",\"worker\":\"RAG\"}");
        when(f.rag.handle(eq("conversation:1001:worker:rag"), eq("请讲讲这个"), anyString())).thenReturn("{\"worker\":\"RAG\",\"status\":\"SUCCESS\",\"answer\":\"知识介绍\"}");
        when(f.leader.handle(anyString(), anyString(), contains("知识介绍"))).thenReturn("{\"action\":\"FINAL\",\"answer\":\"知识介绍\"}");
        assertThat(f.orchestrator.process(1001L, "请讲讲这个")).isEqualTo("知识介绍");
        ArgumentCaptor<String> input = ArgumentCaptor.forClass(String.class);
        verify(f.leader).handle(input.capture(), anyString(), eq(""));
        assertThat(input.getValue()).isEqualTo("请讲讲这个");
        assertThat(input.getValue()).doesNotContain("action", "workerAnswer", "repairAdvice", "交接");
    }

    @Test
    void mainMemoryContainsOnlyFinalUserAndAssistantExchange() {
        Fixture f = fixture();
        when(f.general.handle(anyString(), anyString(), anyString())).thenReturn("{\"worker\":\"GENERAL\",\"status\":\"SUCCESS\",\"answer\":\"回答\"}");
        when(f.leader.handle(anyString(), anyString(), contains("回答"))).thenReturn("{\"action\":\"FINAL\",\"answer\":\"回答\"}");
        f.orchestrator.process(1001L, "你好");
        ArgumentCaptor<List<ChatMessage>> saved = ArgumentCaptor.forClass(List.class);
        verify(f.memoryStore).updateMessages(eq(1001L), saved.capture());
        assertThat(saved.getValue()).hasSize(2);
        assertThat(saved.getValue().get(0)).isInstanceOf(UserMessage.class);
    }

    @Test
    void identityQuestionNeverExposesProviderAnswer() {
        Fixture f = fixture();
        when(f.general.handle(anyString(), anyString(), anyString()))
                .thenReturn("{\"worker\":\"GENERAL\",\"status\":\"SUCCESS\",\"answer\":\"我是来自阿里云的超大规模语言模型，我叫通义千问\"}");
        when(f.leader.handle(anyString(), anyString(), contains("阿里云"))).thenReturn("{\"action\":\"FINAL\",\"answer\":\"我是来自阿里云的超大规模语言模型，我叫通义千问\"}");
        assertThat(f.orchestrator.process(1001L, "你是谁？只能告诉我你的角色"))
                .doesNotContain("阿里云", "通义千问", "Qwen")
                .contains("医疗小智");
    }

    private static Fixture fixture() {
        LeaderAgent leader = mock(LeaderAgent.class);
        HospitalWorkerAgent hospital = mock(HospitalWorkerAgent.class);
        RagWorkerAgent rag = mock(RagWorkerAgent.class);
        GeneralWorkerAgent general = mock(GeneralWorkerAgent.class);
        MongoChatMemoryStore memory = mock(MongoChatMemoryStore.class);
        when(memory.getMessages(1001L)).thenReturn(List.of());
        MultiAgentOrchestrator orchestrator = new MultiAgentOrchestrator(
                leader, hospital, rag, general, memory, new ChatMemoryPolicy(20, 4));
        return new Fixture(orchestrator, leader, hospital, rag, general, memory);
    }

    private record Fixture(MultiAgentOrchestrator orchestrator, LeaderAgent leader,
                           HospitalWorkerAgent hospital, RagWorkerAgent rag,
                           GeneralWorkerAgent general, MongoChatMemoryStore memoryStore) {}
}
