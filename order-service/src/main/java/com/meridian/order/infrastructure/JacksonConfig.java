package com.meridian.order.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Jackson {@link ObjectMapper} bean for the application context.
 * <p>
 * Ensures modern Java 8+ temporal types (e.g., {@link java.time.LocalDateTime})
 * serialize directly to ISO-8601 string representations instead of unreadable
 * arrays.
 */
@Configuration
public class JacksonConfig {

    /**
     * Produces a custom Jackson ObjectMapper.
     * 
     * @return The configured ObjectMapper capable of ISO-8601 JavaTime
     *         serialization.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
