package langchain4j;

/**
 * @author yangyue
 * @date 2026/4/9 22:45
 * @desc TODO
 */

import langchain4j.assistant.XiaoZhiAgent;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
@Disabled("Manual integration test: requires live LLM, database, vector store, and valid API keys.")
public class XiaoZhiTest {

    @Autowired
    XiaoZhiAgent xiaoZhiAgent;

    @Test
    public void testXiaoZhi() {
        Flux<String> chat = xiaoZhiAgent.chat(1L, "我头疼");
        System.out.println(chat.blockFirst());

    }


}
