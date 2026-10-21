package langchain4j.rag.ingestion;

import langchain4j.rag.MedicalKnowledgeDocument;
import langchain4j.rag.MedicalKnowledgeDocumentType;
import langchain4j.rag.store.InMemoryMedicalKnowledgeChunkStore;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PumchDepartmentPageIngestionRunnerTest {

    private static final Path DEPARTMENT_PAGE_DIR = Path.of("docs/knowledge/raw/pumch/2026-07-31/department-pages");

    @Test
    void shouldImportFullDepartmentIntroductionsFromDownloadedPumchPages() {
        InMemoryMedicalKnowledgeChunkStore store = new InMemoryMedicalKnowledgeChunkStore();
        MedicalKnowledgeIngestionService service = new MedicalKnowledgeIngestionService(
                new MedicalKnowledgeChunker(),
                store
        );

        MedicalKnowledgeIngestionResult result = new PumchDepartmentPageIngestionRunner(
                new PumchDepartmentPageHtmlParser(),
                service
        ).ingest(DEPARTMENT_PAGE_DIR, LocalDate.of(2026, 7, 31));

        assertThat(result.getDocumentCount()).isEqualTo(51);
        assertThat(result.getChunkCount()).isGreaterThan(result.getDocumentCount());
        assertThat(store.findAll())
                .extracting("documentType")
                .containsOnly(MedicalKnowledgeDocumentType.HOSPITAL_INFO);
        assertThat(store.findAll())
                .extracting("sourceName")
                .containsOnly("北京协和医院");
    }

    @Test
    void shouldExtractInternalMedicineDepartmentNameFromNeikxxPage() {
        PumchDepartmentPageHtmlParser parser = new PumchDepartmentPageHtmlParser();

        MedicalKnowledgeDocument document = parser.parse(
                DEPARTMENT_PAGE_DIR.resolve("department_neikxx.html"),
                LocalDate.of(2026, 7, 31)
        ).orElseThrow();

        assertThat(document.getDepartment()).isEqualTo("内科学系");
        assertThat(document.getDiseaseOrTopic()).isEqualTo("内科学系");
        assertThat(document.getTitle()).isEqualTo("北京协和医院内科学系科室介绍");
        assertThat(document.getText()).contains("北京协和医院内科学系自1921年建院之初即成立");
    }
}
