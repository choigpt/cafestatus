package com.example.cafestatus.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI cafeStatusOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cafe Status API")
                        .description("카페 실시간 혼잡도 및 좌석 가용성 API")
                        .version("1.0.0"));
    }
}
