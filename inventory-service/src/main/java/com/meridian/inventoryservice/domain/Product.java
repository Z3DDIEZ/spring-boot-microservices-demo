package com.meridian.inventoryservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Core domain aggregate representing a physical or digital, sellable item in
 * the Meridian catalog.
 * <p>
 * This entity tracks both fundamental metadata (name, description, explicit
 * pricing) and
 * highly volatile operational data (current stock levels).
 * Protected from concurrent overselling via the {@code @Version} optimistic
 * locking mechanism.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
@CompoundIndexes({
        @CompoundIndex(name = "category_idx", def = "{'categoryId': 1}")
})
public class Product {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sku;

    private String name;

    private String description;

    private String categoryId;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    private Integer stockQuantity;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Long version;

    /**
     * Safely attempts to decrement the available stock if sufficient quantities
     * exist.
     *
     * @param quantity The discrete volume to deduct.
     * @throws IllegalStateException if the requested quantity exceeds available
     *                               bounds (Oversell mitigation).
     */
    public void reserveStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException("Insufficient stock for product " + this.id);
        }
        this.stockQuantity -= quantity;
    }

    /**
     * Unconditionally increments the stock counter. Commonly utilized during
     * incoming fulfillment shipments or when a user order is cancelled/returned.
     *
     * @param quantity The discrete volume to restock.
     */
    public void releaseStock(int quantity) {
        this.stockQuantity += quantity;
    }
}
