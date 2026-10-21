package langchain4j;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Manual integration test: requires a running local Ollama service.")
public class OllamaTest {

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @Test
    public void testOllamaModel() {
        String response = ollamaChatModel.chat("我最近头痛，应该挂什么科？");
        System.out.println("模型回答: " + response);
    }
}
