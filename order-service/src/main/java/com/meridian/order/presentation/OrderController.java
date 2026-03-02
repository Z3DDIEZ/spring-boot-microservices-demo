package com.meridian.order.presentation;

import com.meridian.order.application.OrderService;
import com.meridian.order.domain.Order;
import com.meridian.order.presentation.dto.CreateOrderRequest;
import com.meridian.order.presentation.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller exposing Order CRUD operations.
 * All endpoints require a valid JWT and extract the userId from the token's
 * subject claim.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

        private final OrderService orderService;

        /**
         * Creates a new logical order belonging to the authenticated principal.
         * Transforms and parses JSON payload rules into resilient data inputs.
         *
         * @param jwt     the valid decoded JWT principal injected by Spring Security.
         * @param request the validated monolithic request body detailing product IDs
         *                and configurations.
         * @return HTTP 201 Created with the strictly populated OrderResponse entity
         *         state.
         */
        @PostMapping
        public ResponseEntity<OrderResponse> createOrder(
                        @AuthenticationPrincipal Jwt jwt,
                        @Valid @RequestBody CreateOrderRequest request) {

                UUID userId = UUID.fromString(jwt.getSubject());

                List<OrderService.OrderItemInput> items = request.getItems().stream()
                                .map(i -> new OrderService.OrderItemInput(
                                                i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                                .collect(Collectors.toList());

                Order order = orderService.createOrder(userId, items);
                return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.fromEntity(order));
        }

        /**
         * Lists all chronological orders historically transacted by the authenticated
         * user.
         *
         * @param jwt the valid decoded JWT principal.
         * @return HTTP 200 OK encompassing an array of flattened OrderResponse DTOs.
         */
        @GetMapping
        public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
                UUID userId = UUID.fromString(jwt.getSubject());

                List<OrderResponse> orders = orderService.getOrdersByUserId(userId).stream()
                                .map(OrderResponse::fromEntity)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(orders);
        }

        /**
         * Retrieves a single order by its ID.
         *
         * @param id the order UUID path variable
         * @return 200 OK with the OrderResponse, or 404 if not found
         */
        @GetMapping("/{id}")
        public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
                return orderService.getOrderById(id)
                                .map(OrderResponse::fromEntity)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }
}
