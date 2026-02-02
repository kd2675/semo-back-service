package semo.back.service.common.config;

import auth.common.core.context.UserContextArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * MVC Configuration
 *
 * Gateway Offloading 패턴:
 * - UserContextArgumentResolver를 등록하여 컨트롤러에서
 *   @RequestHeader 대신 UserContext를 직접 받을 수 있게 함
 * - Gateway가 보낸 X-User-Id, X-User-Name, X-User-Role 헤더를
 *   UserContext 객체로 변환
 */
@EnableWebMvc
@Configuration
@ComponentScan(basePackages = {"semo.back.service"})
public class MvcConfig implements WebMvcConfigurer {

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
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
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
