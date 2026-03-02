package com.meridian.inventoryservice.presentation;

import com.meridian.inventoryservice.application.InventoryService;
import com.meridian.inventoryservice.application.dto.ProductRequest;
import com.meridian.inventoryservice.application.dto.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller acting as the public-facing Presentation Layer for the
 * Catalog &amp; Inventory boundary.
 * <p>
 * Implements granular role-based access control (RBAC). For example, only
 * principals holding
 * {@code SCOPE_ADMIN} can mutate the catalog, whereas reads are globally open.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Registers a strictly new product inside the catalog. Admin exclusive.
     *
     * @param request The formally validated blueprint of the new product.
     * @return HTTP 201 Created and the flattened product summary.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = inventoryService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Publicly fetches all available products across the system.
     *
     * @return HTTP 200 OK containing an array of all products.
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(inventoryService.getAllProducts());
    }

    /**
     * Publicly fetches a discrete product by its primary key.
     *
     * @param id The exact MongoDB ObjectId string.
     * @return HTTP 200 OK and the product, or HTTP 404 Not Found if missing.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(inventoryService.getProductById(id));
    }
}
