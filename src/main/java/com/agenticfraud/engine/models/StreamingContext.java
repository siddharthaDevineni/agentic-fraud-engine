package com.agenticfraud.engine.models;

/**
 * StreamingContext
 * @param recentTransactionsCount recent transactions count in the last 5 minutes
 * @param customerProfile customer profile
 * @param contextSummary context summary
 */
public record StreamingContext(
    Long recentTransactionsCount, // Transactions in the last 5 minutes
    CustomerProfile customerProfile,
    String contextSummary) {

  public boolean hasHighVelocity() {
    return recentTransactionsCount != null && recentTransactionsCount > 3;
  }

  public String getAIContext() {
    StringBuilder context = new StringBuilder();

    if (hasHighVelocity()) {
      context.append(
          String.format(
              "HIGH VELOCITY: %d transactions in the last 5 minutes", recentTransactionsCount));
    }

    if (customerProfile != null) {
      context.append(
          String.format(
              "Customer baseline: $%.2f avg, %s risk.",
              customerProfile.averageTransactionAmount().doubleValue(),
              customerProfile.riskLevel()));
    }

    return context.toString();
  }
}
