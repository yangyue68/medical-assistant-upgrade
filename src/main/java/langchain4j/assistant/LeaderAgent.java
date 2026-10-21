package langchain4j.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "qwenChatModel")
public interface LeaderAgent {

    @SystemMessage(fromResource = "prompts/leader_prompt.md")
    String handle(
            @UserMessage String request,
            @dev.langchain4j.service.V("mainConversationHistory") String mainConversationHistory,
            @dev.langchain4j.service.V("workerResult") String workerResult
    );
}
