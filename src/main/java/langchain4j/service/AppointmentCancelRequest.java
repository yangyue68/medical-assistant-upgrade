// 声明当前类所在的包，service 表示它服务于预约业务接口。
package langchain4j.service;

// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义前端取消预约时提交的请求体。
public class AppointmentCancelRequest {

    // 患者身份证号，用来确认用户有权取消该预约。
    private String idCard;
}
