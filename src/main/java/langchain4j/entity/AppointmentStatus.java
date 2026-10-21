// 声明当前枚举所在的包，entity 表示它描述预约实体自身的领域状态。
package langchain4j.entity;

// 定义预约状态枚举，用具名状态替代 0/1/2 这类魔法数字。
public enum AppointmentStatus {

    // 表示预约有效，用户可以按时就诊，也可以取消。
    ACTIVE(0),

    // 表示预约已被用户取消。
    CANCELLED(1),

    // 表示预约已完成。
    COMPLETED(2);

    // 保存数据库中实际存储的状态码。
    private final int code;

    // 构造枚举值时绑定数据库状态码。
    AppointmentStatus(
            // 数据库存储的整数状态码。
            int code
    ) {
        // 保存状态码到成员变量。
        this.code = code;
    }

    // 返回数据库中使用的整数状态码。
    public int code() {
        // 返回当前枚举值绑定的状态码。
        return code;
    }

    // 判断某个数据库状态码是否等于当前枚举值。
    public boolean matches(Integer value) {
        // 如果 value 不为空且等于当前枚举状态码，就返回 true。
        return value != null && value == code;
    }
}
