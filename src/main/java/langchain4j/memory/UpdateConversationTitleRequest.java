// 声明当前类所在的包，memory 表示它属于会话管理模块。
package langchain4j.memory;

// 定义更新会话标题请求体。
public class UpdateConversationTitleRequest {

    // title 表示前端希望保存到会话列表里的标题。
    private String title;

    // 返回请求中的标题。
    public String getTitle() {
        // 返回 title 字段。
        return title;
    }

    // 设置请求中的标题。
    public void setTitle(String title) {
        // 保存 title 字段。
        this.title = title;
    }
}
