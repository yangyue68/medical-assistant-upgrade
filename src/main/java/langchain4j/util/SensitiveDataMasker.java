// 声明当前工具类所在的包；util 表示它是通用工具，不属于某个具体业务模块。
package langchain4j.util;

// Matcher 用来执行正则匹配，并支持逐段替换文本。
import java.util.regex.Matcher;
// Pattern 是 Java 中编译后的正则表达式对象，复用它比每次重新编译正则更高效。
import java.util.regex.Pattern;

// final 表示这个工具类不能被继承，避免别人通过继承改变工具类语义。
public final class SensitiveDataMasker {

    // 身份证号正则：捕获前 6 位和后 4 位，中间 8 位会被隐藏。
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(?<!\\d)(\\d{6})\\d{8}(\\d{3}[0-9Xx])(?![0-9A-Za-z])");
    // 手机号正则：捕获前 3 位和后 4 位，中间 4 位会被隐藏。
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(1[3-9]\\d)\\d{4}(\\d{4})(?!\\d)");
    // API Key 类正则：匹配 api-key、token、secret、password 这类键名后面的敏感值。
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(?i)(api[-_]?key|token|secret|password)(\\s*[=:]\\s*)([^\\s,;]+)");

    // 私有构造方法表示这个类不应该被 new 出对象，只通过静态方法使用。
    private SensitiveDataMasker() {
    }

    // 对一整段文本进行脱敏，适合日志打印前统一调用。
    public static String mask(String text) {
        // 如果传入 null 或空字符串，直接返回原值，避免后续正则处理出现无意义开销。
        if (text == null || text.isEmpty()) {
            // 返回原始值；null 进来仍然是 null，空字符串进来仍然是空字符串。
            return text;
        }

        // 第一步：把文本中的身份证号替换为“前 6 位 + ******** + 后 4 位”。
        String masked = ID_CARD_PATTERN.matcher(text).replaceAll("$1********$2");
        // 第二步：把文本中的手机号替换为“前 3 位 + **** + 后 4 位”。
        masked = PHONE_PATTERN.matcher(masked).replaceAll("$1****$2");
        // 第三步：继续处理 api-key/token/password 这类键值格式的敏感信息。
        return maskApiKeys(masked);
    }

    // 单独脱敏身份证号，适合已经明确知道某个字段就是身份证号的场景。
    public static String maskIdCard(String idCard) {
        // 如果身份证号为空或长度太短，就不尝试截取，直接返回通用掩码。
        if (idCard == null || idCard.length() < 10) {
            // 返回四个星号，表示这里有敏感字段但原始值不可用或格式异常。
            return "****";
        }
        // 保留前 6 位和最后 4 位，中间统一替换成 8 个星号。
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    // 单独脱敏手机号，适合已经明确知道某个字段就是手机号的场景。
    public static String maskPhone(String phone) {
        // 如果手机号为空或长度太短，就不尝试截取，避免 substring 越界。
        if (phone == null || phone.length() < 7) {
            // 返回四个星号，表示这里有敏感字段但原始值不可用或格式异常。
            return "****";
        }
        // 保留前 3 位和最后 4 位，中间统一替换成 4 个星号。
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    // 脱敏 api-key/token/password 这类“键=值”或“键:值”格式的文本。
    private static String maskApiKeys(String text) {
        // 用 API_KEY_PATTERN 在文本中查找所有符合规则的敏感键值片段。
        Matcher matcher = API_KEY_PATTERN.matcher(text);
        // StringBuffer 是 Matcher.appendReplacement API 要求使用的可变字符串容器。
        StringBuffer result = new StringBuffer();
        // while 循环逐个处理匹配到的敏感键值片段。
        while (matcher.find()) {
            // group(1) 是键名，例如 api-key、token、password。
            // group(2) 是分隔符和空白，例如 "="、": "。
            // 敏感值本身不保留，统一替换为 ******。
            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + matcher.group(2) + "******"));
        }
        // 把最后一次匹配之后剩余的普通文本追加到 result 中。
        matcher.appendTail(result);
        // 返回完成 API Key 类字段脱敏后的文本。
        return result.toString();
    }
}
