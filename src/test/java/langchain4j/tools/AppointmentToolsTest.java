package langchain4j.tools;

import langchain4j.entity.DoctorSchedule;
import langchain4j.service.DoctorScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentToolsTest {

    // 验证范围号源查询会委托后端排班服务并返回医生排班。
    @Test
    void shouldQueryAvailableSchedulesBetweenDates() {
        // 创建医生排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建被测预约工具。
        AppointmentTools tools = new AppointmentTools();
        // 把 mock 注入到被测工具的私有字段中。
        ReflectionTestUtils.setField(tools, "doctorScheduleService", scheduleService);
        // 把真实日期解析组件注入到被测工具中。
        ReflectionTestUtils.setField(tools, "dateTools", new DateTools());
        // 准备开始日期。
        LocalDate startDate = LocalDate.now().plusDays(1);
        // 准备结束日期。
        LocalDate endDate = LocalDate.now().plusDays(3);
        // 指定排班服务返回一条眼科排班。
        when(scheduleService.findAvailableSchedulesBetween(eq(startDate), eq(endDate), eq("眼科"), isNull()))
                .thenReturn(List.of(schedule(startDate)));

        // 调用新增的范围号源工具。
        String result = tools.queryAvailableSchedulesBetween("眼科", startDate.toString(), endDate.toString(), null);

        // 断言结果包含科室。
        assertThat(result).contains("眼科");
        // 断言结果包含医生。
        assertThat(result).contains("王医生");
        // 断言结果包含剩余号源。
        assertThat(result).contains("剩余 17 个号源");
        // 验证工具调用了后端范围查询服务。
        verify(scheduleService).findAvailableSchedulesBetween(startDate, endDate, "眼科", null);
    }

    // 验证范围号源查询会拒绝非法日期格式。
    @Test
    void shouldRejectInvalidRangeDate() {
        // 创建被测预约工具。
        AppointmentTools tools = new AppointmentTools();

        // 调用新增工具并传入非法开始日期。
        String result = tools.queryAvailableSchedulesBetween("眼科", "2026/07/29", "2026-07-31", null);

        // 断言返回清晰的日期格式错误。
        assertThat(result).contains("开始日期格式不正确");
    }

    // 验证相对日期范围查询会先解析日期，再委托真实排班服务。
    @Test
    void shouldQueryAvailableSchedulesByRelativeDate() {
        // 创建医生排班服务 mock。
        DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
        // 创建被测预约工具。
        AppointmentTools tools = new AppointmentTools();
        // 注入医生排班服务 mock。
        ReflectionTestUtils.setField(tools, "doctorScheduleService", scheduleService);
        // 注入真实日期解析组件。
        ReflectionTestUtils.setField(tools, "dateTools", new DateTools());
        // 计算明天日期。
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        // 指定排班服务返回一条神经内科排班。
        when(scheduleService.findAvailableSchedulesBetween(eq(tomorrow), eq(tomorrow), eq("神经内科"), isNull()))
                .thenReturn(List.of(schedule(tomorrow)));

        // 调用相对日期范围查询工具。
        String result = tools.queryAvailableSchedulesByRelativeDate("神经内科", "明天", null);

        // 断言返回包含科室。
        assertThat(result).contains("神经内科");
        // 断言返回包含医生。
        assertThat(result).contains("王医生");
        // 验证最终查询使用了 Java 解析出的明确日期。
        verify(scheduleService).findAvailableSchedulesBetween(tomorrow, tomorrow, "神经内科", null);
    }

    // 构造测试排班实体。
    private static DoctorSchedule schedule(LocalDate date) {
        // 创建排班对象。
        DoctorSchedule schedule = new DoctorSchedule();
        // 设置医生姓名。
        schedule.setDoctorName("王医生");
        // 设置科室。
        schedule.setDepartment("眼科");
        // 设置职称。
        schedule.setTitle("主任医师");
        // 设置排班日期。
        schedule.setDate(date);
        // 设置时间段。
        schedule.setTimeSlot("上午");
        // 设置最大号源数。
        schedule.setMaxAppointments(20);
        // 设置已预约人数。
        schedule.setBookedCount(3);
        // 返回排班对象。
        return schedule;
    }
}
