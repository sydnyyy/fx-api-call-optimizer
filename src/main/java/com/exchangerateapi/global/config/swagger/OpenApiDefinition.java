package com.exchangerateapi.global.config.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "환율 OPEN API 호출 테스트 서비스",
                description = "실시간 환율 제공",
                version = "0.0.1"
        )
)
@Configuration
public class OpenApiDefinition {
}
