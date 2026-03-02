package com.meridian.inventoryservice.infrastructure;

import com.meridian.inventoryservice.domain.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB Repository orchestrating reads and writes to the
 * `products` document collection.
 */
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    /**
     * Lookups a distinct product document using its human-readable Stock Keeping
     * Unit.
     *
     * @param sku The canonical business identifier of the physical product.
     * @return An Optional wrapping the entity if present.
     */
    Optional<Product> findBySku(String sku);
}
