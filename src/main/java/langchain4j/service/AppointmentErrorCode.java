// 声明当前枚举所在的包，service 表示它服务于预约业务层。
package langchain4j.service;

// 定义预约业务错误码枚举，让失败原因从“散落的字符串”升级为“可判断的业务状态”。
public enum AppointmentErrorCode {

    // 表示预约成功，没有业务错误。
    SUCCESS,

    // 表示用户提交的预约参数不完整或格式不正确。
    INVALID_REQUEST,

    // 表示指定医生、日期、时间段没有可预约排班。
    NO_AVAILABLE_SCHEDULE,

    // 表示用户已经在同一天同一时间段预约过同科室，不能重复预约。
    DUPLICATE_APPOINTMENT,

    // 表示号源在并发预约过程中被抢完。
    SLOT_SOLD_OUT,

    // 表示取消预约时没有找到可取消的有效预约。
    APPOINTMENT_NOT_FOUND,

    // 表示取消预约时身份证号和预约记录不匹配。
    ID_CARD_MISMATCH,

    // 表示预约已经取消或完成，不能重复取消。
    APPOINTMENT_NOT_ACTIVE
}
