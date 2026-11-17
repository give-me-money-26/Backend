package kr.ac.catholic.aeroharvest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String[] allowedOrigins;

    /**
     * allowed-origins: 프런트 주소 (운영에서는 CORS_ALLOWED_ORIGINS 환경변수로 덮어씀)
     * api-docs-origins: Swagger UI가 열리는 서버 자신의 주소. 프록시(HTTPS 종료) 뒤에서는
     * 같은 주소의 POST도 교차 출처로 판정되므로 환경변수와 별도로 항상 허용한다.
     */
    public WebConfig(@Value("${aeroharvest.cors.allowed-origins}") String allowedOrigins,
                     @Value("${aeroharvest.cors.api-docs-origins:}") String apiDocsOrigins) {
        this.allowedOrigins = Stream.concat(split(allowedOrigins), split(apiDocsOrigins))
                .distinct()
                .toArray(String[]::new);
    }

    private static Stream<String> split(String origins) {
        return Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
