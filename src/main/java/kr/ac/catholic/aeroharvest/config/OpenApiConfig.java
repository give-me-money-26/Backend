package kr.ac.catholic.aeroharvest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger UI(/swagger-ui.html)와 OpenAPI 문서(/v3/api-docs) 기본 정보.
 * 서버 주소를 "/"(현재 접속한 주소)로 두어, Cloudflare HTTPS 뒤에서도
 * Swagger의 Try it out 요청이 http로 바뀌지 않게 한다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aeroHarvestOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AeroHarvest API")
                        .version("v1")
                        .description("건물 배기구 바람의 발전량·절감액·탄소 저감을 계산하는 AeroHarvest 백엔드 API"))
                .servers(List.of(new Server().url("/").description("현재 서버")));
    }
}
