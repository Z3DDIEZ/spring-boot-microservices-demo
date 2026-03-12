package com.meridian.apigateway.presentation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Controller handling resilience fallback responses when primary microservices
 * are unavailable.
 * <p>
 * Mapped to the CircuitBreaker filters in the Spring Cloud Gateway
 * configuration.
 * Rather than dropping connections or timing out, the Gateway routes failed
 * requests to these endpoints to provide a graceful degradation UX to the
 * client.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    public record ErrorResponse(String code, String message, Instant timestamp) {
    }

    /**
     * Fallback endpoint triggered when the {@code auth-service} is unreachable or
     * times out.
     *
     * @return A {@link Mono} wrapping a 503 Service Unavailable HTTP response with
     *         an error message.
     */
    @GetMapping("/auth")
    public Mono<ResponseEntity<ErrorResponse>> authServiceFallback() {
        return buildServiceUnavailableResponse("Auth Service");
    }

    /**
     * Fallback endpoint triggered when the {@code order-service} is unreachable or
     * times out.
     *
     * @return A {@link Mono} wrapping a 503 Service Unavailable HTTP response with
     *         an error message.
     */
    @GetMapping("/order")
    public Mono<ResponseEntity<ErrorResponse>> orderServiceFallback() {
        return buildServiceUnavailableResponse("Order Service");
    }

    private Mono<ResponseEntity<ErrorResponse>> buildServiceUnavailableResponse(String serviceName) {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.RETRY_AFTER, "30") // Suggest client wait 30 seconds
                .body(new ErrorResponse("SERVICE_UNAVAILABLE",
                        serviceName + " is currently unavailable. Please try again later.", Instant.now())));
    }
}
