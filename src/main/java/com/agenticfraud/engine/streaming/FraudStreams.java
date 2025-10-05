package com.agenticfraud.engine.streaming;

import com.agenticfraud.engine.models.*;
import com.agenticfraud.engine.services.AgentCoordinator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
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

    logger.info("Starting Intelligent Fraud Detection Streaming...");

    StreamsBuilder builder = new StreamsBuilder();

    // Configure JSON serdes
    JsonSerde<Transaction> transactionSerde = new JsonSerde<>(Transaction.class);
    JsonSerde<CustomerProfile> customerProfileSerde = new JsonSerde<>(CustomerProfile.class);
    JsonSerde<FraudDecision> decisionJsonSerde = new JsonSerde<>(FraudDecision.class);

    // ================================
    // INPUT STREAMS
    // ================================
    KStream<String, Transaction> transactions =
        builder.stream("transactions", Consumed.with(Serdes.String(), transactionSerde));

    KTable<String, CustomerProfile> customerProfiles =
        builder.table("customerProfiles", Consumed.with(Serdes.String(), customerProfileSerde));

    // 1. Velocity context for AI agents, which provide velocity patterns to detect rapid-fire
    // attacks - calculate transaction velocity (count in 5-minute windows)
    KTable<String, Long> velocityContext =
        transactions
            .selectKey((key, txn) -> txn.customerId())
            .groupByKey(Grouped.with(Serdes.String(), transactionSerde))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
            .count(Materialized.as("velocity-windows"))

            // Convert windowed table to regular table but first to KStream with each record as a

            // KStream<Windowed<String>, Long> to a KStream<String, Long> value
            .toStream()

            // Windowed("CUST-001", [10:00-10:05]), Value=3 -> Key="CUST-001", Value=3
            .map((windowedKey, count) -> KeyValue.pair(windowedKey.key(), count))

            // Group by customer ID with explicit serdes for Long
            .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))

            // Reduce to keep the latest count with explicit serdes
            .reduce(
                (oldValue, newValue) -> newValue, // keep the latest count
                Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("current-velocity")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(Serdes.Long()));

      logger.info("Velocity intelligence configured");

    // ================================
    // STREAMING CONTEXT ENRICHMENT
    // ================================
    KStream<String, EnrichedTransaction> contextEnrichedTransactions =
        transactions
            .selectKey((key, txn) -> txn.customerId())
            // Join with customer profiles to enrich transaction data
            .leftJoin(
                customerProfiles,
                (txn, profile) -> {
                  logger.info(
                      "Enriching transaction {} with profile {}",
                      txn.transactionId(),
                      profile != null ? profile.customerId() : "NO PROFILE");
                  // Create enriched object with profile, but no velocity yet
                  return new EnrichedTransaction(txn, profile, null);
                },
                Joined.with(Serdes.String(), transactionSerde, customerProfileSerde))

            // Join with velocity context for AI agents to provide velocity patterns
            .leftJoin(
                velocityContext,
                (enriched, velocity) -> {
                  if (velocity != null && velocity > 3) {
                    logger.warn(
                        "High velocity detected for customer {}: {} txns/5min",
                        enriched.transaction(),
                        velocity);
                  }
                  return new EnrichedTransaction(
                      enriched.transaction(), enriched.customerProfile(), velocity);
                },
                Joined.with(
                    Serdes.String(), new JsonSerde<>(EnrichedTransaction.class), Serdes.Long()));

    // ================================
    // STREAMING-INTELLIGENT ANALYSIS
    // ================================

    KStream<String, FraudDecision> streamingIntelligentDecisions =
        contextEnrichedTransactions.mapValues(
            ((readOnlyKey, enriched) -> {
              try {
                Transaction txn = enriched.transaction();

                // Build streaming context from enriched data
                StreamingContext context = enriched.toStreamingContext();

                logger.info(
                    "Analyzing with streaming context for transaction: {}: {}",
                    enriched.transaction(),
                    context.getAIContext());

                // Log streaming context details
                if (enriched.velocityCount() != null && enriched.velocityCount() > 1) {
                  logger.info(
                      "Velocity: {} transactions in last 5 minutes", enriched.velocityCount());
                }

                if (enriched.customerProfile() != null) {
                  logger.info(
                      String.format(
                          "Customer Profile: $%.0f avg, %s risk, %s",
                          enriched.customerProfile().averageTransactionAmount(),
                          enriched.customerProfile().riskLevel(),
                          enriched.customerProfile().isAmountUnusual(txn.amount())
                              ? "UNUSUAL AMOUNT"
                              : "normal amount"));
                }

                // AI agents analyze transaction with streaming intelligence via context
                logger.info(
                    "Invoking AI-enhanced streaming intelligence context for transaction: {}", txn);
                return agentCoordinator.investigateTransaction(txn, context);

              } catch (Exception e) {
                logger.error("Error in contextual analysis: {}", e.getMessage(), e);
                return AgentCoordinator.createErrorDecision(enriched.transaction(), e);
              }
            }));

    logger.info("AI-enhanced streaming context created");

    // Intelligent Routing: AI-driven decision routing
    Map<String, KStream<String, FraudDecision>> intelligentRouting =
        streamingIntelligentDecisions
            .split()

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

    logger.info("Routing map keys: {}", intelligentRouting.keySet());

    intelligentRouting
        .keySet()
        .forEach(
            (key) -> {
              logger.info("Routing key: {}", key);
              intelligentRouting
                  .get(key)
                  .peek((k, v) -> logger.info("Key: {}, Decision: {}", k, v));
            });

    String fraudKey =
        intelligentRouting.keySet().stream()
            .filter(k -> k.contains("ai-fraud-alert"))
            .findFirst()
            .orElseThrow();

    String reviewKey =
        intelligentRouting.keySet().stream()
            .filter(k -> k.contains("ai-review-needed"))
            .findFirst()
            .orElseThrow();

    String approvedKey =
        intelligentRouting.keySet().stream()
            .filter(k -> k.contains("ai-approved"))
            .findFirst()
            .orElseThrow();

    // Route to appropriate output topics
    intelligentRouting
        .get(fraudKey)
        .peek(
            (key, decision) ->
                logger.warn(
                    "AI FRAUD ALERT for transaction: {} Confidence: {}% - agents: {}",
                    decision.transactionId(),
                    decision.confidenceScore() * 100,
                    decision.agentInsights().size()))
        .mapValues(this::createFraudAlert)
        .to("fraud-alerts", Produced.with(Serdes.String(), new JsonSerde<>()));

    intelligentRouting
        .get(reviewKey)
        .peek(
            (key, decision) ->
                logger.info(
                    "AI REVIEW NEEDED: {} (confidence: {}%)",
                    decision.transactionId(), decision.confidenceScore() * 100))
        .mapValues(this::createReviewCase)
        .to("human-review", Produced.with(Serdes.String(), new JsonSerde<>()));

    intelligentRouting
        .get(approvedKey)
        .peek(
            (key, decision) ->
                logger.debug(
                    "AI APPROVED: {} (confidence: {}%)",
                    decision.transactionId(), decision.confidenceScore() * 100))
        .mapValues(this::createApproval)
        .to("approved-transactions", Produced.with(Serdes.String(), new JsonSerde<>()));

    logger.info("Intelligent routing complete");

    // ================================
    // AI LEARNING LOOP
    // ================================
    KStream<String, Map<String, Object>> learningFeedback =
        builder.stream(
            "analyst-feedback", Consumed.with(Serdes.String(), new JsonSerde<>(Map.class)));

    learningFeedback.foreach(
        (key, feedback) ->
            logger.info(
                "AI LEARNING: Processing Feedback for transaction : {}",
                feedback.get("transactionId")));

    logger.info("AI learning loop configured");

    // Start the intelligent streaming application
    this.kafkaStreams = new KafkaStreams(builder.build(), getStreamProperties());

    kafkaStreams.setStateListener(
        ((newState, oldState) ->
            logger.info("Intelligent Streams State changed from {} to {}", oldState, newState)));

    kafkaStreams.start();
    logger.info("Intelligent Fraud Detection streaming started");
  }

  /** Kafka Streams properties optimized for intelligent processing */
  private Properties getStreamProperties() {
    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "intelligent-fraud-detection");
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class);

    // Optimized for AI workloads
    props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
    props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
    props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024); // 10MB

    return props;
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
