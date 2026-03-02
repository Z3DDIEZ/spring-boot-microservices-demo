package com.meridian.apigateway.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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

    /**
     * Fallback endpoint triggered when the {@code auth-service} is unreachable or
     * times out.
     *
     * @return A {@link Mono} wrapping a 503 Service Unavailable HTTP response with
     *         an error message.
     */
    @RequestMapping("/auth")
    public Mono<ResponseEntity<String>> authServiceFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Auth Service is currently unavailable. Please try again later."));
    }

    /**
     * Fallback endpoint triggered when the {@code order-service} is unreachable or
     * times out.
     *
     * @return A {@link Mono} wrapping a 503 Service Unavailable HTTP response with
     *         an error message.
     */
    @RequestMapping("/order")
    public Mono<ResponseEntity<String>> orderServiceFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Order Service is currently unavailable. Please try again later."));
    }
}
