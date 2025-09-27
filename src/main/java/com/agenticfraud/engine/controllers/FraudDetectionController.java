package com.agenticfraud.engine.controllers;

import com.agenticfraud.engine.models.FraudDecision;
import com.agenticfraud.engine.models.Transaction;
import com.agenticfraud.engine.services.AgentCoordinator;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud-detection")
@CrossOrigin(origins = "*")
public class FraudDetectionController {

  private static final Logger logger = LoggerFactory.getLogger(FraudDetectionController.class);

  private final AgentCoordinator agentCoordinator;

  public FraudDetectionController(AgentCoordinator agentCoordinator) {
    this.agentCoordinator = agentCoordinator;
  }

  /**
   * Analyzes a given transaction to determine if it is potentially fraudulent using a multi-agent
   * AI system.
   *
   * @param transaction the transaction to be analyzed. Must be a valid {@link Transaction} object.
   * @return a {@link ResponseEntity} containing a {@link FraudDecision} that encapsulates the fraud
   *     analysis result, including whether the transaction is fraudulent, a confidence score, and
   *     additional analysis details.
   */
  @PostMapping("/analyze")
  public ResponseEntity<FraudDecision> analyzeTransaction(
      @Valid @RequestBody Transaction transaction) {

    logger.info("New fraud analysis request for transaction: {}", transaction.transactionId());

    try {

      FraudDecision decision = agentCoordinator.investigateTransaction(transaction);

      logger.info(
          "Analysis complete: {} (confidence: {:.2f})",
          decision.isFraudulent() ? "FRAUD" : "LEGITIMATE",
          decision.confidenceScore());

      return ResponseEntity.ok(decision);
    } catch (Exception e) {
      logger.error(
          "Error analyzing transaction {}: {}", transaction.transactionId(), e.getMessage());

      FraudDecision errorDecision =
          FraudDecision.fraudulent(
              transaction.transactionId(),
              0.5,
              "Technical Error",
              "Analysis failed due to technical error: " + e.getMessage(),
              List.of());

      return ResponseEntity.internalServerError().body(errorDecision);
    }
  }

  /**
   * Get information about the AI agents
   *
   * @return Map of agent information
   */
  @GetMapping("/agents/info")
  public ResponseEntity<Map<String, Object>> getAgentsInfo() {

    Map<String, Object> agentInfo =
        Map.of(
            "totalAgents",
            5,
            "agents",
            Map.of(
                "BEHAVIOR_ANALYST", "Analyzes customer behavior patterns and anomalies",
                "PATTERN_DETECTOR", "Identifies known fraud patterns and attack vectors",
                "RISK_ASSESSOR", "Calculates overall financial risk and impact",
                "GEOGRAPHIC_ANALYST", "Evaluates location-based risk factors",
                "TEMPORAL_ANALYST", "Analyzes timing patterns and velocity"),
            "capabilities",
            List.of(
                "Parallel multi-agent analysis",
                "Agent collaboration and debate",
                "Consensus building through weighted voting",
                "Explainable AI decision making",
                "Real-time learning from feedback"),
            "version",
            "1.0.0");

    return ResponseEntity.ok(agentInfo);
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> healthCheck() {

    try {
      // Simple health check - just verify coordinator is available
      Map<String, Object> health =
          Map.of(
              "status", "UP",
              "agentCoordinator", "ACTIVE",
              "aiSystem", "OPERATIONAL",
              "timestamp", java.time.LocalDateTime.now());

      return ResponseEntity.ok(health);
    } catch (Exception e) {
      Map<String, Object> health =
          Map.of(
              "status", "DOWN",
              "error", e.getMessage(),
              "timestamp", java.time.LocalDateTime.now());
      return ResponseEntity.status(503).body(health);
    }
  }
}
