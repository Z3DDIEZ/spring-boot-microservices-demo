package com.meridian.inventoryservice.application;

import com.meridian.inventoryservice.application.dto.ProductRequest;
import com.meridian.inventoryservice.application.dto.ProductResponse;
import com.meridian.inventoryservice.domain.InventoryReservation;
import com.meridian.inventoryservice.domain.Product;
import com.meridian.inventoryservice.infrastructure.ProductRepository;
import com.meridian.inventoryservice.infrastructure.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating new product with SKU: {}", request.getSku());

        if (productRepository.findBySku(request.getSku()).isPresent()) {
            throw new IllegalArgumentException("Product with SKU " + request.getSku() + " already exists");
        }

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .categoryId(request.getCategoryId())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .build();

        Product savedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(savedProduct);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + id));
        return ProductResponse.fromEntity(product);
    }

    /**
     * Attempts to reserve stock for a list of items associated with an order.
     * Throws an exception if any item fails reservation.
     */
    @Transactional
    public void reserveStock(UUID orderId, Map<String, Integer> itemsToReserve) {
        log.info("Reserving stock for Order ID: {}", orderId);

        // Fail fast if already reserved/processed
        List<InventoryReservation> existingReservations = reservationRepository.findByOrderId(orderId);
        if (!existingReservations.isEmpty()) {
            log.warn("Reservations already exist for Order ID {}. Skipping.", orderId);
            return;
        }

        for (Map.Entry<String, Integer> entry : itemsToReserve.entrySet()) {
            String sku = entry.getKey();
            Integer quantity = entry.getValue();

            Product product = productRepository.findBySku(sku)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with SKU: " + sku));

            try {
                product.reserveStock(quantity);
            } catch (IllegalStateException e) {
                log.error("Failed to reserve stock for Order {} on SKU {}: {}", orderId, sku, e.getMessage());
                throw new IllegalStateException("Insufficient stock for SKU " + sku);
            }

            productRepository.save(product);

            InventoryReservation reservation = InventoryReservation.create(orderId, product.getId(), quantity);
            reservation.confirm();
            reservationRepository.save(reservation);
        }

        log.info("Successfully reserved stock for Order ID: {}", orderId);
    }
}
