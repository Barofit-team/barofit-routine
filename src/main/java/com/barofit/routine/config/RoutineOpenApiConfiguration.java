package com.barofit.routine.config;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RoutineOpenApiConfiguration {
    @Bean public OpenAPI routineOpenApi() {
        return new OpenAPI().info(new Info().title("Barofit Routine API").version("v1")
                .description("운동 종목, 가이드/TTS 원문, 사용자 루틴, 기본 루틴"))
            .components(new Components().addSecuritySchemes("routineAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                    .description("소셜 로그인 응답의 accessToken을 입력하세요. 내부 사용자 UUID는 검증된 JWT의 sub에서 읽습니다.")));
    }
}
