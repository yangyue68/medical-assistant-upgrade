package langchain4j.tools;

import dev.langchain4j.agent.tool.Tool;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class DateToolsTest {

    // FORMATTER 用来把测试中的 LocalDate 格式化为 yyyy-MM-dd。
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // dateTools 是被测的后端日期解析组件。
    private final DateTools dateTools = new DateTools();

    // 验证“明天”会解析为今天之后一天。
    @Test
    void shouldCalculateTomorrow() {
        // 调用旧兼容方法解析“明天”。
        String result = dateTools.calculateFutureDates("明天");

        // 断言结果包含明天的具体日期。
        assertThat(result).contains(LocalDate.now().plusDays(1).format(FORMATTER));
    }

    // 验证“未来3天”会展开为连续三天。
    @Test
    void shouldCalculateMultipleFutureDays() {
        // 调用旧兼容方法解析“未来3天”。
        String result = dateTools.calculateFutureDates("未来3天");

        // 断言结果包含未来三天。
        assertThat(result)
                .contains(LocalDate.now().plusDays(1).format(FORMATTER))
                .contains(LocalDate.now().plusDays(2).format(FORMATTER))
                .contains(LocalDate.now().plusDays(3).format(FORMATTER));
    }

    // 验证 2026-07-28 星期二时，“下周一之前”应解析到 2026-08-02。
    @Test
    void shouldResolveBeforeNextMondayFromTuesday() {
        // 固定今天为 2026-07-28，这一天是星期二。
        LocalDate today = LocalDate.parse("2026-07-28");
        // 解析用户说的“下周一之前”。
        DateTools.DateRange dateRange = dateTools.resolveAppointmentDateRange("下周一之前", today);

        // 断言范围从今天开始。
        assertThat(dateRange.startDate()).isEqualTo(LocalDate.parse("2026-07-28"));
        // 断言“下周一之前”不包含下周一当天，所以结束在 2026-08-02。
        assertThat(dateRange.endDate()).isEqualTo(LocalDate.parse("2026-08-02"));
    }

    // 多 Agent 阶段要求医院业务 Worker 使用日期工具消除相对日期歧义。
    @Test
    void shouldExposeDateCalculationAsAgentTool() throws Exception {
        // 通过反射获取日期解析方法。
        var method = DateTools.class.getMethod("calculateFutureDates", String.class);

        Tool tool = method.getAnnotation(Tool.class);
        assertThat(tool).isNotNull();
        assertThat(tool.name()).isEqualTo("calculateFutureDates");
    }
}
