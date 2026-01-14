package mephi.hotelservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    public OpenAPI hotelServiceOpenAPI() {
        return new OpenAPI()
            .info(
                new Info()
                    .title("Hotel Service API")
                    .description("Microservice for managing hotels and rooms in the Hotel Booking System")
                    .version("1.0.0")
                    .contact(
                        new Contact()
                            .name("Hotel Booking Team")
                            .email("support@hotel-booking.com")
                    )
                    .license(
                        new License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local Development Server"),
                new Server()
                    .url("http://localhost:8080/api")
                    .description("API Gateway")
            ))
            .components(
                new Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT token authentication. Obtain token from /api/user/auth endpoint.")
                ))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
