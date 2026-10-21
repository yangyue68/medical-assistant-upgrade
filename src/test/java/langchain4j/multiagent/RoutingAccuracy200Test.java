package langchain4j.multiagent;

import langchain4j.assistant.GeneralWorkerAgent;
import langchain4j.assistant.HospitalWorkerAgent;
import langchain4j.assistant.LeaderAgent;
import langchain4j.assistant.RagWorkerAgent;
import langchain4j.memory.ChatMemoryPolicy;
import langchain4j.store.MongoChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Public 200-case regression suite; the cases are also documented in docs/routing-test-cases-200.md. */
class RoutingAccuracy200Test {
    @Test
    void deterministicRulesAndConservativeFallbackReachAtLeast98Percent() throws Exception {
        MultiAgentOrchestrator orchestrator = new MultiAgentOrchestrator(
                mock(LeaderAgent.class), mock(HospitalWorkerAgent.class), mock(RagWorkerAgent.class),
                mock(GeneralWorkerAgent.class), mock(MongoChatMemoryStore.class), new ChatMemoryPolicy(20, 4));
        List<String> failures = new ArrayList<>();
        int total = 0;
        int correct = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/routing-test-cases-200.tsv"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\t", -1);
                if (fields.length != 4) {
                    continue;
                }
                total++;
                WorkerType expected = WorkerType.valueOf(fields[3]);
                WorkerType deterministic = orchestrator.deterministicRoute(history(fields[1]), fields[2]);
                // An unresolved case is deliberately sent to the lowest-priority GENERAL fallback.
                WorkerType actual = deterministic == null ? WorkerType.GENERAL : deterministic;
                if (actual == expected) {
                    correct++;
                } else {
                    failures.add(fields[0] + ": expected=" + expected + ", actual=" + actual + ", message=" + fields[2]);
                }
            }
        }
        double accuracy = (double) correct / total;
        assertThat(total).isEqualTo(200);
        assertThat(accuracy).as("routing failures: %s", failures).isGreaterThanOrEqualTo(0.98);
    }

    private List<ChatMessage> history(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<ChatMessage> messages = new ArrayList<>();
        for (String part : text.split(" (?=助手：|用户：)")) {
            if (part.startsWith("用户：")) {
                messages.add(UserMessage.from(part.substring(3)));
            }
        }
        return messages;
    }
}
