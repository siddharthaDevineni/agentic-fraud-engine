package com.agenticfraud.engine.streaming;

import com.agenticfraud.engine.models.CustomerProfile;
import com.agenticfraud.engine.models.FraudDecision;
import com.agenticfraud.engine.models.StreamingContext;
import com.agenticfraud.engine.models.Transaction;
import com.agenticfraud.engine.services.AgentCoordinator;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
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
    private KafkaStreams streams;

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
        KStream<String, Transaction> transactions = builder.stream("transactions",
                Consumed.with(Serdes.String(), transactionSerde));

        KTable<String, CustomerProfile> customerProfiles = builder.table("customerProfiles",
                Consumed.with(Serdes.String(), customerProfileSerde));

        // 1. Velocity context for AI agents, which provide velocity patterns to detect rapid-fire attacks
        KTable<String, Long> velocityContext = transactions
                .selectKey((key, txn) -> txn.customerId())
                .groupByKey(Grouped.with(Serdes.String(), transactionSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
                .count(Materialized.as("velocity-windows"))

                // Convert windowed table to regular table but first to KStream with each record as a Windowed key and value
                .toStream()
                // Windowed("John", [10:00-10:05]), Value=3 -> Key="John", Value=3
                .map((windowedKey, count) -> KeyValue.pair(windowedKey.key(), count))
                .groupByKey()
                .reduce((oldValue, newValue) -> newValue, // keep the latest count
                        Materialized.as("current-velocity"));

        KStream<String, Transaction> contextEnrichedTransactions = transactions
                .selectKey((key, txn) -> txn.customerId())
                // Add customer behavioral context for AI agents
                .leftJoin(customerProfiles,
                        (txn, profile) -> {
                            logger.info("Enriching transaction {} with profile {}", txn.transactionId(), profile);
                            return txn; // Transaction remains the same, we'll use profile in context
                        },
                        Joined.with(Serdes.String(), transactionSerde, customerProfileSerde))

                // Add velocity context for AI agents
                .leftJoin(velocityContext,
                        (txn, velocity) -> {
                            if (velocity != null && velocity > 3) {
                                logger.info("High velocity detected for customer {}: {} txns/5min", txn.customerId(), velocity);
                            }
                            return txn; // Transaction remains the same, we'll use velocity in context
                        },
                        Joined.with(Serdes.String(), transactionSerde, Serdes.Long()));

        KStream<String, FraudDecision> contextualDecisions =  contextEnrichedTransactions
                .mapValues(((readOnlyKey, txn) -> {
                    try {
                        // Build streaming context for AI agents
                        String customerId = txn.customerId();

                        // Get customer profile (from previous join)
                        CustomerProfile profile = getCustomerProfile(customerId);

                        // Get velocity context (from previous join)
                        Long velocity = getVelocityContext(customerId);

                        // Create AI-focused streaming context
                        StreamingContext context = new StreamingContext(
                                velocity, profile, buildContextSummary(velocity, profile, txn)
                        );

                        logger.info("Analyzing with streaming context for transaction {}: {}", txn.transactionId(), context.getAIContext());

                        // AI agents analyze transaction with streaming context
                        return agentCoordinator.investigateTransactionWithStreamingContext(txn, context);

                    } catch (Exception e) {
                        logger.error("Error in contextual analysis: {}", e.getMessage(), e);
                        return AgentCoordinator.createErrorDecision(txn, e);
                    }
                }));


        // Add customer behavioral context for AI agents
    }

    private CustomerProfile getCustomerProfile(String customerId) {
        return null;
    }

    private Long getVelocityContext(String customerId) {
        return 0L;
    }

    private String buildContextSummary(Long velocity, CustomerProfile profile, Transaction txn) {
        StringBuilder summary = new StringBuilder("Streaming Context: ");

        if(velocity != null && velocity > 1) {
            summary.append(String.format("%d recent txns, ", velocity));
        }

        if(profile != null) {
            summary.append(String.format("$%.0f avg, ", profile.averageTransactionAmount()));
            summary.append(String.format("%s risk customer", profile.riskLevel()));
        }

        return summary.toString();
    }
}
