package langchain4j.tools;

import langchain4j.entity.Appointment;
import langchain4j.entity.DoctorInfo;
import langchain4j.entity.DoctorSchedule;
import langchain4j.service.AppointmentCancelResult;
import langchain4j.service.AppointmentService;
import langchain4j.service.AppointmentResult;
import langchain4j.service.DoctorInfoService;
import langchain4j.service.DoctorScheduleService;
import langchain4j.util.SensitiveDataMasker;
import langchain4j.auth.AccountContext;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class AppointmentTools {

    private static final Logger log = LoggerFactory.getLogger(AppointmentTools.class);

    @Autowired
    private DoctorScheduleService doctorScheduleService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorInfoService doctorInfoService;  // ✅ 新增

    // dateTools 用来把预约场景中的相对日期范围解析成明确起止日期。
    @Autowired
    private DateTools dateTools;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 范围号源查询最多允许查 14 天，避免 Agent 一次工具调用扫太大的排班范围。
    private static final int MAX_SCHEDULE_RANGE_DAYS = 14;

    // ==================== 0. 查询科室列表 ====================

    @Tool(name = "getDepartments",
            value = "查询医院所有可预约的科室列表。" +
                    "当用户询问有哪些科室、可以挂什么科时，调用此工具。")
    public String getDepartments() {
        List<String> departments = doctorInfoService.getAllDepartments();

        if (departments == null || departments.isEmpty()) {
            return "暂无科室信息。";
        }

        StringBuilder result = new StringBuilder();
        result.append("🏥 本院有以下科室：\n\n");
        for (int i = 0; i < departments.size(); i++) {
            result.append(i + 1).append(". ").append(departments.get(i)).append("\n");
        }
        result.append("\n请问您想预约哪个科室？");
        return result.toString();
    }

    // ==================== 0.2 查询科室医生列表 ====================

    @Tool(name = "getDoctorsByDepartment",
            value = "查询指定科室的所有在职医生列表。" +
                    "当用户想知道某个科室有哪些医生时，调用此工具。" +
                    "返回医生的姓名、职称、擅长领域等信息。")
    public String getDoctorsByDepartment(
            @P(value = "科室名称", required = true) String department
    ) {
        List<DoctorInfo> doctors = doctorInfoService.getDoctorsByDepartmentWithTitleOrder(department);

        if (doctors == null || doctors.isEmpty()) {
            return "❌ 抱歉，" + department + "暂无在职医生信息。";
        }

        StringBuilder result = new StringBuilder();
        result.append("🏥 ").append(department).append("的医生团队：\n\n");

        for (int i = 0; i < doctors.size(); i++) {
            DoctorInfo doctor = doctors.get(i);
            result.append(i + 1).append(". ");
            result.append(doctor.getDoctorName()).append("（").append(doctor.getTitle()).append("）\n");
            if (doctor.getSpecialty() != null && !doctor.getSpecialty().isEmpty()) {
                result.append("   擅长：").append(doctor.getSpecialty()).append("\n");
            }
        }

        result.append("\n请选择您想预约的医生。");
        return result.toString();
    }

    // ==================== 1. 查询号源（增强版，包含医生信息） ====================

    @Tool(name = "queryAvailableSlots",
            value = "根据科室名称、日期、时间查询是否有可预约的号源。" +
                    "如果用户没有提供具体日期（如说'明天'、'后天'），请先计算出具体日期后再调用。" +
                    "日期格式必须为YYYY-MM-DD，例如：2026-04-22。" +
                    "返回该科室在指定时间段的可预约医生列表、剩余号源数量和医生简介。")
    public String queryAvailableSlots(
            @P(value = "科室名称", required = true) String department,
            @P(value = "预约日期，格式：YYYY-MM-DD", required = true) String dateStr,
            @P(value = "预约时间，可选值：上午、下午", required = true) String timeSlot
    ) {
        log.info("Query available slots. department={}, date={}, timeSlot={}", department, dateStr, timeSlot);

        // 参数校验
        if (department == null || department.trim().isEmpty()) {
            return "❌ 请提供科室名称。";
        }
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return "❌ 请提供预约日期。";
        }
        if (timeSlot == null || (!timeSlot.equals("上午") && !timeSlot.equals("下午"))) {
            return "❌ 请提供正确的时间段（上午或下午）。";
        }

        // 解析日期
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return "❌ 日期格式不正确，请使用 YYYY-MM-DD 格式，例如：2026-04-22。";
        }

        // 查询数据库
        List<DoctorSchedule> schedules = doctorScheduleService.findAvailableSchedules(
                department, date, timeSlot);

        if (schedules == null || schedules.isEmpty()) {
            return "❌ 抱歉，" + department + "在" + dateStr + timeSlot + "没有可预约的号源。" +
                    "可能原因：1) 该时段没有医生排班；2) 所有号源已约满。请选择其他日期或时间段。";
        }

        // 构建返回结果
        StringBuilder result = new StringBuilder();
        result.append("✅ ").append(department).append("在").append(dateStr).append(timeSlot)
                .append("有以下医生可预约：\n\n");

        for (DoctorSchedule schedule : schedules) {
            int remaining = schedule.getMaxAppointments() - schedule.getBookedCount();

            // 获取医生详细信息
            DoctorInfo doctorInfo = doctorInfoService.getDoctorByName(schedule.getDoctorName());

            result.append("  • ").append(schedule.getDoctorName())
                    .append("（").append(schedule.getTitle()).append("）")
                    .append("：剩余 ").append(remaining).append(" 个号源\n");

            if (doctorInfo != null && doctorInfo.getSpecialty() != null) {
                result.append("    擅长：").append(doctorInfo.getSpecialty()).append("\n");
            }
        }

        result.append("\n请告诉我要预约哪位医生，并提供患者信息（姓名、身份证号、手机号）来完成预约。");
        return result.toString();
    }

    // ==================== 1.2 按日期范围查询号源 ====================

    @Tool(name = "queryAvailableSchedulesBetween",
            value = "根据科室、开始日期、结束日期查询一段时间内的可预约医生排班。" +
                    "适用于用户询问'未来几天某科有哪些医生出诊'、'本周某科号源'这类范围查询。" +
                    "startDate 和 endDate 必须是 YYYY-MM-DD；timeSlot 只能为空、上午或下午。")
    public String queryAvailableSchedulesBetween(
            @P(value = "科室名称", required = true) String department,
            @P(value = "开始日期，格式：YYYY-MM-DD", required = true) String startDateStr,
            @P(value = "结束日期，格式：YYYY-MM-DD", required = true) String endDateStr,
            @P(value = "预约时间，可选值：上午、下午；不确定时可以为空", required = false) String timeSlot
    ) {
        // 记录范围号源查询参数，方便排查 Agent 是否传入了错误日期。
        log.info("Query available schedules between. department={}, startDate={}, endDate={}, timeSlot={}",
                department, startDateStr, endDateStr, timeSlot);
        // 校验科室名称，避免空科室扫全库。
        if (department == null || department.trim().isEmpty()) {
            // 返回可读错误，告诉模型需要补齐科室。
            return "❌ 请提供科室名称。";
        }
        // 解析开始日期，失败时直接返回格式提示。
        LocalDate startDate = parseToolDate(startDateStr, "开始日期");
        // 如果开始日期解析失败，就结束本次工具调用。
        if (startDate == null) {
            // 返回统一日期格式提示。
            return "❌ 开始日期格式不正确，请使用 YYYY-MM-DD 格式，例如：2026-07-29。";
        }
        // 解析结束日期，失败时直接返回格式提示。
        LocalDate endDate = parseToolDate(endDateStr, "结束日期");
        // 如果结束日期解析失败，就结束本次工具调用。
        if (endDate == null) {
            // 返回统一日期格式提示。
            return "❌ 结束日期格式不正确，请使用 YYYY-MM-DD 格式，例如：2026-07-31。";
        }
        // 不允许结束日期早于开始日期。
        if (endDate.isBefore(startDate)) {
            // 返回日期范围错误提示。
            return "❌ 结束日期不能早于开始日期。";
        }
        // 不允许查询过去日期，避免预约已经过期的排班。
        if (startDate.isBefore(LocalDate.now())) {
            // 返回过去日期错误提示。
            return "❌ 不能查询过去日期的预约号源。";
        }
        // 计算本次查询覆盖的自然日数量。
        long queryDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        // 限制范围查询最大天数，防止 Agent 误传过大范围。
        if (queryDays > MAX_SCHEDULE_RANGE_DAYS) {
            // 返回范围过大提示，引导用户缩小时间范围。
            return "❌ 一次最多查询未来 " + MAX_SCHEDULE_RANGE_DAYS + " 天号源，请缩小日期范围。";
        }
        // 规整时间段参数，空字符串视为不限制上午/下午。
        String normalizedTimeSlot = normalizeTimeSlot(timeSlot);
        // 如果用户传入了非法时间段，就直接返回提示。
        if ("INVALID".equals(normalizedTimeSlot)) {
            // 返回时间段错误提示。
            return "❌ 时间段只能是上午或下午；不确定时可以不填。";
        }
        // 调用后端排班服务查询真实可预约号源。
        List<DoctorSchedule> schedules = doctorScheduleService.findAvailableSchedulesBetween(
                startDate, endDate, department.trim(), normalizedTimeSlot);
        // 如果没有查询到排班，就告诉用户无可预约号源。
        if (schedules == null || schedules.isEmpty()) {
            // 返回无号源提示。
            return "❌ 抱歉，" + department.trim() + "在 " + startDate + " 至 " + endDate + " 暂无可预约号源。";
        }
        // 对结果排序，确保 Agent 和前端看到的顺序稳定。
        List<DoctorSchedule> sortedSchedules = schedules.stream()
                .sorted(Comparator.comparing(DoctorSchedule::getDate)
                        .thenComparing(DoctorSchedule::getTimeSlot)
                        .thenComparing(DoctorSchedule::getDoctorName))
                .toList();
        // 创建面向用户的结果文本。
        StringBuilder result = new StringBuilder();
        // 写入结果标题。
        result.append("✅ ").append(department.trim()).append("在 ")
                .append(startDate).append(" 至 ").append(endDate)
                .append(" 有以下可预约排班：\n\n");
        // 控制最多展示 20 条，防止工具响应过长挤占模型上下文。
        int displayLimit = Math.min(sortedSchedules.size(), 20);
        // 遍历要展示的排班。
        for (int i = 0; i < displayLimit; i++) {
            // 取出当前排班。
            DoctorSchedule schedule = sortedSchedules.get(i);
            // 计算剩余号源数量。
            int remaining = schedule.getMaxAppointments() - schedule.getBookedCount();
            // 拼接一条排班摘要。
            result.append(i + 1).append(". ")
                    .append(schedule.getDate()).append(" ")
                    .append(schedule.getTimeSlot()).append(" | ")
                    .append(schedule.getDoctorName()).append("（")
                    .append(schedule.getTitle()).append("） | 剩余 ")
                    .append(remaining).append(" 个号源\n");
        }
        // 如果总结果超过展示上限，就提醒用户已截断。
        if (sortedSchedules.size() > displayLimit) {
            // 写入截断说明。
            result.append("\n仅展示前 ").append(displayLimit).append(" 条，请缩小日期或时间段查看更多。");
        }
        // 返回最终工具结果。
        return result.toString();
    }

    // ==================== 1.3 按相对日期范围查询号源 ====================

    @Tool(name = "queryAvailableSchedulesByRelativeDate",
            value = "根据科室和中文相对日期范围查询可预约排班。" +
                    "适用于'下周一之前某科有没有号源'、'本周某科有什么号源'、'未来几天某科有哪些医生出诊'。" +
                    "不要自己计算下周一、本周日等星期边界，直接把用户原始相对日期表达传入 relativeDateExpression。")
    public String queryAvailableSchedulesByRelativeDate(
            @P(value = "科室名称", required = true) String department,
            @P(value = "用户原始相对日期表达，如：下周一之前、本周、未来3天", required = true) String relativeDateExpression,
            @P(value = "预约时间，可选值：上午、下午；不确定时可以为空", required = false) String timeSlot
    ) {
        // 记录相对日期范围查号源参数，方便排查模型传入的原始表达。
        log.info("Query available schedules by relative date. department={}, relativeDateExpression={}, timeSlot={}",
                department, relativeDateExpression, timeSlot);
        // 校验科室，避免空科室扫全库。
        if (department == null || department.trim().isEmpty()) {
            // 返回可读错误，提醒模型补齐科室。
            return "❌ 请提供科室名称。";
        }
        // 校验相对日期表达，避免无法确定查询范围。
        if (relativeDateExpression == null || relativeDateExpression.trim().isEmpty()) {
            // 返回可读错误，提醒模型补齐日期范围。
            return "❌ 请提供要查询的日期范围，例如：下周一之前、本周、未来3天。";
        }
        // 调用普通 Java 日期解析组件计算起止日期。
        DateTools.DateRange dateRange = dateTools.resolveAppointmentDateRange(relativeDateExpression, LocalDate.now());
        // 无法解析日期范围时要求用户澄清。
        if (dateRange == null) {
            // 返回澄清提示，避免模型继续编造日期。
            return "❌ 我无法准确理解这个日期范围，请换成明确日期，例如 2026-08-03 之前，或直接选择开始和结束日期。";
        }
        // 复用已经完成校验和数据库查询的范围查号源工具。
        return queryAvailableSchedulesBetween(
                // 传入科室。
                department,
                // 传入解析后的开始日期。
                dateRange.startDate().toString(),
                // 传入解析后的结束日期。
                dateRange.endDate().toString(),
                // 传入可选时间段。
                timeSlot
        );
    }

    // ==================== 2. 预约挂号 ====================

    @Tool(name = "bookAppointment",
            value = "执行预约挂号操作。" +
                    "调用此工具前，请确保：1) 已经查询过号源并确认有号；2) 用户已确认所有预约信息。" +
                    "患者信息必须包含：姓名、身份证号、手机号。" +
                    "如果用户没有提供手机号，请主动询问。" +
                    "身份证号必须是18位（最后一位可能是数字或X），手机号必须是11位数字。")
    public String bookAppointment(
            @P(value = "患者姓名", required = true) String username,
            @P(value = "患者身份证号，18位", required = true) String idCard,
            @P(value = "患者手机号，11位", required = true) String phone,
            @P(value = "预约科室名称", required = true) String department,
            @P(value = "预约医生姓名", required = true) String doctorName,
            @P(value = "预约日期，格式：YYYY-MM-DD", required = true) String dateStr,
            @P(value = "预约时间，可选值：上午、下午", required = true) String timeSlot
    ) {
        log.info(
                "Book appointment. username={}, idCard={}, phone={}, department={}, doctorName={}, date={}, timeSlot={}",
                username,
                SensitiveDataMasker.maskIdCard(idCard),
                SensitiveDataMasker.maskPhone(phone),
                department,
                doctorName,
                dateStr,
                timeSlot
        );

        // 参数校验
        String validationError = validateParams(username, idCard, phone, department, doctorName, dateStr, timeSlot);
        if (validationError != null) {
            return validationError;
        }

        // Do not let private worker memory silently provide patient identity data.
        // Authenticated chat requests must contain all identity fields in this turn.
        if (!identityWasProvidedInCurrentRequest(username, idCard, phone)) {
            return "请先提供本次预约患者的姓名、18位身份证号和11位手机号。我不会使用历史会话中的个人信息代替您本轮提供的资料。";
        }

        // 解析日期
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return "❌ 日期格式不正确，请使用 YYYY-MM-DD 格式。";
        }

        // 执行预约
        AppointmentResult result = AccountContext.currentUserId() == null
                ? appointmentService.bookAppointment(username, idCard, phone, department, doctorName, date, timeSlot)
                : AccountContext.withUser(AccountContext.currentUserId(), () ->
                appointmentService.bookAppointment(username, idCard, phone, department, doctorName, date, timeSlot));

        if (result.isSuccess()) {
            Appointment a = result.getAppointment();
            // 获取医生信息用于更友好的回复
            DoctorInfo doctorInfo = doctorInfoService.getDoctorByName(doctorName);
            String doctorTitle = (doctorInfo != null) ? doctorInfo.getTitle() : "医生";

            return "✅ 预约成功！\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "患者姓名：" + a.getUsername() + "\n" +
                    "身份证号：" + maskIdCard(a.getIdCard()) + "\n" +
                    "手机号：" + maskPhone(a.getPhone()) + "\n" +
                    "预约科室：" + a.getDepartment() + "\n" +
                    "预约医生：" + a.getDoctorName() + "（" + doctorTitle + "）\n" +
                    "预约日期：" + a.getDate() + "\n" +
                    "预约时间：" + a.getTimeSlot() + "\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "请按时就诊，祝您早日康复！";
        } else {
            return "❌ " + result.getMessage();
        }
    }

    // ==================== 3. 取消预约 ====================

    @Tool(name = "cancelAppointment",
            value = "取消已预约的挂号。" +
                    "根据患者姓名和身份证号查询所有有效预约，" +
                    "让用户选择要取消哪一条，然后调用此工具。" +
                    "预约ID可以从'查询我的预约'的结果中获取。")
    public String cancelAppointment(
            @P(value = "患者姓名", required = true) String username,
            @P(value = "患者身份证号", required = true) String idCard,
            @P(value = "要取消的预约ID，从查询结果中获取", required = true) Long appointmentId
    ) {
        log.info(
                "Cancel appointment. username={}, idCard={}, appointmentId={}",
                username,
                SensitiveDataMasker.maskIdCard(idCard),
                appointmentId
        );

        if (username == null || username.trim().isEmpty()) {
            return "❌ 请提供患者姓名。";
        }
        if (idCard == null || idCard.trim().isEmpty()) {
            return "❌ 请提供身份证号。";
        }
        if (appointmentId == null) {
            return "❌ 请提供要取消的预约ID。";
        }
        if (!identityWasProvidedInCurrentRequest(username, idCard, null)) {
            return "请先在本轮消息中提供患者姓名和身份证号，我不会使用历史会话中的个人信息执行取消预约。";
        }

        // 调用结构化取消预约入口，获取更精确的取消结果。
        AppointmentCancelResult result = AccountContext.currentUserId() == null
                ? appointmentService.cancelAppointmentWithResult(appointmentId, idCard)
                : appointmentService.cancelAppointmentWithResult(AccountContext.currentUserId(), appointmentId, idCard);

        // 如果取消成功，就返回成功提示。
        if (result.isSuccess()) {
            // 告诉用户预约已取消且号源已释放。
            return "✅ 取消预约成功！\n已为您取消该预约，号源已释放。如需重新预约，请随时联系我。";
        }

        // 取消失败时返回服务层给出的明确失败原因。
        return "❌ 取消预约失败：" + result.getMessage();
    }

    // ==================== 4. 查询我的预约 ====================

    @Tool(name = "queryAppointmentsByNameAndIdCard",
            value = "根据患者姓名和身份证号查询当前所有有效的预约记录。" +
                    "返回预约的ID、科室、医生、日期、时间等信息。" +
                    "预约ID可用于取消预约。")
    public String queryAppointmentsByNameAndIdCard(
            @P(value = "患者姓名", required = true) String username,
            @P(value = "患者身份证号", required = true) String idCard
    ) {
        log.info("Query appointments. username={}, idCard={}", username, SensitiveDataMasker.maskIdCard(idCard));

        if (username == null || username.trim().isEmpty()) {
            return "❌ 请提供患者姓名。";
        }
        if (idCard == null || idCard.trim().isEmpty()) {
            return "❌ 请提供身份证号。";
        }
        if (!identityWasProvidedInCurrentRequest(username, idCard, null)) {
            return "请先在本轮消息中提供患者姓名和身份证号，我不会使用历史会话中的个人信息查询预约。";
        }

        List<Appointment> appointments = AccountContext.currentUserId() == null
                ? appointmentService.findValidAppointmentsByNameAndIdCard(username, idCard)
                : appointmentService.findValidAppointmentsByNameAndIdCard(AccountContext.currentUserId(), username, idCard);

        if (appointments == null || appointments.isEmpty()) {
            return "📋 您好 " + username + "，您当前没有任何预约记录。\n如需预约挂号，请告诉我科室、医生和就诊时间。";
        }

        StringBuilder result = new StringBuilder();
        result.append("📋 您好 ").append(username).append("，您有以下预约记录：\n\n");

        for (int i = 0; i < appointments.size(); i++) {
            Appointment a = appointments.get(i);
            result.append(i + 1).append(". ");
            result.append("【ID:").append(a.getId()).append("】");
            result.append(a.getDepartment()).append(" | ");
            result.append(a.getDoctorName()).append(" | ");
            result.append(a.getDate()).append(" ").append(a.getTimeSlot());
            result.append("\n");
        }

        result.append("\n如需取消某条预约，请告诉我预约ID（如：取消ID为123的预约）。");
        return result.toString();
    }

    // ==================== 5. 查询医生详情 ====================

    @Tool(name = "getDoctorDetailByName",
            value = "根据医生姓名查询详细信息。" +
                    "当用户询问某位医生的具体情况、擅长领域、简介时，调用此工具。" +
                    "返回医生的姓名、职称、擅长领域、详细简介等。")
    public String getDoctorDetailByName(
            @P(value = "医生姓名", required = true) String doctorName
    ) {
        DoctorInfo doctor = doctorInfoService.getDoctorByName(doctorName);

        if (doctor == null) {
            return "❌ 未找到医生：" + doctorName + "，请确认医生姓名是否正确。";
        }

        StringBuilder result = new StringBuilder();
        result.append("👨‍⚕️ ").append(doctor.getDoctorName()).append("\n");
        result.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        result.append("职称：").append(doctor.getTitle()).append("\n");
        if (doctor.getSpecialty() != null && !doctor.getSpecialty().isEmpty()) {
            result.append("擅长：").append(doctor.getSpecialty()).append("\n");
        }
        if (doctor.getIntroduction() != null && !doctor.getIntroduction().isEmpty()) {
            result.append("简介：").append(doctor.getIntroduction()).append("\n");
        }
        result.append("━━━━━━━━━━━━━━━━━━━━━━\n");

        return result.toString();
    }

    // ==================== 辅助方法 ====================

    private String validateParams(String username, String idCard, String phone,
                                  String department, String doctorName,
                                  String dateStr, String timeSlot) {
        if (username == null || username.trim().isEmpty()) {
            return "❌ 请提供患者姓名。";
        }
        if (idCard == null || idCard.trim().isEmpty()) {
            return "❌ 请提供身份证号。";
        }
        if (!idCard.matches("\\d{17}[\\dXx]")) {
            return "❌ 身份证号格式不正确，应为18位数字（最后一位可能是数字或X）。";
        }
        if (phone == null || phone.trim().isEmpty()) {
            return "❌ 请提供手机号。";
        }
        if (!phone.matches("\\d{11}")) {
            return "❌ 手机号格式不正确，应为11位数字。";
        }
        if (department == null || department.trim().isEmpty()) {
            return "❌ 请提供预约科室。";
        }
        if (doctorName == null || doctorName.trim().isEmpty()) {
            return "❌ 请提供预约医生姓名。";
        }
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return "❌ 请提供预约日期。";
        }
        if (timeSlot == null || (!timeSlot.equals("上午") && !timeSlot.equals("下午"))) {
            return "❌ 请提供正确的时间段（上午或下午）。";
        }
        return null;
    }

    // parseToolDate 把工具入参中的日期字符串解析成 LocalDate。
    private LocalDate parseToolDate(String dateStr, String fieldName) {
        // 空日期无法解析，直接返回 null 交给调用方生成提示。
        if (dateStr == null || dateStr.trim().isEmpty()) {
            // null 表示解析失败。
            return null;
        }
        // 捕获日期格式错误，避免工具调用抛异常给 Agent。
        try {
            // 使用统一 yyyy-MM-dd 格式解析日期。
            return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // 记录具体字段名和错误值，方便后端排查。
            log.warn("Invalid tool date. fieldName={}, value={}", fieldName, dateStr);
            // null 表示解析失败。
            return null;
        }
    }

    // normalizeTimeSlot 将可选时间段规整为数据库可查询值。
    private String normalizeTimeSlot(String timeSlot) {
        // 空时间段表示不限制上午或下午。
        if (timeSlot == null || timeSlot.trim().isEmpty()) {
            // 返回 null 让 service 不加 timeSlot 过滤。
            return null;
        }
        // 去掉用户输入两侧空白。
        String normalized = timeSlot.trim();
        // 上午和下午是当前数据库支持的两个标准时间段。
        if ("上午".equals(normalized) || "下午".equals(normalized)) {
            // 返回标准时间段。
            return normalized;
        }
        // INVALID 是内部哨兵值，用于让调用方返回友好错误。
        return "INVALID";
    }

    /**
     * 身份证号脱敏（前6位 + ******** + 后4位）
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) return "****";
        return SensitiveDataMasker.maskIdCard(idCard);
    }

    /**
     * 手机号脱敏（前3位 + **** + 后4位）
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "****";
        return SensitiveDataMasker.maskPhone(phone);
    }



    private static final int MAX_FUTURE_DAYS = 7;  // 最多查询未来7天

    // ==================== 新增：智能预约工具 ====================

//    @Tool(name = "智能预约挂号",
//            value = "当用户说'约一个尽量近的时间'、'最近的时间'、'尽快'等模糊时间需求时，使用此工具。" +
//                    "此工具会自动查询未来多天内该科室所有可预约的号源，并推荐最近的日期。" +
//                    "用户确认后直接完成预约，无需再次查询。")
//    public String smartBookAppointment(
//            @P(value = "患者姓名", required = true) String username,
//            @P(value = "患者身份证号，18位", required = true) String idCard,
//            @P(value = "患者手机号，11位", required = true) String phone,
//            @P(value = "预约科室名称", required = true) String department,
//            @P(value = "预约医生姓名，如果不指定则自动推荐", required = false) String doctorName
//    ) {
//        System.out.println("=== 智能预约挂号 ===");
//        System.out.println("患者：" + username + "，" + idCard);
//        System.out.println("科室：" + department);
//        System.out.println("指定医生：" + (doctorName == null ? "无，自动推荐" : doctorName));
//
//        // 1. 参数校验
//        if (username == null || username.trim().isEmpty()) {
//            return "❌ 请提供患者姓名。";
//        }
//        if (idCard == null || idCard.trim().isEmpty()) {
//            return "❌ 请提供身份证号。";
//        }
//        if (!idCard.matches("\\d{17}[\\dXx]")) {
//            return "❌ 身份证号格式不正确，应为18位数字（最后一位可能是数字或X）。";
//        }
//        if (phone == null || phone.trim().isEmpty()) {
//            return "❌ 请提供手机号。";
//        }
//        if (!phone.matches("\\d{11}")) {
//            return "❌ 手机号格式不正确，应为11位数字。";
//        }
//        if (department == null || department.trim().isEmpty()) {
//            return "❌ 请提供预约科室。";
//        }
//
//        // 2. 查询未来7天的号源
//        LocalDate today = LocalDate.now();
//        List<LocalDate> datesToQuery = new ArrayList<>();
//        for (int i = 1; i <= MAX_FUTURE_DAYS; i++) {
//            datesToQuery.add(today.plusDays(i));
//        }
//
//        // 存储所有可预约的排班
//        List<AvailableSlot> availableSlots = new ArrayList<>();
//
//        for (LocalDate date : datesToQuery) {
//            // 分别查询上午和下午
//            List<DoctorSchedule> morningSlots = doctorScheduleService.findAvailableSchedules(
//                    department, date, "上午");
//            List<DoctorSchedule> afternoonSlots = doctorScheduleService.findAvailableSchedules(
//                    department, date, "下午");
//
//            for (DoctorSchedule slot : morningSlots) {
//                if (doctorName == null || slot.getDoctorName().equals(doctorName)) {
//                    availableSlots.add(new AvailableSlot(date, "上午", slot));
//                }
//            }
//            for (DoctorSchedule slot : afternoonSlots) {
//                if (doctorName == null || slot.getDoctorName().equals(doctorName)) {
//                    availableSlots.add(new AvailableSlot(date, "下午", slot));
//                }
//            }
//        }
//
//        // 3. 如果没有可预约的号源
//        if (availableSlots.isEmpty()) {
//            return "❌ 抱歉，" + department + "在未来" + MAX_FUTURE_DAYS + "天内没有可预约的号源。\n" +
//                    "建议您：\n" +
//                    "   • 选择其他科室\n" +
//                    "   • 或稍后再来查询（医院会定期更新排班）\n" +
//                    "   • 或联系医院客服咨询";
//        }
//
//        // 4. 取最近的可用时间（按日期和时间排序）
//        availableSlots.sort((a, b) -> {
//            if (!a.date.equals(b.date)) return a.date.compareTo(b.date);
//            return a.timeSlot.compareTo(b.timeSlot);
//        });
//
//        AvailableSlot nearest = availableSlots.get(0);
//        int remaining = nearest.schedule.getMaxAppointments() - nearest.schedule.getBookedCount();
//
//        // 5. 构建确认信息
//        StringBuilder confirmMsg = new StringBuilder();
//        confirmMsg.append("📋 为您查询到 ").append(department).append(" 最近的可预约时间：\n\n");
//        confirmMsg.append("   • 日期：").append(nearest.date).append("\n");
//        confirmMsg.append("   • 时间：").append(nearest.timeSlot).append("\n");
//        confirmMsg.append("   • 医生：").append(nearest.schedule.getDoctorName())
//                .append("（").append(nearest.schedule.getTitle()).append("）\n");
//        confirmMsg.append("   • 剩余号源：").append(remaining).append(" 个\n\n");
//
//        // 也列出其他可选时间（最多再显示2个）
//        if (availableSlots.size() > 1) {
//            confirmMsg.append("其他可选时间：\n");
//            for (int i = 1; i < Math.min(3, availableSlots.size()); i++) {
//                AvailableSlot slot = availableSlots.get(i);
//                confirmMsg.append("   • ").append(slot.date).append(" ").append(slot.timeSlot)
//                        .append("（").append(slot.schedule.getDoctorName()).append("）\n");
//            }
//            confirmMsg.append("\n");
//        }
//
//        confirmMsg.append("请确认以上信息是否正确？回复「确认」即可完成预约。");
//
//        // 这里不直接预约，而是返回确认信息，等待用户确认
//        // 同时将 nearest 信息存储到某个地方，等待用户确认后再调用 doBook
//        // 简化方案：直接在这里完成预约（因为用户已经表达了"约一个尽量近的时间"的意图）
//        // 但更好的做法是分两步
//
//        // 为了简化，我们直接预约最近的号源
//        // 实际生产环境应该先返回确认信息，等用户确认后再执行
//
//        // 执行预约
//        AppointmentResult result = appointmentService.bookAppointment(
//                username, idCard, phone, department,
//                nearest.schedule.getDoctorName(),
//                nearest.date, nearest.timeSlot);
//
//        if (result.isSuccess()) {
//            Appointment a = result.getAppointment();
//            return "✅ 已为您自动预约最近的号源！\n" +
//                    "━━━━━━━━━━━━━━━━━━━━━━\n" +
//                    "患者姓名：" + a.getUsername() + "\n" +
//                    "身份证号：" + maskIdCard(a.getIdCard()) + "\n" +
//                    "手机号：" + maskPhone(a.getPhone()) + "\n" +
//                    "预约科室：" + a.getDepartment() + "\n" +
//                    "预约医生：" + a.getDoctorName() + "\n" +
//                    "预约日期：" + a.getDate() + "\n" +
//                    "预约时间：" + a.getTimeSlot() + "\n" +
//                    "━━━━━━━━━━━━━━━━━━━━━━\n" +
//                    "请按时就诊，祝您早日康复！";
//        } else {
//            return "❌ 预约失败：" + result.getMessage();
//        }
//    }
//
//    // 内部类：存储可用号源
//    private static class AvailableSlot {
//        LocalDate date;
//        String timeSlot;
//        DoctorSchedule schedule;
//
//        AvailableSlot(LocalDate date, String timeSlot, DoctorSchedule schedule) {
//            this.date = date;
//            this.timeSlot = timeSlot;
//            this.schedule = schedule;
//        }
//    }



    private boolean identityWasProvidedInCurrentRequest(String username, String idCard, String phone) {
        // Direct tool calls without an authenticated request keep their legacy behavior.
        // Chat requests are fail-closed: identity must be present in the current message,
        // never silently copied from a worker's private memory.
        if (AccountContext.currentUserId() == null) return true;
        String message = normalizeIdentityText(AccountContext.currentUserMessage());
        if (message.isEmpty()) return false;
        if (!message.contains(normalizeIdentityText(username))
                || !message.contains(normalizeIdentityText(idCard))) return false;
        return phone == null || message.contains(normalizeIdentityText(phone));
    }

    private String normalizeIdentityText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }
}
