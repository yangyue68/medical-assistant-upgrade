package langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import langchain4j.entity.DoctorSchedule;

import java.time.LocalDate;
import java.util.List;

/**
 * 医生排班服务接口
 *
 * @author yangyue
 * @date 2026/4/21
 */
public interface DoctorScheduleService extends IService<DoctorSchedule> {

    /**
     * 查询可预约的排班
     *
     * @param department 科室名称
     * @param date 预约日期
     * @param timeSlot 时间段（上午/下午）
     * @return 可预约的医生排班列表（按职称排序）
     */
    List<DoctorSchedule> findAvailableSchedules(String department, LocalDate date, String timeSlot);

    /**
     * 查询一段日期范围内的可预约排班
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param department 可选科室过滤
     * @param timeSlot 可选时间段过滤
     * @return 可预约排班列表
     */
    List<DoctorSchedule> findAvailableSchedulesBetween(LocalDate startDate, LocalDate endDate, String department, String timeSlot);

    /**
     * 增加已预约人数（带并发控制）
     *
     * @param scheduleId 排班ID
     * @return 是否增加成功（false表示号源已满或更新失败）
     */
    boolean incrementBookedCount(Long scheduleId);

    /**
     * 减少已预约人数（取消预约时调用）
     *
     * @param scheduleId 排班ID
     * @return 是否减少成功
     */
    boolean decrementBookedCount(Long scheduleId);

    /**
     * 根据医生姓名、日期、时间段查询排班
     *
     * @param doctorName 医生姓名
     * @param date 日期
     * @param timeSlot 时间段
     * @return 排班信息，不存在则返回null
     */
    DoctorSchedule findByDoctorAndDateAndTimeSlot(String doctorName, LocalDate date, String timeSlot);

    /**
     * 检查医生在指定时间段是否有可预约号源
     *
     * @param doctorName 医生姓名
     * @param date 日期
     * @param timeSlot 时间段
     * @return 是否有号源
     */
    boolean hasAvailableSlot(String doctorName, LocalDate date, String timeSlot);

    /**
     * 获取医生在指定时间段的剩余号源数量
     *
     * @param doctorName 医生姓名
     * @param date 日期
     * @param timeSlot 时间段
     * @return 剩余号源数量，如果医生无排班返回0
     */
    int getRemainingSlots(String doctorName, LocalDate date, String timeSlot);
}
