package langchain4j.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DateTools {

    // log 记录后端日期解析行为，便于排查用户相对日期被解析成什么范围。
    private static final Logger log = LoggerFactory.getLogger(DateTools.class);

    // FORMATTER 统一把内部日期格式化为工具和数据库都能接受的 yyyy-MM-dd。
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // DIGIT_PATTERN 用来从“未来3天”这类文本中提取阿拉伯数字。
    private static final Pattern DIGIT_PATTERN = Pattern.compile("(\\d+)");

    // DEFAULT_FUTURE_DAYS 表示“未来几天”没有明确数字时默认查询未来 3 天。
    private static final int DEFAULT_FUTURE_DAYS = 3;

    // MAX_FUTURE_DAYS 限制一次相对日期展开的天数，避免用户一句话触发过大查询范围。
    private static final int MAX_FUTURE_DAYS = 14;

    // calculateFutureDates 保留旧方法名，方便已有测试和后续 Java 编排代码继续复用。
    @Tool(name = "calculateFutureDates", value = "将未来三天、未来一周、明天、最近两周等相对日期转换为今天之后的精确日期列表。不要自行猜测日期。")
    public String calculateFutureDates(@P("用户使用的相对日期表达，例如未来三天、明天、最近一周") String relativeTime) {
        // 记录原始输入，方便定位日期理解问题。
        log.info("Calculate future dates. relativeTime={}", relativeTime);
        // 将相对日期解析成具体 LocalDate 列表。
        List<LocalDate> dates = resolveFutureDates(relativeTime, LocalDate.now());
        // 将 LocalDate 列表转换成 yyyy-MM-dd 字符串列表。
        List<String> formattedDates = dates.stream().map(FORMATTER::format).toList();
        // 返回兼容旧测试和人工调试的中文结果。
        return "转换结果：" + String.join(", ", formattedDates);
    }

    // resolveAppointmentDateRange 把预约场景中的相对日期表达解析成起止日期。
    DateRange resolveAppointmentDateRange(String relativeTime, LocalDate today) {
        // normalized 避免 null 和空白字符影响后续判断。
        String normalized = relativeTime == null ? "" : relativeTime.trim();
        // 空表达式无法可靠解析，返回 null 交给调用方提示用户补充。
        if (normalized.isEmpty()) {
            // null 表示解析失败。
            return null;
        }
        // 如果用户表达的是“下周一之前”这类边界范围，就按“今天到目标日期前一天”处理。
        if (normalized.contains("之前")) {
            // 解析“之前”前面的目标日期。
            LocalDate boundaryDate = resolveBoundaryDateBeforeKeyword(normalized, today);
            // 如果无法解析边界日期，就返回 null。
            if (boundaryDate == null) {
                // null 表示解析失败。
                return null;
            }
            // endDate 使用边界日期前一天，因为“之前”通常不包含边界当天。
            LocalDate endDate = boundaryDate.minusDays(1);
            // 如果边界前一天早于今天，说明这个范围已经没有未来可预约日期。
            if (endDate.isBefore(today)) {
                // null 表示没有有效未来范围。
                return null;
            }
            // 返回从今天到边界前一天的范围。
            return new DateRange(today, endDate);
        }
        // 如果用户表达的是“下周”但没有具体星期，就返回下周一到下周日。
        if (normalized.contains("下周") && extractNextWeekDay(normalized) == null) {
            // 计算下周一。
            LocalDate nextMonday = nextWeekDate(today, DayOfWeek.MONDAY);
            // 返回下周一到下周日。
            return new DateRange(nextMonday, nextMonday.plusDays(6));
        }
        // 如果用户表达的是“本周”，就返回今天到本周日。
        if (normalized.contains("本周") || normalized.contains("这周")) {
            // 计算本周日还差几天。
            int daysUntilSunday = DayOfWeek.SUNDAY.getValue() - today.getDayOfWeek().getValue();
            // 返回今天到本周日。
            return new DateRange(today, today.plusDays(Math.max(0, daysUntilSunday)));
        }
        // 将单日或未来多日表达复用已有日期列表解析。
        List<LocalDate> dates = resolveFutureDates(relativeTime, today);
        // 没有解析出日期时返回 null。
        if (dates.isEmpty()) {
            // null 表示解析失败。
            return null;
        }
        // 返回日期列表的第一天和最后一天，形成范围。
        return new DateRange(dates.get(0), dates.get(dates.size() - 1));
    }

    // resolveFutureDates 提供可测试的纯 Java 解析入口，不暴露给 LLM 作为工具。
    List<LocalDate> resolveFutureDates(String relativeTime, LocalDate today) {
        // normalized 避免 null 和空白字符影响后续 contains 判断。
        String normalized = relativeTime == null ? "" : relativeTime.trim();
        // results 保存解析出的具体日期。
        List<LocalDate> results = new ArrayList<>();

        // “明天”明确表示今天之后 1 天。
        if (normalized.contains("明天")) {
            // 加入明天日期。
            results.add(today.plusDays(1));
            // 返回明确日期结果。
            return results;
        }

        // “后天”明确表示今天之后 2 天。
        if (normalized.contains("后天")) {
            // 加入后天日期。
            results.add(today.plusDays(2));
            // 返回明确日期结果。
            return results;
        }

        // “未来几天”和“未来N天”表示从明天开始的一段连续日期。
        if (normalized.contains("未来") && normalized.contains("天")) {
            // 提取天数，未提取到时使用默认 3 天。
            int days = extractDays(normalized);
            // 按天数展开为从明天开始的日期列表。
            for (int i = 1; i <= days; i++) {
                // 加入第 i 个未来日期。
                results.add(today.plusDays(i));
            }
            // 返回连续日期结果。
            return results;
        }

        // “下周一”到“下周日”表示下一周的指定星期。
        DayOfWeek requestedDayOfWeek = extractNextWeekDay(normalized);
        // 如果文本中包含可识别的下周星期，就计算对应日期。
        if (requestedDayOfWeek != null) {
            // 加入目标日期。
            results.add(nextWeekDate(today, requestedDayOfWeek));
            // 返回下周目标日期。
            return results;
        }




        // 不能明确识别时，默认展开未来 3 天，供后端兜底编排使用。
        for (int i = 1; i <= DEFAULT_FUTURE_DAYS; i++) {
            // 加入第 i 个未来日期。
            results.add(today.plusDays(i));
        }
        // 返回默认日期结果。
        return results;
    }

    // extractDays 从文本中提取查询天数，并做上限保护。
    private int extractDays(String text) {
        // matcher 负责匹配文本中的阿拉伯数字。
        Matcher matcher = DIGIT_PATTERN.matcher(text);
        // 如果找到数字，就使用该数字作为天数。
        if (matcher.find()) {
            // days 是用户明确提供的未来天数。
            int days = Integer.parseInt(matcher.group(1));
            // 返回限制在 1 到 MAX_FUTURE_DAYS 之间的天数。
            return Math.max(1, Math.min(days, MAX_FUTURE_DAYS));
        }
        // 支持“未来三天”这类中文数字表达。
        if (text.contains("三")) {
            // 返回 3 天。
            return 3;
        }
        // 支持“未来两天”这类中文数字表达。
        if (text.contains("两") || text.contains("二")) {
            // 返回 2 天。
            return 2;
        }
        // 支持“未来一天”这类中文数字表达。
        if (text.contains("一")) {
            // 返回 1 天。
            return 1;
        }
        // 未识别数字时返回默认未来 3 天。
        return DEFAULT_FUTURE_DAYS;
    }

    // resolveBoundaryDateBeforeKeyword 解析“xxx之前”里的边界日期。
    private LocalDate resolveBoundaryDateBeforeKeyword(String text, LocalDate today) {
        // 取出“之前”前面的部分，例如“下周一之前”会得到“下周一”。
        String boundaryText = text.substring(0, text.indexOf("之前"));
        // 如果边界文本为空，就无法解析。
        if (boundaryText.isBlank()) {
            // null 表示解析失败。
            return null;
        }
        // 复用已有日期解析能力获取边界日期。
        List<LocalDate> boundaryDates = resolveFutureDates(boundaryText, today);
        // 没有解析到边界日期时返回 null。
        if (boundaryDates.isEmpty()) {
            // null 表示解析失败。
            return null;
        }
        // 取第一个日期作为范围边界。
        return boundaryDates.get(0);
    }

    // nextWeekDate 计算“下周几”对应的真实日期。
    private LocalDate nextWeekDate(LocalDate today, DayOfWeek targetDayOfWeek) {
        // 计算从今天到目标星期需要经过的天数。
        int daysToAdd = targetDayOfWeek.getValue() - today.getDayOfWeek().getValue();
        // 下周语义要求目标日期必须落在未来 1 到 7 天之后。
        int normalizedDaysToAdd = daysToAdd <= 0 ? daysToAdd + 7 : daysToAdd;
        // 返回计算后的目标日期。
        return today.plusDays(normalizedDaysToAdd);
    }

    // extractNextWeekDay 识别“下周一”到“下周日”。
    private DayOfWeek extractNextWeekDay(String text) {
        // 如果文本不包含下周，就不进入星期解析。
        if (!text.contains("下周")) {
            // null 表示没有识别到下周星期。
            return null;
        }
        // 识别“下周一”。
        if (text.contains("一")) {
            // 返回星期一。
            return DayOfWeek.MONDAY;
        }
        // 识别“下周二”。
        if (text.contains("二")) {
            // 返回星期二。
            return DayOfWeek.TUESDAY;
        }
        // 识别“下周三”。
        if (text.contains("三")) {
            // 返回星期三。
            return DayOfWeek.WEDNESDAY;
        }
        // 识别“下周四”。
        if (text.contains("四")) {
            // 返回星期四。
            return DayOfWeek.THURSDAY;
        }
        // 识别“下周五”。
        if (text.contains("五")) {
            // 返回星期五。
            return DayOfWeek.FRIDAY;
        }
        // 识别“下周六”。
        if (text.contains("六")) {
            // 返回星期六。
            return DayOfWeek.SATURDAY;
        }
        // 识别“下周日”或“下周天”。
        if (text.contains("日") || text.contains("天")) {
            // 返回星期日。
            return DayOfWeek.SUNDAY;
        }
        // 没有识别到具体星期时返回 null。
        return null;
    }

    // DateRange 表示预约日期范围。
    record DateRange(
            // startDate 表示范围开始日期。
            LocalDate startDate,
            // endDate 表示范围结束日期。
            LocalDate endDate
    ) {
    }
}
