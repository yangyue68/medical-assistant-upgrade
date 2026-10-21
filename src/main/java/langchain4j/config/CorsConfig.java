// 声明当前类位于 config 包，表示它是 Spring 的配置类。
package langchain4j.config;

import langchain4j.auth.AuthInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;

// Bean 注解用来把方法的返回值注册成 Spring 容器中的 Bean。
import org.springframework.context.annotation.Bean;
// Configuration 注解标记当前类是一个 Spring 配置类。
import org.springframework.context.annotation.Configuration;
// CorsRegistry 是 Spring MVC 提供的跨域规则注册器。
import org.springframework.web.servlet.config.annotation.CorsRegistry;
// WebMvcConfigurer 是 Spring MVC 的扩展接口，允许自定义 MVC 相关配置。
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

// 标记当前类为 Spring 配置类，应用启动时会被扫描并加载。
@Configuration
// 定义跨域配置类，集中解决前后端分离部署时浏览器的跨域访问限制。
public class CorsConfig {

    @Bean
    public AuthInterceptor authInterceptor(ObjectMapper objectMapper) {
        return new AuthInterceptor(objectMapper);
    }

    @Bean
    public WebMvcConfigurer authWebMvcConfigurer(AuthInterceptor authInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(authInterceptor);
            }
        };
    }

    // 把一个 WebMvcConfigurer 实现注册为 Bean，Spring MVC 启动时会回调它完成配置。
    @Bean
    // 定义跨域配置 Bean 的工厂方法，返回定制的 MVC 配置器。
    public WebMvcConfigurer corsConfigurer() {
        // 使用匿名内部类实现 WebMvcConfigurer，只重写需要定制的方法，其余方法使用默认实现。
        return new WebMvcConfigurer() {
            // 重写添加跨域映射规则的方法，Spring MVC 初始化时会自动回调。
            @Override
            // registry 是跨域规则的注册入口，所有跨域规则都通过它登记。
            public void addCorsMappings(CorsRegistry registry) {
                // 对项目中所有接口路径（/**）应用同一套跨域规则。
                registry.addMapping("/**")
                        // 允许任意来源的前端域名访问；因为下面 allowCredentials=true，不能用 allowedOrigins("*")，所以改用 allowedOriginPatterns。
                        .allowedOriginPatterns("*")  // 改用 allowedOriginPatterns
                        // 明确列出允许的 HTTP 方法；OPTIONS 必须包含，因为浏览器的预检请求使用 OPTIONS 方法。
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 明确列出方法
                        // 允许跨域请求携带任意自定义请求头。
                        .allowedHeaders("*")
                        // 允许浏览器在跨域请求中携带 Cookie 等凭证信息。
                        .allowCredentials(true)  // 允许携带凭证
                        // 预检请求结果缓存 3600 秒（1 小时），期间浏览器不再重复发送 OPTIONS 预检请求。
                        .maxAge(3600);  // 预检请求缓存1小时
            }
        };
    }
}
