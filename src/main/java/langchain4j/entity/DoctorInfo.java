package langchain4j.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 医生信息实体类
 * 
 * @author yangyue
 * @date 2026/4/21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("doctor_info")
public class DoctorInfo {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String doctorName;      // 医生姓名
    private String department;      // 所属科室
    private String title;           // 职称：主任医师、副主任医师、主治医师、住院医师
    private String specialty;       // 专业特长
    private String avatar;          // 头像URL
    private String introduction;    // 医生简介
    private Integer status;         // 状态：0-离职，1-在职
    private Integer sortOrder;      // 排序
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}