package mephi.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {
    @GetMapping("/booking")
    public Mono<ResponseEntity<Map<String, Object>>> bookingServiceFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", "Booking Service is currently unavailable. Please try again later.",
                "service", "booking-service",
                "timestamp", LocalDateTime.now().toString()
            ))
        );
    }

    @GetMapping("/hotel")
    public Mono<ResponseEntity<Map<String, Object>>> hotelServiceFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", "Hotel Service is currently unavailable. Please try again later.",
                "service", "hotel-service",
                "timestamp", LocalDateTime.now().toString()
            ))
        );
    }

    @GetMapping("/default")
    public Mono<ResponseEntity<Map<String, Object>>> defaultFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", "The requested service is currently unavailable. Please try again later.",
                "timestamp", LocalDateTime.now().toString()
            ))
        );
    }
}
