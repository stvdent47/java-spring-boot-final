package mephi.bookingservice.config;

import feign.Logger;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mephi.bookingservice.entity.Role;
import mephi.bookingservice.entity.User;
import mephi.bookingservice.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FeignConfig {
    private final JwtService jwtService;

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public RequestInterceptor authRequestInterceptor() {
        return requestTemplate -> {
            User serviceAccount = User.builder()
                .id(0L)
                .username("booking-service")
                .email("booking-service@internal")
                .role(Role.ADMIN)
                .build();

            String token = jwtService.generateToken(serviceAccount);
            requestTemplate.header("Authorization", "Bearer " + token);

            log.debug(
                "Added service account token to Feign request: {}",
                requestTemplate.url()
            );
        };
    }
}
