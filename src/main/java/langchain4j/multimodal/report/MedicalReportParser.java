// 声明医疗报告结构化与解释功能所在的包。
package langchain4j.multimodal.report;

// 引入可变列表实现，用于逐行收集解析结果。
import java.util.ArrayList;
// 引入列表接口，用于结果建模。
import java.util.List;
// 引入正则匹配器，用于读取各捕获组。
import java.util.regex.Matcher;
// 引入预编译正则表达式类型。
import java.util.regex.Pattern;

/** 面向常见纯文本化验单的保守结构化解析器。 */
public class MedicalReportParser {

    // 匹配“指标名 数值 单位 参考下限-参考上限”格式的单行化验项目。
    private static final Pattern ITEM_PATTERN = Pattern.compile(
            // 限制指标名长度，并支持正负数、小数、比较符及多种范围分隔符。
            "^(.{1,30}?)\\s+([<>]?[-+]?\\d+(?:\\.\\d+)?)\\s*([^\\s]*)\\s+([-+]?\\d+(?:\\.\\d+)?)\\s*[-~—至]\\s*([-+]?\\d+(?:\\.\\d+)?)\\s*$"
    );
    // 兼容 OCR 把指标名、数值、单位和参考范围粘连在一起的常见结果。
    private static final Pattern COMPACT_OCR_ITEM_PATTERN = Pattern.compile(
            "^(.{1,30}?)([<>]?[-+]?\\d+(?:\\.\\d+)?)\\s*"
                    + "(10(?:\\^|\\*|[xX×])?\\d+/[A-Za-z]+|[A-Za-zμµ%]+(?:/[A-Za-zμµ]+)?)\\s*"
                    + "([-+]?\\d+(?:\\.\\d+)?)\\s*[-~—至]\\s*([-+]?\\d+(?:\\.\\d+)?)\\s*$"
    );

    // 将提取后的报告纯文本转换为保守的结构化结果。
    public StructuredMedicalReport parse(String fileId, String text) {
        // 没有报告文本时无法执行结构化处理。
        if (text == null || text.trim().isEmpty()) {
            // 返回明确的业务参数错误。
            throw new IllegalArgumentException("报告文本不能为空。");
        }
        // 将 Windows 和旧式换行统一为 \n 后按行切分。
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        // 创建成功解析指标的可变收集列表。
        List<LabReportItem> items = new ArrayList<>();
        // 创建无法解析但可安全返回的文本行列表。
        List<String> unparsed = new ArrayList<>();
        // 在报告未提供名称时使用中性默认标题。
        String title = "医疗报告";
        // 标本类型默认未知。
        String specimen = null;
        // 逐行处理提取后的报告文本。
        for (String rawLine : lines) {
            // 清理首尾空白并把连续空格或制表符合并为单个空格。
            String line = rawLine.trim().replaceAll("[ \\t]+", " ");
            // 空行不参与任何解析和返回。
            if (line.isEmpty()) {
                // 继续处理下一行。
                continue;
            }
            // 识别常见的报告标题字段。
            if (line.startsWith("报告名称") || line.startsWith("报告类型")) {
                // 提取中英文冒号后的标题，提取失败则保留原值。
                title = valueAfterColon(line, title);
                // 标题行不再作为化验项目处理。
                continue;
            }
            // 识别常见的标本或样本字段。
            if (line.startsWith("标本") || line.startsWith("样本")) {
                // 提取中英文冒号后的标本类型。
                specimen = valueAfterColon(line, specimen);
                // 标本行不再作为化验项目处理。
                continue;
            }
            // 使用预编译规则匹配当前化验项目行。
            Matcher matcher = ITEM_PATTERN.matcher(line);
            // 普通规则未命中时，再尝试真实 OCR 常见的无空格紧凑格式。
            if (!matcher.matches()) {
                matcher = COMPACT_OCR_ITEM_PATTERN.matcher(line);
            }
            // 完整匹配时才认为当前行是可信的结构化指标。
            if (matcher.matches()) {
                // 将正则捕获组转换为强类型化验项目。
                items.add(toItem(matcher));
            // 未匹配的个人信息行直接丢弃，不进入接口响应。
            } else if (!looksLikePersonalInformation(line)) {
                // 保存其他未解析行，便于前端提示规则覆盖范围。
                unparsed.add(line);
            }
        }
        // 创建解析告警列表。
        List<String> warnings = new ArrayList<>();
        // 始终提示自动结构化不能替代原始报告和医生解释。
        warnings.add("结构化结果来自自动解析，必须以原始报告和医生解释为准。");
        // 没有识别到任何指标时增加明确告警。
        if (items.isEmpty()) {
            // 说明当前规则需要同时看到数值和参考范围。
            warnings.add("没有识别到包含数值和参考范围的化验指标。");
        }
        // 使用只读列表快照构建最终结构化报告。
        return new StructuredMedicalReport(fileId, title, specimen, List.copyOf(items),
                // 同时返回未解析行和安全告警的只读快照。
                List.copyOf(unparsed), List.copyOf(warnings));
    }

    // 将成功匹配的正则捕获组转换为单项化验指标。
    private LabReportItem toItem(Matcher matcher) {
        // 第一组是指标名称。
        String name = matcher.group(1).trim();
        // 第二组是保留比较符的原始结果。
        String value = matcher.group(2);
        // 第三组是没有空白字符的计量单位。
        String unit = matcher.group(3);
        // 清除比较符后把结果转换为数值。
        double numericValue = number(value);
        // 第四组是报告给出的参考下限。
        double low = Double.parseDouble(matcher.group(4));
        // 第五组是报告给出的参考上限。
        double high = Double.parseDouble(matcher.group(5));
        // 只依据当前报告的参考范围判断低、正常或高。
        LabIndicatorStatus status = numericValue < low ? LabIndicatorStatus.LOW
                // 不引入疾病诊断规则，只做上限比较。
                : numericValue > high ? LabIndicatorStatus.HIGH : LabIndicatorStatus.NORMAL;
        // 构建同时保留原始值和可计算数值的指标对象。
        return new LabReportItem(name, value, unit, low + "-" + high,
                // 写入数值上下限和比较结果。
                numericValue, low, high, status);
    }

    // 将可能带比较符的结果字符串转换为 double。
    private double number(String value) {
        // 去除小于号和大于号后交给 JDK 数值解析器。
        return Double.parseDouble(value.replace("<", "").replace(">", ""));
    }

    // 提取中英文冒号后的字段值。
    private String valueAfterColon(String line, String fallback) {
        // 同时寻找中文全角冒号和英文半角冒号的位置。
        int index = Math.max(line.indexOf('：'), line.indexOf(':'));
        // 冒号后有内容时返回该内容，否则返回调用方提供的默认值。
        return index >= 0 && index + 1 < line.length() ? line.substring(index + 1).trim() : fallback;
    }

    // 判断未解析行是否可能包含高敏感个人信息。
    private boolean looksLikePersonalInformation(String line) {
        // 过滤姓名、身份证号和手机号字段。
        return line.startsWith("姓名") || line.startsWith("身份证") || line.startsWith("手机号")
                // 同时过滤住院号和门诊号字段。
                || line.startsWith("住院号") || line.startsWith("门诊号");
    }
}
