// 声明测试类所在的包。
package langchain4j.rag.ingestion;

// MedicalKnowledgeChunk 是切分器输出的知识片段。
import langchain4j.rag.MedicalKnowledgeChunk;
// MedicalKnowledgeDocument 是切分器输入的完整知识文档。
import langchain4j.rag.MedicalKnowledgeDocument;
// MedicalKnowledgeDocumentType 是医学知识文档类型枚举。
import langchain4j.rag.MedicalKnowledgeDocumentType;
// LocalDate 表示测试文档获取日期。
import java.time.LocalDate;
// List 表示一组切分后的片段。
import java.util.List;
// Test 是 JUnit 5 的测试方法注解。
import org.junit.jupiter.api.Test;

// AssertJ 断言工具。
import static org.assertj.core.api.Assertions.assertThat;
// AssertJ 异常断言工具。
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 定义医学知识切分器的单元测试。
class MedicalKnowledgeChunkerTest {

    // 测试长文档会被切成多个片段，并保留文档元数据。
    @Test
    // 方法名描述测试意图：切分后应该保留可追溯元数据。
    void shouldSplitDocumentAndKeepMetadata() {
        // 创建切分器，使用较小长度方便测试多片段。
        MedicalKnowledgeChunker chunker = new MedicalKnowledgeChunker(120, 20);
        // 创建测试文档。
        MedicalKnowledgeDocument document = sampleDocument(longText());

        // 执行文档切分。
        List<MedicalKnowledgeChunk> chunks = chunker.split(document);

        // 验证长文档被切成多个片段。
        assertThat(chunks).hasSizeGreaterThan(1);
        // 验证每个片段都不超过最大长度。
        assertThat(chunks).allMatch(chunk -> chunk.getText().length() <= 120);
        // 验证第一个片段 ID 带有文档 ID 和片段序号。
        assertThat(chunks.get(0).getChunkId()).isEqualTo("medlineplus:6308#chunk-0");
        // 验证第一个片段保留原始文档 ID。
        assertThat(chunks.get(0).getDocumentId()).isEqualTo("medlineplus:6308");
        // 验证第一个片段保留标题。
        assertThat(chunks.get(0).getTitle()).isEqualTo("A1C");
        // 验证第一个片段保留来源 URL。
        assertThat(chunks.get(0).getSourceUrl()).isEqualTo("https://medlineplus.gov/a1c.html");
        // 验证第一个片段保留文档类型。
        assertThat(chunks.get(0).getDocumentType()).isEqualTo(MedicalKnowledgeDocumentType.HEALTH_TOPIC);
        // 验证第二个片段序号正确递增。
        assertThat(chunks.get(1).getChunkIndex()).isEqualTo(1);
    }

    // 测试中文长段落的下一片段不会从半句话中间开始。
    @Test
    // 方法名描述测试意图：overlap 应该服务于上下文，而不是制造不自然的片段开头。
    void shouldStartNextChineseChunkAtNaturalSentenceBoundary() {
        // 创建切分器，使用默认生产参数。
        MedicalKnowledgeChunker chunker = new MedicalKnowledgeChunker();
        // 创建急诊科长文档样本。
        MedicalKnowledgeDocument document = sampleDocument(emergencyDepartmentText());
        // 设置中文元数据。
        document.setDocumentId("pumch:department-page:department_jizk");
        document.setTitle("北京协和医院急诊科科室介绍");
        document.setLanguage("Chinese");
        document.setSourceName("北京协和医院");
        document.setSourceUrl("https://www.pumch.cn/department_jizk.html");
        document.setDocumentType(MedicalKnowledgeDocumentType.HOSPITAL_INFO);
        document.setDepartment("急诊科");
        document.setDiseaseOrTopic("急诊科");

        // 执行切分。
        List<MedicalKnowledgeChunk> chunks = chunker.split(document);

        // 验证会切成多个片段。
        assertThat(chunks).hasSizeGreaterThan(1);
        // 第二个片段应从完整句子开始，而不是从“教材，并...”这种半句开始。
        assertThat(chunks.get(1).getText()).startsWith("急诊科近年来特别加强在科研方面的投入");
        assertThat(chunks.get(1).getText()).doesNotStartWith("教材，并");
    }

    // 测试空文档或空正文不会产生片段。
    @Test
    // 方法名描述测试意图：没有正文时不应该产生可检索片段。
    void shouldReturnEmptyChunksWhenDocumentTextIsBlank() {
        // 创建切分器。
        MedicalKnowledgeChunker chunker = new MedicalKnowledgeChunker();
        // 创建正文为空白的文档。
        MedicalKnowledgeDocument document = sampleDocument("   ");

        // 验证空白正文不会产生片段。
        assertThat(chunker.split(document)).isEmpty();
        // 验证空文档不会产生片段。
        assertThat(chunker.split(null)).isEmpty();
    }

    // 测试非法切分参数会被拒绝。
    @Test
    // 方法名描述测试意图：重叠长度不能大于等于片段最大长度。
    void shouldRejectInvalidChunkerOptions() {
        // 验证过小的最大长度会抛异常。
        assertThatThrownBy(() -> new MedicalKnowledgeChunker(50, 10))
                // 验证异常类型。
                .isInstanceOf(IllegalArgumentException.class)
                // 验证异常信息。
                .hasMessageContaining("maxChunkLength");
        // 验证重叠长度大于等于最大长度会抛异常。
        assertThatThrownBy(() -> new MedicalKnowledgeChunker(120, 120))
                // 验证异常类型。
                .isInstanceOf(IllegalArgumentException.class)
                // 验证异常信息。
                .hasMessageContaining("overlapLength");
    }

    // 创建测试用医学知识文档。
    private static MedicalKnowledgeDocument sampleDocument(String text) {
        // 创建文档对象。
        MedicalKnowledgeDocument document = new MedicalKnowledgeDocument();
        // 设置文档 ID。
        document.setDocumentId("medlineplus:6308");
        // 设置标题。
        document.setTitle("A1C");
        // 设置正文。
        document.setText(text);
        // 设置语言。
        document.setLanguage("English");
        // 设置来源名称。
        document.setSourceName("MedlinePlus");
        // 设置来源 URL。
        document.setSourceUrl("https://medlineplus.gov/a1c.html");
        // 设置文档类型。
        document.setDocumentType(MedicalKnowledgeDocumentType.HEALTH_TOPIC);
        // 设置关联科室。
        document.setDepartment("Endocrinology");
        // 设置疾病或主题。
        document.setDiseaseOrTopic("Diabetes");
        // 设置来源创建日期。
        document.setSourceCreatedAt("12/22/2015");
        // 设置来源生成时间。
        document.setSourceGeneratedAt("07/23/2026 02:30:37");
        // 设置本项目获取日期。
        document.setRetrievedAt(LocalDate.of(2026, 7, 24));
        // 返回文档对象。
        return document;
    }

    // 构造长文本，模拟真实医学科普摘要。
    private static String longText() {
        // 返回包含多个句子的长文本。
        return "A1C is a blood test for type 2 diabetes. "
                + "It measures your average blood sugar level over the past three months. "
                + "Doctors may use it to diagnose diabetes or prediabetes. "
                + "People with diabetes may use A1C to monitor whether their treatment plan is working. "
                + "Ask a health care provider what your personal target range should be.";
    }

    // 构造来自协和急诊科页面的长段落切分样本。
    private static String emergencyDepartmentText() {
        return "北京协和医院急诊科成立于1983年4月，是我国第一个独立建制的急诊科，经过40余年的建设与发展，在医教研各方面均取得了长足的进步，临床能力和学术水平国内领先，在2012年和2022年两次获批国家临床重点专科项目，自2015年急诊医学首次进入复旦专科排行榜以来，北京协和医院急诊科连续8年位居榜首。"
                + "急诊科诊疗区域包括预检分诊区、发热门诊、急诊诊区、抢救室、输液区、留观区、综合病房和EICU，总计一百余张床位，现拥有四十余名急诊医生和一百四十余名急诊护士，每年招收临床博士后4-5名，博士研究生5-6名，硕士研究生5-6名，规培住院医师4-5名，还常年接收来自全国各地三级医院急诊科的进修医师30余人和进修护士30余人。"
                + "目前是国家急诊质控中心主任单位、北京市急诊质控中心主任单位、中国医师协会急诊医师分会前任及候任会长单位、中华医学会急诊医学分会前主任委员单位、中国医师协会毕业后医学教育急诊科专业委员会主任委员单位、国家住院医师规范化培训重点专业基地和北京市临床技能培训和考核中心。"
                + "北京协和医院急诊科年均接诊急诊患者20余万人次，日均急诊量600-700人次且逐年增加，疑难危重患者比例在国内急诊名列前茅。"
                + "急诊科还在很多大型活动及重要会议的医疗保障工作中勇担重任，并在灾难救援及突发公共卫生事件的应急处置中发挥重要作用。"
                + "除了临床工作之外，北京协和医院急诊科非常重视教学工作，主编多部急诊医学教材，并在国内大力推广小班互动式培训课程，包括针对临床技能和思维培训的E-Training课程，针对师资胜任力培训的I-Teacher课程和针对科研能力培训的V-Trial课程，在课程中融入先进的教学理念，培训效果显著，在急诊领域享有盛誉。"
                + "急诊科近年来特别加强在科研方面的投入，获多项国家级、省部级和院校级课题的资助，获多项发明专利授权并实现成果转化，发表的SCI文章数量和质量不断提升，连续多年位居中国医院科技量值（STEM）排行榜前列，在心肺复苏、脓毒症等方面的研究取得了较大的成果。"
                + "北京协和医院急诊科一贯坚持以病人为中心，以临床为根本，视急诊医疗质量和安全为生命，不断完善制度建设，推进改革创新，注重人才培养，强化党建引领，努力建设国内领先、国际一流的急诊医学中心。";
    }
}
