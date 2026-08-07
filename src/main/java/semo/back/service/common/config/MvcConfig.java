package semo.back.service.common.config;

import auth.common.core.context.RequirePrincipalRoleFilter;
import auth.common.core.context.UserContextArgumentResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MVC Configuration
 *
 * Gateway Offloading 패턴:
 * - UserContextArgumentResolver를 등록하여 컨트롤러에서
 *   @RequestHeader 대신 UserContext를 직접 받을 수 있게 함
 * - Gateway가 보낸 X-User-Key, X-User-Name, X-User-Role 헤더를
 *   UserContext 객체로 변환
 */
@EnableWebMvc
@Configuration
@ComponentScan(basePackages = {"semo.back.service"})
public class MvcConfig implements WebMvcConfigurer {

    @Value("${semo.cors.allowed-origins:}")
    private String allowedOrigins;

    /**
     * UserContext ArgumentResolver 등록
     * - 컨트롤러에서 UserContext를 파라미터로 받을 수 있게 함
     * - auth-common-core에서 제공하는 클래스를 직접 생성하여 등록
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UserContextArgumentResolver());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        if (origins.isEmpty()) {
            return;
        }

        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public FilterRegistrationBean<RequirePrincipalRoleFilter> requirePrincipalRoleFilter(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping
    ) {
        FilterRegistrationBean<RequirePrincipalRoleFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequirePrincipalRoleFilter(handlerMapping));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
        ByteArrayHttpMessageConverter arrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
        arrayHttpMessageConverter.setSupportedMediaTypes(getSupportedMediaTypes());
        return arrayHttpMessageConverter;
    }

    private List<MediaType> getSupportedMediaTypes() {
        List<MediaType> list = new ArrayList<>();
        list.add(MediaType.IMAGE_JPEG);
        list.add(MediaType.IMAGE_PNG);
        list.add(MediaType.APPLICATION_OCTET_STREAM);
        list.add(MediaType.TEXT_HTML);
        return list;
    }
}
