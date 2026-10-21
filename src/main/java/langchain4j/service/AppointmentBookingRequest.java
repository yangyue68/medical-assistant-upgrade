// 声明当前类所在的包，service 表示它是预约业务层使用的请求对象。
package langchain4j.service;

// LocalDate 表示不带时间的预约日期。
import java.time.LocalDate;
// ArrayList 用来收集多个参数校验错误。
import java.util.ArrayList;
// List 表示一组校验错误消息。
import java.util.List;
// Pattern 用来编译手机号、身份证号等格式正则。
import java.util.regex.Pattern;
// AllArgsConstructor 会让 Lombok 生成全参构造方法。
import lombok.AllArgsConstructor;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;
// NoArgsConstructor 会让 Lombok 生成无参构造方法。
import lombok.NoArgsConstructor;

// 让 Lombok 自动生成 getter、setter 等样板代码。
@Data
// 生成无参构造方法，方便测试和未来 JSON 反序列化。
@NoArgsConstructor
// 生成全参构造方法，方便业务代码一次性创建请求。
@AllArgsConstructor
// 定义预约挂号请求对象，把原来散落的多个参数收束成一个业务对象。
public class AppointmentBookingRequest {

    // 定义中国大陆手机号的基础格式：1 开头，第二位 3-9，总共 11 位。
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    // 定义身份证号的基础格式：18 位，最后一位可以是数字或 X/x。
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[0-9Xx]$");

    // 患者姓名。
    private String username;

    // 患者身份证号。
    private String idCard;

    // 患者手机号。
    private String phone;

    // 预约科室名称。
    private String department;

    // 预约医生姓名。
    private String doctorName;

    // 预约日期。
    private LocalDate date;

    // 预约时间段，目前支持“上午”和“下午”。
    private String timeSlot;

    // 校验当前预约请求，并返回所有发现的问题。
    public List<String> validate() {
        // 创建错误列表，用来一次性收集所有参数问题。
        List<String> errors = new ArrayList<>();
        // 校验患者姓名不能为空。
        requireText(errors, username, "请提供患者姓名。");
        // 校验身份证号不能为空。
        requireText(errors, idCard, "请提供患者身份证号。");
        // 校验手机号不能为空。
        requireText(errors, phone, "请提供患者手机号。");
        // 校验科室不能为空。
        requireText(errors, department, "请提供预约科室。");
        // 校验医生姓名不能为空。
        requireText(errors, doctorName, "请提供预约医生姓名。");
        // 校验预约日期不能为空。
        if (date == null) {
            // 记录日期缺失错误。
            errors.add("请提供预约日期。");
        } else if (date.isBefore(LocalDate.now())) {
            // 记录日期不能早于今天的错误。
            errors.add("预约日期不能早于今天。");
        }
        // 如果身份证号有文本，再进一步校验格式。
        if (hasText(idCard) && !ID_CARD_PATTERN.matcher(idCard).matches()) {
            // 记录身份证号格式错误。
            errors.add("身份证号格式不正确，请提供18位身份证号。");
        }
        // 如果手机号有文本，再进一步校验格式。
        if (hasText(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            // 记录手机号格式错误。
            errors.add("手机号格式不正确，请提供11位大陆手机号。");
        }
        // 校验时间段是否为当前系统支持的值。
        if (!"上午".equals(timeSlot) && !"下午".equals(timeSlot)) {
            // 记录时间段错误。
            errors.add("预约时间段只能是上午或下午。");
        }
        // 返回完整错误列表；空列表表示校验通过。
        return errors;
    }

    // 判断当前预约请求是否通过校验。
    public boolean isValid() {
        // 如果校验错误列表为空，就表示请求有效。
        return validate().isEmpty();
    }

    // 校验某个必填字符串是否为空。
    private static void requireText(List<String> errors, String value, String errorMessage) {
        // 如果字符串没有有效文本，就加入对应错误消息。
        if (!hasText(value)) {
            // 把错误消息放入错误列表。
            errors.add(errorMessage);
        }
    }

    // 判断字符串是否包含非空白字符。
    private static boolean hasText(String value) {
        // 先判断不是 null，再去掉首尾空白后判断是否为空。
        return value != null && !value.trim().isEmpty();
    }
}
