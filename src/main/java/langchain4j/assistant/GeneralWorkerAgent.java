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
        tools = "webSearchTool",
        chatMemoryProvider = "generalWorkerMemoryProvider"
)
public interface GeneralWorkerAgent {
    @SystemMessage(fromResource = "prompts/general_agent.md")
    String handle(
            @MemoryId String memoryId,
            @UserMessage String currentUserMessage,
            @V("taskEnvelope") String taskEnvelope
    );
}
