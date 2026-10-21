// 声明当前类所在的包，memory 表示它属于对话记忆和会话管理模块。
package langchain4j.memory;

// Data 会让 Lombok 生成 getter、setter、toString、equals、hashCode 等方法。
import lombok.Data;

// 让 Lombok 自动生成常用样板代码。
@Data
// 定义创建会话请求。
public class CreateConversationRequest {

    // 前端传入的会话标题；为空时后端会使用默认标题。
    private String title;
}
