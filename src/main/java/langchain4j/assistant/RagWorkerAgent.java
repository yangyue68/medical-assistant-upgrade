package langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel",
        chatMemoryProvider = "ragWorkerMemoryProvider",
        contentRetriever = "contentRetrieverXiaozhiElasticsearch"
)
public interface RagWorkerAgent {
    @SystemMessage(fromResource = "prompts/rag_prompt.md")
    String handle(
            @MemoryId String memoryId,
            @UserMessage String currentUserMessage,
            @V("taskEnvelope") String taskEnvelope
    );
}
