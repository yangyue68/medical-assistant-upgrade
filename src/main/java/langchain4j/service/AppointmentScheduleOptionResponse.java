// 声明当前类所在的包，service 表示它属于预约业务响应模型。
package langchain4j.service;

// DoctorSchedule 是数据库中的医生排班实体。
import langchain4j.entity.DoctorSchedule;
// LocalDate 表示预约日期。
import java.time.LocalDate;

// 定义前端预约表单下拉选项响应对象。
public class AppointmentScheduleOptionResponse {

    // id 表示排班记录 ID。
    private final Long id;

    // department 表示科室名称。
    private final String department;

    // doctorName 表示医生姓名。
    private final String doctorName;

    // title 表示医生职称。
    private final String title;

    // date 表示可预约日期。
    private final LocalDate date;

    // timeSlot 表示可预约时间段。
    private final String timeSlot;

    // remainingSlots 表示当前剩余号源数。
    private final int remainingSlots;

    // 构造预约排班选项响应对象。
    public AppointmentScheduleOptionResponse(
            // 排班记录 ID。
            Long id,
            // 科室名称。
            String department,
            // 医生姓名。
            String doctorName,
            // 医生职称。
            String title,
            // 可预约日期。
            LocalDate date,
            // 可预约时间段。
            String timeSlot,
            // 剩余号源数。
            int remainingSlots
    ) {
        // 保存排班记录 ID。
        this.id = id;
        // 保存科室名称。
        this.department = department;
        // 保存医生姓名。
        this.doctorName = doctorName;
        // 保存医生职称。
        this.title = title;
        // 保存可预约日期。
        this.date = date;
        // 保存可预约时间段。
        this.timeSlot = timeSlot;
        // 保存剩余号源数。
        this.remainingSlots = remainingSlots;
    }

    // 从数据库排班实体创建前端选项响应。
    public static AppointmentScheduleOptionResponse from(DoctorSchedule schedule) {
        // 计算剩余号源。
        int remainingSlots = Math.max(0, schedule.getMaxAppointments() - schedule.getBookedCount());
        // 返回前端选项响应对象。
        return new AppointmentScheduleOptionResponse(
                // 设置排班 ID。
                schedule.getId(),
                // 设置科室。
                schedule.getDepartment(),
                // 设置医生姓名。
                schedule.getDoctorName(),
                // 设置医生职称。
                schedule.getTitle(),
                // 设置日期。
                schedule.getDate(),
                // 设置时间段。
                schedule.getTimeSlot(),
                // 设置剩余号源。
                remainingSlots
        );
    }

    // 返回排班记录 ID。
    public Long getId() {
        // 返回 id 字段。
        return id;
    }

    // 返回科室名称。
    public String getDepartment() {
        // 返回 department 字段。
        return department;
    }

    // 返回医生姓名。
    public String getDoctorName() {
        // 返回 doctorName 字段。
        return doctorName;
    }

    // 返回医生职称。
    public String getTitle() {
        // 返回 title 字段。
        return title;
    }

    // 返回可预约日期。
    public LocalDate getDate() {
        // 返回 date 字段。
        return date;
    }

    // 返回可预约时间段。
    public String getTimeSlot() {
        // 返回 timeSlot 字段。
        return timeSlot;
    }

    // 返回剩余号源数。
    public int getRemainingSlots() {
        // 返回 remainingSlots 字段。
        return remainingSlots;
    }
}
