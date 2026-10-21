package langchain4j;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.HuggingFaceTokenizer;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.IngestionResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.List;

import static java.awt.SystemColor.text;

/**
 * @author yangyue
 * @date 2026/4/10 17:21
 * @desc TODO
 */

@SpringBootTest
@Disabled("Manual RAG experiments: depends on local absolute paths and document fixtures outside the repo.")
public class RAGTest {

    @Test
    public void testReadDocument() {
        Document document = FileSystemDocumentLoader.loadDocument("D:\\尚硅谷课程\\医疗小智\\资料\\文生图.txt");
        System.out.println(document);
    }

    @Test
    public void testLoadDocument() {
        Document document = FileSystemDocumentLoader.loadDocument("D:\\尚硅谷课程\\医疗小智\\资料\\文生图.txt", new TextDocumentParser());
        System.out.println(document);
    }

    @Test
    public void testLoadDocumentWithFile() {
        // 从一个目录中加载所有的.txt文档
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*.md");
        List<Document> documents = FileSystemDocumentLoader.loadDocuments("E:\\自学课程合集\\王道java长期班\\java长期班教案\\java-60-course-materials\\01-SE\\02_note", pathMatcher, new TextDocumentParser());
        for (Document document : documents) {
            Metadata metadata = document.metadata();
            System.out.println(metadata);
        }
    }

    @Test
    public void testLoadDocumentWithDirectory() {
        Document document = FileSystemDocumentLoader.loadDocument("D:\\尚硅谷课程\\医疗小智\\尚硅谷-Java+大模型应用-硅谷小智（医疗版）(1).pdf", new ApachePdfBoxDocumentParser());
        System.out.println(document);
    }


    @Test
    public void testLoadDocumentWithFileAndDirectory() {
        // 从一个目录及其子目录中加载所有文档
        List<Document> documents = FileSystemDocumentLoader.loadDocumentsRecursively(
                "D:\\尚硅谷课程\\医疗小智\\java-ai-langchain4j\\src\\test", new TextDocumentParser());
        for (Document document : documents) {
            System.out.println(document.metadata());
        }

    }


    @Test
    public void testLoadDocumentWithFileAndDirectoryAndFile() {
        Document document = FileSystemDocumentLoader.loadDocument("D:\\尚硅谷课程\\医疗小智\\资料\\文生图.txt");
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(document, embeddingStore);

        System.out.println(embeddingStore);
    }



    @Test
    public void testLoadDocumentWithFileAndDirectoryAndDirectory() {
        Document document = FileSystemDocumentLoader.loadDocument("D:\\尚硅谷课程\\医疗小智\\资料\\文生图.txt");
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(
                300, 30, new HuggingFaceTokenizer()
        );

        IngestionResult ingest = EmbeddingStoreIngestor
                .builder()
                .embeddingStore(store)
                .documentSplitter(splitter)
                .build()
                .ingest(document);
        System.out.println(ingest);
    }



    @Test
    public void test1() {
        String text = "这是一个示例文本，用于测试 token 长度的计算。";
        UserMessage userMessage = UserMessage.userMessage(text);
        HuggingFaceTokenizer huggingFaceTokenizer = new HuggingFaceTokenizer();
        int count = huggingFaceTokenizer.estimateTokenCountInMessage(userMessage);
        System.out.println(count);
    }




}
