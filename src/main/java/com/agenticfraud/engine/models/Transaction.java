package com.agenticfraud.engine.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record Transaction(
        @NotBlank String transactionId,
        @NotBlank String customerId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String merchantId,
        @NotBlank String merchantCategory,
        @NotBlank String location,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        LocalDateTime timestamp,
        Map<String, Object> metadata
) {

    // Helper methods for AI agent analysis
    public String toAnalysisText() {
        return String.format(
                "Transaction: %s, Customer: %s, Amount: %s %s, Merchant: %s (%s), Location: %s, Time: %s",
                transactionId, customerId, amount, currency,
                merchantId, merchantCategory, location, timestamp
        );
    }

    public Map<String, Object> toFeatureMap() {
        return Map.of(
                "amount", amount,
                "currency", currency,
                "merchantCategory", merchantCategory,
                "location", location,
                "hour", timestamp.getHour(),
                "dayOfWeek", timestamp.getDayOfWeek().getValue(),
                "metadata", metadata != null ? metadata : Map.of()
        );
    }
}