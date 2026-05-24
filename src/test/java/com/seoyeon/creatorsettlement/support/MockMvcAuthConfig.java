package com.seoyeon.creatorsettlement.support;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * 기능 테스트용 기본 인증 헤더(관리자) 주입.
 * 인가 자체는 AuthorizationTest 에서 별도로 검증하고,
 * 비즈니스 로직 테스트는 인증된 관리자 컨텍스트를 가정한다.
 */
@TestConfiguration
public class MockMvcAuthConfig {

    @Bean
    MockMvcBuilderCustomizer adminDefaultHeaders() {
        return builder -> builder.defaultRequest(
                MockMvcRequestBuilders.get("/")
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "ADMIN"));
    }
}
