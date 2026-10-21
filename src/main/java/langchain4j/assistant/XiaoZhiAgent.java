package langchain4j.assistant;

import reactor.core.publisher.Flux;


/**
 * @author yangyue
 * @date 2026/4/9 21:10
 * @desc TODO
 */

public interface XiaoZhiAgent {
    Flux<String> chat(Long memoryId, String userMessage);

    default Flux<String> chat(Long userId, Long memoryId, String userMessage) {
        return chat(memoryId, userMessage);
    }
}
