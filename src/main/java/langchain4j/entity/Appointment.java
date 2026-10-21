package langchain4j.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("appointment")
public class Appointment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;

    private String username;
    private String idCard;
    private String phone;           // ✅ 新增：手机号
    private String department;
    private String doctorName;
    private LocalDate date;
    private String timeSlot;
    private Long scheduleId;
    private Integer status;          // 状态码见 AppointmentStatus：0-有效，1-已取消，2-已完成

    private LocalDateTime cancelTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
