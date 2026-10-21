package langchain4j.multiagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultiAgentProtocolParserTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesLeaderControlActions() {
        LeaderDecision call = MultiAgentProtocolParser.leader(mapper,
                "{\"action\":\"CALL_WORKER\",\"worker\":\"GENERAL\",\"taskEnvelope\":\"{}\"}",
                WorkerType.HOSPITAL, "fallback");
        assertThat(call.action()).isEqualTo(LeaderAction.CALL_WORKER);
        assertThat(call.worker()).isEqualTo(WorkerType.GENERAL);
        assertThat(call.taskEnvelope()).isEqualTo("{}");
    }

    @Test
    void parsesWorkerStatusesAndPendingInformation() {
        WorkerResult result = MultiAgentProtocolParser.worker(mapper,
                "{\"worker\":\"HOSPITAL\",\"status\":\"NEED_USER_INFO\",\"pendingInformation\":[\"手机号\"]}",
                WorkerType.HOSPITAL);
        assertThat(result.status()).isEqualTo(WorkerStatus.NEED_USER_INFO);
        assertThat(result.pendingInformation()).containsExactly("手机号");
    }

    @Test
    void malformedOutputCannotBecomeAnInternalProtocolObject() {
        WorkerResult result = MultiAgentProtocolParser.worker(mapper, "不是JSON", WorkerType.RAG);
        assertThat(result.worker()).isEqualTo(WorkerType.RAG);
        assertThat(result.status()).isEqualTo(WorkerStatus.SUCCESS);
        assertThat(result.answer()).isEqualTo("不是JSON");
    }
    @Test
    void malformedLeaderOutputFallsBackToRoutingInsteadOfBeingShownToUser() {
        LeaderDecision decision = MultiAgentProtocolParser.leader(mapper, "internal chain", WorkerType.RAG, "{}");
        assertThat(decision.action()).isEqualTo(LeaderAction.CALL_WORKER);
        assertThat(decision.worker()).isEqualTo(WorkerType.RAG);
        assertThat(decision.answer()).isEmpty();
    }
}
