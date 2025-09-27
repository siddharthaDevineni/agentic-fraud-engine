package com.agenticfraud.engine.models;

import java.math.BigDecimal;
import java.util.List;

public record CustomerProfile(
    String customerId,
    BigDecimal averageTransactionAmount,
    BigDecimal dailySpendingLimit,
    List<String> transactionCategories,
    String primaryLocation,
    String riskLevel // LOW, MEDIUM, HIGH
    ) {

  public boolean isAmountUnusual(BigDecimal amount) {
    return amount.compareTo(averageTransactionAmount.multiply(BigDecimal.valueOf(3))) > 0;
  }

  public boolean isTypicalCategory(String category) {
    return transactionCategories.contains(category);
  }

  public boolean isTypicalLocation(String location) {
    return primaryLocation.equalsIgnoreCase(location);
  }
}
