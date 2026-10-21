package langchain4j.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import langchain4j.entity.DoctorSchedule;
import langchain4j.mapper.DoctorScheduleMapper;
import langchain4j.service.DoctorScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 医生排班服务实现类
 * 
 * @author yangyue
 * @date 2026/4/21
 */
@Service
@Transactional
public class DoctorScheduleServiceImpl extends ServiceImpl<DoctorScheduleMapper, DoctorSchedule> 
        implements DoctorScheduleService {
    
    @Override
    public List<DoctorSchedule> findAvailableSchedules(String department, LocalDate date, String timeSlot) {
        return baseMapper.findAvailableSchedules(department, date, timeSlot);
    }

    // 查询一段日期范围内的可预约排班。
    @Override
    public List<DoctorSchedule> findAvailableSchedulesBetween(
            // 查询开始日期。
            LocalDate startDate,
            // 查询结束日期。
            LocalDate endDate,
            // 可选科室过滤。
            String department,
            // 可选时间段过滤。
            String timeSlot
    ) {
        // 创建 MyBatis-Plus 查询条件。
        LambdaQueryWrapper<DoctorSchedule> wrapper = new LambdaQueryWrapper<>();
        // 只查询正常出诊的排班。
        wrapper.eq(DoctorSchedule::getStatus, 1);
        // 只查询还有剩余号源的排班。
        wrapper.apply("booked_count < max_appointments");
        // 如果传入开始日期，就查询不早于开始日期的排班。
        if (startDate != null) {
            // 添加开始日期条件。
            wrapper.ge(DoctorSchedule::getDate, startDate);
        }
        // 如果传入结束日期，就查询不晚于结束日期的排班。
        if (endDate != null) {
            // 添加结束日期条件。
            wrapper.le(DoctorSchedule::getDate, endDate);
        }
        // 如果传入科室，就按科室过滤。
        if (department != null && !department.trim().isEmpty()) {
            // 添加科室条件。
            wrapper.eq(DoctorSchedule::getDepartment, department.trim());
        }
        // 如果传入时间段，就按时间段过滤。
        if (timeSlot != null && !timeSlot.trim().isEmpty()) {
            // 添加时间段条件。
            wrapper.eq(DoctorSchedule::getTimeSlot, timeSlot.trim());
        }
        // 按日期、科室、医生、时间段排序，保证前端下拉稳定。
        wrapper.orderByAsc(DoctorSchedule::getDate, DoctorSchedule::getDepartment, DoctorSchedule::getDoctorName, DoctorSchedule::getTimeSlot);
        // 执行查询并返回结果。
        return this.list(wrapper);
    }
    
    @Override
    public boolean incrementBookedCount(Long scheduleId) {
        return baseMapper.incrementBookedCount(scheduleId) > 0;
    }
    
    @Override
    public boolean decrementBookedCount(Long scheduleId) {
        return baseMapper.decrementBookedCount(scheduleId) > 0;
    }
    
    @Override
    public DoctorSchedule findByDoctorAndDateAndTimeSlot(String doctorName, LocalDate date, String timeSlot) {
        LambdaQueryWrapper<DoctorSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorSchedule::getDoctorName, doctorName)
               .eq(DoctorSchedule::getDate, date)
               .eq(DoctorSchedule::getTimeSlot, timeSlot)
               .eq(DoctorSchedule::getStatus, 1);
        return this.getOne(wrapper);
    }
    
    @Override
    public boolean hasAvailableSlot(String doctorName, LocalDate date, String timeSlot) {
        DoctorSchedule schedule = findByDoctorAndDateAndTimeSlot(doctorName, date, timeSlot);
        if (schedule == null) {
            return false;
        }
        return schedule.getBookedCount() < schedule.getMaxAppointments();
    }
    
    @Override
    public int getRemainingSlots(String doctorName, LocalDate date, String timeSlot) {
        DoctorSchedule schedule = findByDoctorAndDateAndTimeSlot(doctorName, date, timeSlot);
        if (schedule == null) {
            return 0;
        }
        return schedule.getMaxAppointments() - schedule.getBookedCount();
    }
}
