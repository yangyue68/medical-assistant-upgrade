package langchain4j.rag.ingestion;

import langchain4j.rag.MedicalKnowledgeDocument;
import langchain4j.rag.MedicalKnowledgeDocumentType;
import langchain4j.rag.store.InMemoryMedicalKnowledgeChunkStore;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalKnowledgeIngestionServiceTest {

    @Test
    void shouldIngestAlreadyParsedDocuments() {
        InMemoryMedicalKnowledgeChunkStore chunkStore = new InMemoryMedicalKnowledgeChunkStore();
        MedicalKnowledgeIngestionService ingestionService = new MedicalKnowledgeIngestionService(
                new MedicalKnowledgeChunker(),
                chunkStore
        );

        MedicalKnowledgeIngestionResult result = ingestionService.ingestDocuments(
                "中文可信医学来源",
                List.of(parsedDocument("pumch:department-page:department-neikxx", "内科学系"))
        );

        assertThat(result.getDocumentCount()).isEqualTo(1);
        assertThat(result.getChunkCount()).isEqualTo(1);
        assertThat(chunkStore.findAll().get(0).getDocumentId())
                .isEqualTo("pumch:department-page:department-neikxx");
    }

    @Test
    void shouldKeepChunkStoreIdempotentWhenSameDocumentsAreIngestedTwice() {
        InMemoryMedicalKnowledgeChunkStore chunkStore = new InMemoryMedicalKnowledgeChunkStore();
        MedicalKnowledgeIngestionService ingestionService = new MedicalKnowledgeIngestionService(
                new MedicalKnowledgeChunker(),
                chunkStore
        );
        List<MedicalKnowledgeDocument> documents = List.of(parsedDocument("pumch:department-page:department-neikxx", "内科学系"));

        MedicalKnowledgeIngestionResult firstResult = ingestionService.ingestDocuments("北京协和医院科室页面", documents);
        MedicalKnowledgeIngestionResult secondResult = ingestionService.ingestDocuments("北京协和医院科室页面", documents);

        assertThat(secondResult.getChunkCount()).isEqualTo(firstResult.getChunkCount());
        assertThat(chunkStore.count()).isEqualTo(firstResult.getChunkCount());
    }

    private static MedicalKnowledgeDocument parsedDocument(String documentId, String department) {
        MedicalKnowledgeDocument document = new MedicalKnowledgeDocument();
        document.setDocumentId(documentId);
        document.setTitle("北京协和医院" + department + "科室介绍");
        document.setText("北京协和医院" + department + "提供患者版科室介绍，用于说明科室历史、诊疗范围和就诊方向。");
        document.setLanguage("Chinese");
        document.setSourceName("北京协和医院");
        document.setSourceUrl("https://www.pumch.cn/department_neikxx.html");
        document.setDocumentType(MedicalKnowledgeDocumentType.HOSPITAL_INFO);
        document.setDepartment(department);
        document.setDiseaseOrTopic(department);
        document.setRetrievedAt(LocalDate.of(2026, 7, 31));
        return document;
    }
}
