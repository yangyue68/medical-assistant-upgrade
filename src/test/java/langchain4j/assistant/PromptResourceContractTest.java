package langchain4j.assistant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptResourceContractTest {
    @Test
    void allPromptResourcesExistAndDeclareTheirRuntimeVariables() throws IOException {
        Path root = Path.of("src/main/resources/prompts");
        for (String file : List.of("leader_prompt.md", "hospital_agent.md", "rag_prompt.md", "general_agent.md")) {
            String content = Files.readString(root.resolve(file), StandardCharsets.UTF_8);
            assertThat(content).isNotBlank();
            assertThat(content).contains("{{current_date}}");
            assertThat(content).doesNotContain("通义千问", "阿里云");
        }
        assertThat(Files.readString(root.resolve("leader_prompt.md"), StandardCharsets.UTF_8))
                .contains("{{mainConversationHistory}}", "{{workerResult}}", "CALL_WORKER", "ASK_USER", "FINAL");
        for (String file : List.of("hospital_agent.md", "rag_prompt.md", "general_agent.md")) {
            assertThat(Files.readString(root.resolve(file), StandardCharsets.UTF_8))
                    .contains("{{taskEnvelope}}", "NEED_USER_INFO", "UNSUPPORTED");
        }
    }
}
