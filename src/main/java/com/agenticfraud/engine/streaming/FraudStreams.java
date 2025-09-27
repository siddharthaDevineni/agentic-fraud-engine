package com.agenticfraud.engine.streaming;

import com.agenticfraud.engine.models.CustomerProfile;
import com.agenticfraud.engine.models.FraudDecision;
import com.agenticfraud.engine.models.StreamingContext;
import com.agenticfraud.engine.models.Transaction;
import com.agenticfraud.engine.services.AgentCoordinator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FraudStreams {

  private static final Logger logger = LoggerFactory.getLogger(FraudStreams.class);

  private final AgentCoordinator agentCoordinator;
  private KafkaStreams kafkaStreams;

  // Real-time context makes AI agents smarter
  @PostConstruct
  public void startStreaming() {

    logger.info("Starting Fraud Detection Streaming...");

    StreamsBuilder builder = new StreamsBuilder();

    // Configure JSON serdes
    JsonSerde<Transaction> transactionSerde = new JsonSerde<>(Transaction.class);
    JsonSerde<CustomerProfile> customerProfileSerde = new JsonSerde<>(CustomerProfile.class);
    JsonSerde<FraudDecision> decisionJsonSerde = new JsonSerde<>(FraudDecision.class);

    // Input streams
    KStream<String, Transaction> transactions =
        builder.stream("transactions", Consumed.with(Serdes.String(), transactionSerde));

    KTable<String, CustomerProfile> customerProfiles =
        builder.table("customerProfiles", Consumed.with(Serdes.String(), customerProfileSerde));

    // 1. Velocity context for AI agents, which provide velocity patterns to detect rapid-fire
    // attacks
    KTable<String, Long> velocityContext =
        transactions
            .selectKey((key, txn) -> txn.customerId())
            .groupByKey(Grouped.with(Serdes.String(), transactionSerde))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
            .count(Materialized.as("velocity-windows"))

            // Convert windowed table to regular table but first to KStream with each record as a
            // Windowed key and value
            .toStream()
            // Windowed("John", [10:00-10:05]), Value=3 -> Key="John", Value=3
            .map((windowedKey, count) -> KeyValue.pair(windowedKey.key(), count))
            .groupByKey()
            .reduce(
                (oldValue, newValue) -> newValue, // keep the latest count
                Materialized.as("current-velocity"));

    KStream<String, Transaction> contextEnrichedTransactions =
        transactions
            .selectKey((key, txn) -> txn.customerId())
            // Add customer behavioral context for AI agents
            .leftJoin(
                customerProfiles,
                (txn, profile) -> {
                  logger.info(
                      "Enriching transaction {} with profile {}", txn.transactionId(), profile);
                  return txn; // Transaction remains the same, we'll use profile in context
                },
                Joined.with(Serdes.String(), transactionSerde, customerProfileSerde))

            // Add velocity context for AI agents
            .leftJoin(
                velocityContext,
                (txn, velocity) -> {
                  if (velocity != null && velocity > 3) {
                    logger.info(
                        "High velocity detected for customer {}: {} txns/5min",
                        txn.customerId(),
                        velocity);
                  }
                  return txn; // Transaction remains the same, we'll use velocity in context
                },
                Joined.with(Serdes.String(), transactionSerde, Serdes.Long()));

    KStream<String, FraudDecision> contextualDecisions =
        contextEnrichedTransactions.mapValues(
            ((readOnlyKey, txn) -> {
              try {
                // Build streaming context for AI agents
                String customerId = txn.customerId();

                // Get customer profile (from previous join)
                CustomerProfile profile = getCustomerProfile(customerId);

                // Get velocity context (from previous join)
                Long velocity = getVelocityContext(customerId);

                // Create AI-focused streaming context
                StreamingContext context =
                    new StreamingContext(
                        velocity, profile, buildContextSummary(velocity, profile, txn));

                logger.info(
                    "Analyzing with streaming context for transaction {}: {}",
                    txn.transactionId(),
                    context.getAIContext());

                // AI agents analyze transaction with streaming context
                return agentCoordinator.investigateTransactionWithStreamingContext(txn, context);

              } catch (Exception e) {
                logger.error("Error in contextual analysis: {}", e.getMessage(), e);
                return AgentCoordinator.createErrorDecision(txn, e);
              }
            }));

    logger.info("AI-enhanced streaming context created");

    // Intelligent Routing: AI-driven decision routing
    Map<String, KStream<String, FraudDecision>> intelligentRouting =
        contextualDecisions
            .split(Named.as("intelligent-routing"))

            // AI High Confidence Fraud
            .branch(
                (key, decision) -> decision.isFraudulent() && decision.confidenceScore() > 0.8,
                Branched.as("ai-fraud-alert"))

            // AI Uncertain - Human Review
            .branch(
                (key, decision) -> decision.isFraudulent() || decision.requireManuelReview(),
                Branched.as("ai-review-needed"))

            // AI approved
            .defaultBranch(Branched.as("ai-approved"));

    // Route to appropriate business processes
    intelligentRouting
        .get("intelligent-routing-ai-fraud-alert")
        .peek(
            (key, decision) ->
                logger.warn(
                    "AI FRAUD ALERT: {} (Confidence: {:.1f}%) - agents: {}",
                    decision.transactionId(),
                    decision.confidenceScore() * 100,
                    decision.agentInsights().size()))
        .mapValues(this::createFraudAlert)
        .to("fraud-alerts", Produced.with(Serdes.String(), new JsonSerde<>()));

    intelligentRouting
        .get("intelligent-routing-ai-review-needed")
        .peek(
            (key, decision) ->
                logger.info(
                    "AI REVIEW NEEDED: {} (confidence: {:.1f}%)",
                    decision.transactionId(), decision.confidenceScore() * 100))
        .mapValues(this::createReviewCase)
        .to("human-review", Produced.with(Serdes.String(), new JsonSerde<>()));

    intelligentRouting
        .get("intelligent-routing-ai-approved")
        .peek(
            (key, decision) ->
                logger.debug(
                    "AI APPROVED: {} (confidence: {:.1f}%)",
                    decision.transactionId(), decision.confidenceScore() * 100))
        .mapValues(this::createApproval)
        .to("approved-transactions", Produced.with(Serdes.String(), new JsonSerde<>()));

    logger.info("Intelligent routing complete");

    // AI LEARNING LOOP: Feedback enhances future decisions
    KStream<String, Map<String, Object>> learningFeedback =
        builder.stream(
            "analyst-feedback", Consumed.with(Serdes.String(), new JsonSerde<>(Map.class)));

    learningFeedback.foreach(
        (key, feedback) ->
            logger.info("AI LEARNING: Processing Feedback for: {}", feedback.get("transactionId")));

    // Start the intelligent streaming application
    this.kafkaStreams = new KafkaStreams(builder.build(), getStreamProperties());

    kafkaStreams.setStateListener(((newState,oldState) ->
            logger.info("Intelligent Streams State changed from {} to {}", oldState, newState)));

    kafkaStreams.start();
    logger.info("Intelligent Fraud Detection streaming started");
  }

    /**
     * 🔧 Kafka Streams properties optimized for intelligent processing
     */
    private Properties getStreamProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "intelligent-fraud-detection");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class);

        // Optimized for AI workloads
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 5 * 1024 * 1024); // 5MB

        return props;
    }

  private CustomerProfile getCustomerProfile(String customerId) {
    return null;
  }

  private Long getVelocityContext(String customerId) {
    return 0L;
  }

  private String buildContextSummary(Long velocity, CustomerProfile profile, Transaction txn) {
    StringBuilder summary = new StringBuilder("Streaming Context: ");

    if (velocity != null && velocity > 1) {
      summary.append(String.format("%d recent txns, ", velocity));
    }

    if (profile != null) {
      summary.append(String.format("$%.0f avg, ", profile.averageTransactionAmount()));
      summary.append(String.format("%s risk customer", profile.riskLevel()));
    }

    return summary.toString();
  }

  // Helper methods for creating business outputs
  private Map<String, Object> createFraudAlert(FraudDecision decision) {
    return Map.of(
        "type", "AI_FRAUD_ALERT",
        "transactionId", decision.transactionId(),
        "confidence", Math.round(decision.confidenceScore() * 100),
        "reason", decision.primaryReason(),
        "agentCount", decision.agentInsights().size(),
        "aiExplanation", decision.detailedExplanation(),
        "timestamp", System.currentTimeMillis(),
        "priority", decision.isHighConfidence() ? "HIGH" : "MEDIUM");
  }

  private Map<String, Object> createReviewCase(FraudDecision decision) {
    return Map.of(
        "type", "AI_REVIEW_CASE",
        "transactionId", decision.transactionId(),
        "confidence", Math.round(decision.confidenceScore() * 100),
        "explanation", decision.detailedExplanation(),
        "agentInsights", decision.agentInsights(),
        "status", "PENDING_HUMAN_REVIEW",
        "timestamp", System.currentTimeMillis());
  }

  private Map<String, Object> createApproval(FraudDecision decision) {
    return Map.of(
        "type", "AI_APPROVAL",
        "transactionId", decision.transactionId(),
        "confidence", Math.round(decision.confidenceScore() * 100),
        "status", "APPROVED_BY_AI",
        "agentCount", decision.agentInsights().size(),
        "timestamp", System.currentTimeMillis());
  }

    @PreDestroy
    public void stopIntelligentStreaming() {
        if (kafkaStreams != null) {
            logger.info("Stopping Intelligent Fraud Detection Streams...");
            kafkaStreams.close();
            logger.info("Intelligent streaming stopped");
        }
    }
}
