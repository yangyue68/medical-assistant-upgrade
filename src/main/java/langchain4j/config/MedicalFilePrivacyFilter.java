// 声明应用配置组件所在的包。
package langchain4j.config;

// 引入过滤器链，用于继续执行后续过滤器和 Controller。
import jakarta.servlet.FilterChain;
// 引入 Servlet 异常类型。
import jakarta.servlet.ServletException;
// 引入 HTTP 请求类型，用于判断当前接口路径。
import jakarta.servlet.http.HttpServletRequest;
// 引入 HTTP 响应类型，用于写入隐私保护响应头。
import jakarta.servlet.http.HttpServletResponse;
// 引入输入输出异常类型。
import java.io.IOException;
// 引入组件注解，使过滤器自动注册到 Spring 容器。
import org.springframework.stereotype.Component;
// 引入单次请求过滤器基类，确保每个请求只执行一次。
import org.springframework.web.filter.OncePerRequestFilter;

/** 防止浏览器或代理缓存敏感医疗文件接口响应。 */
// 将本类注册为 Spring 管理的过滤器组件。
@Component
public class MedicalFilePrivacyFilter extends OncePerRequestFilter {

    // 覆盖路径选择逻辑，仅过滤医疗文件接口。
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 非医疗文件路径返回 true，由框架跳过当前过滤器。
        return !request.getRequestURI().startsWith("/xiaozhi/medical-files");
    }

    // 覆盖请求过滤逻辑，为敏感响应添加禁止缓存和类型嗅探响应头。
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    // 接收待写入安全响应头的 HTTP 响应。
                                    HttpServletResponse response,
                                    // 接收后续过滤器链。
                                    FilterChain filterChain) throws ServletException, IOException {
        // 禁止浏览器和中间代理存储医疗文件接口响应。
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        // 兼容旧版 HTTP/1.0 客户端的禁止缓存行为。
        response.setHeader("Pragma", "no-cache");
        // 禁止浏览器根据响应内容猜测 MIME 类型。
        response.setHeader("X-Content-Type-Options", "nosniff");
        // 完成响应头设置后继续执行正常请求链。
        filterChain.doFilter(request, response);
    }
}
