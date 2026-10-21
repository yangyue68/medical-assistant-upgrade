package langchain4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import langchain4j.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    /**
     * 查询患者的所有有效预约
     */
    @Select("SELECT * FROM appointment " +
            "WHERE id_card = #{idCard} " +
            "AND status = 0 " +
            "ORDER BY date ASC, time_slot ASC")
    List<Appointment> findValidAppointmentsByIdCard(@Param("idCard") String idCard);

    /**
     * 检查是否存在重复预约
     */
    @Select("SELECT COUNT(*) FROM appointment " +
            "WHERE id_card = #{idCard} " +
            "AND department = #{department} " +
            "AND date = #{date} " +
            "AND time_slot = #{timeSlot} " +
            "AND status = 0")
    int countDuplicateAppointment(@Param("idCard") String idCard,
                                  @Param("department") String department,
                                  @Param("date") LocalDate date,
                                  @Param("timeSlot") String timeSlot);

    @Select("SELECT COUNT(*) FROM appointment WHERE user_id = #{userId} AND id_card = #{idCard} " +
            "AND department = #{department} AND date = #{date} AND time_slot = #{timeSlot} AND status = 0")
    int countDuplicateAppointmentForUser(@Param("userId") Long userId, @Param("idCard") String idCard,
                                         @Param("department") String department, @Param("date") LocalDate date,
                                         @Param("timeSlot") String timeSlot);

    /**
     * 取消预约
     */
    @Update("UPDATE appointment SET status = 1, cancel_time = NOW() " +
            "WHERE id = #{id} AND status = 0")
    int cancelAppointment(@Param("id") Long id);
}
