// 声明当前类所在的包，controller 表示它负责处理 HTTP 请求。
package langchain4j.controller;

// ApiResponse 是项目统一的非流式接口返回结构。
import langchain4j.common.ApiResponse;
// AppointmentBookingRequest 是前端预约表单提交的结构化请求体。
import langchain4j.service.AppointmentBookingRequest;
// AppointmentBookingResponse 是前端预约表单提交后的结构化响应体。
import langchain4j.service.AppointmentBookingResponse;
// AppointmentCancelRequest 是前端取消预约请求体。
import langchain4j.service.AppointmentCancelRequest;
// AppointmentCancelResponse 是前端取消预约响应体。
import langchain4j.service.AppointmentCancelResponse;
// AppointmentCancelResult 是取消预约业务层返回的结构化结果。
import langchain4j.service.AppointmentCancelResult;
// AppointmentProvider 是预约能力提供者接口。
import langchain4j.service.AppointmentProvider;
// AppointmentQueryRequest 是前端查询本人预约请求体。
import langchain4j.service.AppointmentQueryRequest;
// AppointmentResult 是预约业务层返回的结构化结果。
import langchain4j.service.AppointmentResult;
// AppointmentScheduleOptionResponse 是预约表单下拉号源响应对象。
import langchain4j.service.AppointmentScheduleOptionResponse;
// AppointmentSummaryResponse 是前端展示预约记录时使用的安全摘要。
import langchain4j.service.AppointmentSummaryResponse;
// DoctorScheduleService 用来查询真实可预约排班。
import langchain4j.service.DoctorScheduleService;
// SensitiveDataMasker 用来在日志中隐藏身份证、手机号等敏感信息。
import langchain4j.util.SensitiveDataMasker;
import langchain4j.auth.AccountContext;
// Operation 用来给 Knife4j/OpenAPI 文档补充接口说明。
import io.swagger.v3.oas.annotations.Operation;
// Tag 用来给 Knife4j/OpenAPI 文档中的接口分组。
import io.swagger.v3.oas.annotations.tags.Tag;
// Logger 是 SLF4J 的日志接口。
import org.slf4j.Logger;
// LoggerFactory 用来创建当前类对应的日志对象。
import org.slf4j.LoggerFactory;
// LocalDate 表示预约日期。
import java.time.LocalDate;
// List 表示一组预约排班选项。
import java.util.List;
// GetMapping 表示处理 HTTP GET 请求。
import org.springframework.web.bind.annotation.GetMapping;
// PathVariable 表示从 URL 路径中读取变量。
import org.springframework.web.bind.annotation.PathVariable;
// PostMapping 表示处理 HTTP POST 请求。
import org.springframework.web.bind.annotation.PostMapping;
// RequestBody 表示从 HTTP 请求体中读取 JSON 参数。
import org.springframework.web.bind.annotation.RequestBody;
// RequestParam 表示从 URL 查询参数中读取过滤条件。
import org.springframework.web.bind.annotation.RequestParam;
// RequestMapping 表示给当前 Controller 的所有接口统一加路径前缀。
import org.springframework.web.bind.annotation.RequestMapping;
// RestController 表示当前类的返回值会直接写入 HTTP 响应体。
import org.springframework.web.bind.annotation.RestController;

// 给 OpenAPI 文档添加接口分组名称。
@Tag(name = "预约挂号")
// 标记当前类为 REST Controller。
@RestController
// 给当前类下所有接口加上 /xiaozhi/appointments 前缀。
@RequestMapping("/xiaozhi/appointments")
// 定义前端预约表单使用的 HTTP 接口控制器。
public class AppointmentController {

    // 创建当前类的日志对象。
    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    // 保存预约能力提供者依赖，当前实现是虚拟示范医院 Provider。
    private final AppointmentProvider appointmentProvider;

    // 保存医生排班服务依赖，用来给前端表单提供可选号源。
    private final DoctorScheduleService doctorScheduleService;

    // 构造器注入预约能力提供者。
    public AppointmentController(
            // Spring 会自动注入 AppointmentProvider Bean。
            AppointmentProvider appointmentProvider,
            // Spring 会自动注入 DoctorScheduleService Bean。
            DoctorScheduleService doctorScheduleService
    ) {
        // 保存预约能力提供者到成员变量。
        this.appointmentProvider = appointmentProvider;
        // 保存医生排班服务到成员变量。
        this.doctorScheduleService = doctorScheduleService;
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "查询可预约号源")
    // 声明 GET /xiaozhi/appointments/schedules 接口。
    @GetMapping("/schedules")
    // schedules 方法返回前端预约表单可选择的真实排班。
    public ApiResponse<List<AppointmentScheduleOptionResponse>> schedules(
            // 可选开始日期，前端默认传今天。
            @RequestParam(name = "startDate", required = false) LocalDate startDate,
            // 可选结束日期，前端默认传未来 30 天。
            @RequestParam(name = "endDate", required = false) LocalDate endDate,
            // 可选科室过滤。
            @RequestParam(name = "department", required = false) String department,
            // 可选时间段过滤。
            @RequestParam(name = "timeSlot", required = false) String timeSlot
    ) {
        // 没传开始日期时默认从今天开始。
        LocalDate actualStartDate = startDate == null ? LocalDate.now() : startDate;
        // 没传结束日期时默认查询未来 30 天。
        LocalDate actualEndDate = endDate == null ? actualStartDate.plusDays(30) : endDate;
        // 查询数据库中可预约的真实排班。
        List<AppointmentScheduleOptionResponse> options = doctorScheduleService
                // 调用排班服务查询可预约排班。
                .findAvailableSchedulesBetween(actualStartDate, actualEndDate, department, timeSlot)
                // 把实体列表转换成 stream。
                .stream()
                // 转换成前端响应 DTO。
                .map(AppointmentScheduleOptionResponse::from)
                // 收集成列表。
                .toList();
        // 返回统一成功响应。
        return ApiResponse.success(options);
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "前端表单提交预约")
    // 声明 POST /xiaozhi/appointments 接口。
    @PostMapping
    // book 方法接收前端结构化预约表单，并返回统一 API 响应。
    public ApiResponse<AppointmentBookingResponse> book(@RequestBody AppointmentBookingRequest request) {
        // 记录预约请求日志，但身份证和手机号必须脱敏。
        log.info(
                // 日志模板只记录关键业务字段。
                "Appointment form submitted. username={}, idCard={}, phone={}, department={}, doctorName={}, date={}, timeSlot={}",
                // 患者姓名。
                request == null ? null : request.getUsername(),
                // 脱敏身份证号。
                request == null ? null : SensitiveDataMasker.maskIdCard(request.getIdCard()),
                // 脱敏手机号。
                request == null ? null : SensitiveDataMasker.maskPhone(request.getPhone()),
                // 预约科室。
                request == null ? null : request.getDepartment(),
                // 预约医生。
                request == null ? null : request.getDoctorName(),
                // 预约日期。
                request == null ? null : request.getDate(),
                // 预约时间段。
                request == null ? null : request.getTimeSlot()
        );

        // 委托预约 Provider 提交预约。
        Long userId = AccountContext.currentUserId();
        AppointmentResult result = userId == null
                ? appointmentProvider.bookAppointment(request)
                : appointmentProvider.bookAppointment(userId, request);
        // 把业务结果转换成前端响应 DTO。
        AppointmentBookingResponse response = AppointmentBookingResponse.from(result);
        // 如果预约成功，就返回统一成功响应。
        if (result.isSuccess()) {
            // 返回 code=0 的成功响应。
            return ApiResponse.success(response);
        }
        // 如果预约失败，也携带结构化 data，方便前端读取 errorCode。
        return ApiResponse.fail(400, result.getMessage(), response);
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "查询本人有效预约")
    // 声明 POST /xiaozhi/appointments/search 接口。
    @PostMapping("/search")
    // search 方法按患者姓名和身份证号查询当前有效预约。
    public ApiResponse<List<AppointmentSummaryResponse>> search(@RequestBody AppointmentQueryRequest request) {
        // 如果请求为空或参数不合法，就返回业务失败。
        if (request == null || !request.validate().isEmpty()) {
            // 拼接校验错误；请求为空时给统一提示。
            String message = request == null ? "查询请求不能为空。" : String.join(" ", request.validate());
            // 返回失败响应，data 为空列表便于前端直接渲染空态。
            return ApiResponse.fail(400, message, List.of());
        }
        // 记录查询日志，身份证号必须脱敏。
        log.info(
                // 日志模板记录患者姓名和脱敏身份证号。
                "Appointment search submitted. username={}, idCard={}",
                // 患者姓名。
                request.getUsername(),
                // 脱敏身份证号。
                SensitiveDataMasker.maskIdCard(request.getIdCard())
        );
        // 委托 Provider 查询当前有效预约。
        Long userId = AccountContext.currentUserId();
        List<AppointmentSummaryResponse> appointments = (userId == null
                ? appointmentProvider.findValidAppointments(request.getUsername().trim(), request.getIdCard().trim())
                : appointmentProvider.findValidAppointments(userId, request.getUsername().trim(), request.getIdCard().trim()))
                // 转换成 stream。
                .stream()
                // 转成前端安全摘要。
                .map(AppointmentSummaryResponse::from)
                // 收集成列表。
                .toList();
        // 返回统一成功响应；没有记录时 data 是空数组。
        return ApiResponse.success(appointments);
    }

    // 给 OpenAPI 文档添加当前接口说明。
    @Operation(summary = "取消本人预约")
    // 声明 POST /xiaozhi/appointments/{appointmentId}/cancel 接口。
    @PostMapping("/{appointmentId}/cancel")
    // cancel 方法按预约 ID 和身份证号取消本人预约。
    public ApiResponse<AppointmentCancelResponse> cancel(
            // 从 URL 中读取要取消的预约 ID。
            @PathVariable("appointmentId") Long appointmentId,
            // 从请求体中读取身份证号。
            @RequestBody AppointmentCancelRequest request
    ) {
        // 读取身份证号，空请求时兜底为空。
        String idCard = request == null ? null : request.getIdCard();
        // 记录取消预约日志，身份证号必须脱敏。
        log.info(
                // 日志模板记录预约 ID 和脱敏身份证号。
                "Appointment cancel submitted. appointmentId={}, idCard={}",
                // 预约 ID。
                appointmentId,
                // 脱敏身份证号。
                SensitiveDataMasker.maskIdCard(idCard)
        );
        // 委托 Provider 执行取消预约。
        Long userId = AccountContext.currentUserId();
        AppointmentCancelResult result = userId == null
                ? appointmentProvider.cancelAppointment(appointmentId, idCard)
                : appointmentProvider.cancelAppointment(userId, appointmentId, idCard);
        // 把业务结果转换成前端响应 DTO。
        AppointmentCancelResponse response = AppointmentCancelResponse.from(result);
        // 成功时返回 code=0。
        if (result.isSuccess()) {
            // 返回统一成功响应。
            return ApiResponse.success(response);
        }
        // 失败时返回 code=400，并保留业务错误码。
        return ApiResponse.fail(400, result.getMessage(), response);
    }
}
