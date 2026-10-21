package langchain4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import langchain4j.entity.DoctorSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DoctorScheduleMapper extends BaseMapper<DoctorSchedule> {
    
    /**
     * 查询可预约的排班列表
     */
    @Select("SELECT * FROM doctor_schedule " +
            "WHERE department = #{department} " +
            "AND date = #{date} " +
            "AND time_slot = #{timeSlot} " +
            "AND status = 1 " +
            "AND booked_count < max_appointments " +
            "ORDER BY FIELD(title, '主任医师', '副主任医师', '主治医师', '住院医师')")
    List<DoctorSchedule> findAvailableSchedules(@Param("department") String department,
                                                  @Param("date") LocalDate date,
                                                  @Param("timeSlot") String timeSlot);
    
    /**
     * 增加已预约人数
     */
    @Update("UPDATE doctor_schedule SET booked_count = booked_count + 1 " +
            "WHERE id = #{scheduleId} AND booked_count < max_appointments")
    int incrementBookedCount(@Param("scheduleId") Long scheduleId);
    
    /**
     * 减少已预约人数（取消预约时）
     */
    @Update("UPDATE doctor_schedule SET booked_count = booked_count - 1 " +
            "WHERE id = #{scheduleId} AND booked_count > 0")
    int decrementBookedCount(@Param("scheduleId") Long scheduleId);
}