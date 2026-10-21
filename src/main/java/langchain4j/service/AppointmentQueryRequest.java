// 声明当前类所在的包，service 表示它服务于预约业务接口。
package langchain4j.service;

// ArrayList 用来收集多个参数校验错误。
import java.util.ArrayList;
// List 表示一组校验错误。
import java.util.List;
// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义前端查询本人预约时提交的请求体。
public class AppointmentQueryRequest {

    // 患者姓名，用来和身份证号一起确认查询范围。
    private String username;

    // 患者身份证号，用来定位本人有效预约。
    private String idCard;

    // 校验查询请求。
    public List<String> validate() {
        // 创建错误列表。
        List<String> errors = new ArrayList<>();
        // 患者姓名不能为空。
        if (username == null || username.trim().isEmpty()) {
            // 添加姓名缺失提示。
            errors.add("患者姓名不能为空。");
        }
        // 身份证号不能为空。
        if (idCard == null || idCard.trim().isEmpty()) {
            // 添加身份证号缺失提示。
            errors.add("身份证号不能为空。");
        }
        // 返回错误列表。
        return errors;
    }
}
