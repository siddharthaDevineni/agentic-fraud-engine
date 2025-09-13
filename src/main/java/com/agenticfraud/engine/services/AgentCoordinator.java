package com.agenticfraud.engine.services;

import com.agenticfraud.engine.agents.*;
import com.agenticfraud.engine.models.AgentInsight;
import com.agenticfraud.engine.models.FraudDecision;
import com.agenticfraud.engine.models.Transaction;
import com.agenticfraud.engine.utils.AgenticFraudUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class AgentCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(AgentCoordinator.class);

    // Thread pool for parallel agent execution
    private final ExecutorService agentExecutor = Executors.newFixedThreadPool(5);

    // 5 AI fraud investigators
    private final BehaviorAnalyst behaviorAnalyst;
    private final PatternDetector patternDetector;
    private final RiskAssessor riskAssessor;
    private final GeographicAnalyst geographicAnalyst;
    private final TemporalAnalyst temporalAnalyst;
    private final ChatModel chatModel;

    public AgentCoordinator(
            BehaviorAnalyst behaviorAnalyst,
            PatternDetector patternDetector,
            RiskAssessor riskAssessor,
            GeographicAnalyst geographicAnalyst,
            TemporalAnalyst temporalAnalyst,
            ChatModel chatModel) {

        this.behaviorAnalyst = behaviorAnalyst;
        this.patternDetector = patternDetector;
        this.riskAssessor = riskAssessor;
        this.geographicAnalyst = geographicAnalyst;
        this.temporalAnalyst = temporalAnalyst;
        this.chatModel = chatModel;

        logger.info("Agent Coordinator initialized with 5 fraud investigators");
    }

    /**
     * Main method to coordinate complete fraud analysis
     *
     * @param transaction trx
     * @return FraudDecision
     */
    public FraudDecision investigateTransaction(Transaction transaction) {
        logger.info("Starting multi-agent investigation for Trx: {}", transaction.transactionId());

        long startTime = System.currentTimeMillis();

        try {
            // Phase 1: Parallel Individual Analysis (all agents analyze simultaneously)
            List<AgentInsight> individualInsights = conductParallelAnalysis(transaction);

            // Phase 2: Agent collaboration (agents debate and discuss findings)
            List<AgentInsight> collaborativeInsights = facilitateAgentCollaboration(transaction, individualInsights);

            // Phase 3: Consensus Decision (synthesize all insights into final decision)
            FraudDecision finalDecision = synthesizeFinalDecision(transaction, individualInsights,
                    collaborativeInsights);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Investigation completed in {}ms: {} (confidence: {})",
                    duration, finalDecision.isFraudulent() ? "FRAUD DETECTED" : "LEGITIMATE",
                    finalDecision.confidenceScore());

            return finalDecision;
        } catch (Exception e) {
            logger.error("Error during investigation: {}", e.getMessage(), e);
            return createErrorDecision(transaction, e);
        }
    }

    /**
     * Phase 1: All agents analyze the transaction in parallel
     * This speeds up analysis while each agent focuses on their specialty
     *
     * @param transaction trx
     * @return List<AgentInsight>
     */
    private List<AgentInsight> conductParallelAnalysis(Transaction transaction) {

        logger.debug("Phase 1: Launching parallel agent analysis");

        // Create async tasks for each agent
        List<CompletableFuture<AgentInsight>> agentTasks = List.of(
                CompletableFuture.supplyAsync(() -> {
                    logger.debug("BehaviorAnalyst starting analysis");
                    return behaviorAnalyst.analyze(transaction);
                }, agentExecutor),

                CompletableFuture.supplyAsync(() -> {
                    logger.debug("PatternDetector starting analysis");
                    return patternDetector.analyze(transaction);
                }, agentExecutor),

                CompletableFuture.supplyAsync(() -> {
                    logger.debug("RiskAssessor starting analysis");
                    return riskAssessor.analyze(transaction);
                }, agentExecutor),

                CompletableFuture.supplyAsync(() -> {
                    logger.debug("GeographicAnalyst starting analysis");
                    return geographicAnalyst.analyze(transaction);
                }, agentExecutor),

                CompletableFuture.supplyAsync(() -> {
                    logger.debug("TemporalAnalyst starting analysis");
                    return temporalAnalyst.analyze(transaction);
                }, agentExecutor)
        );

        // wait for all agents to complete and collect results
        List<AgentInsight> insights = agentTasks.stream()
                .map(CompletableFuture::join)
                .toList();

        logger.info("Phase 1 Complete: {} agents provided insights", insights.size());
        logAgentSummary(insights);

        return insights;

    }

    /**
     * Agents collaborate and debate their findings
     * This is where agents challenge each other and refine their analysis
     */
    private List<AgentInsight> facilitateAgentCollaboration(Transaction transaction,
                                                            List<AgentInsight> individualInsights) {

        logger.debug("Phase 2: Facilitating agent collaboration");

        List<AgentInsight> collaborativeInsights = new ArrayList<>();

        // Check if there's a disagreement between agents that warrants discussion
        if (requiresCollaboration(individualInsights)) {
            logger.info("Agents have conflicting views - initiating collaboration");

            // High-risk agents challenge low-risk agents
            collaborativeInsights.addAll(facilitateHighRiskChallenge(transaction, individualInsights));

            // Geographic and temporal agents cross-validate
            collaborativeInsights.addAll(facilitateCrossValidation(transaction, individualInsights));

            // Final consensus building
            collaborativeInsights.add(buildAgentConsensus(transaction, individualInsights));

        } else {
            logger.info("Agents are in agreement - minimal collaboration needed");

            // Still do lightweight collaboration for confirmation
            collaborativeInsights.add(buildAgentConsensus(transaction, individualInsights));
        }

        return collaborativeInsights;
    }

    private boolean requiresCollaboration(List<AgentInsight> insights) {
        if (insights.size() < 2) return false;

        double maxRisk = insights.stream().mapToDouble(AgentInsight::riskScore).max().orElse(0);
        double minRisk = insights.stream().mapToDouble(AgentInsight::riskScore).min().orElse(0);

        // If risk scores vary by more than 0.4, agents should collaborate
        return (maxRisk - minRisk) > 0.4;
    }

    /**
     * High-risk agents challenge low-risk agents
     * If some agents see high risk but others don't, they debate
     *
     * @param transaction Trx
     * @param insights    List of AgentsInsight
     * @return List of AgentInsight
     */
    private List<AgentInsight> facilitateHighRiskChallenge(Transaction transaction, List<AgentInsight> insights) {

        List<AgentInsight> challengeInsights = new ArrayList<>();

        // Find agents with high-risk scores (>=0.7)
        List<AgentInsight> highRiskAgents = insights
                .stream()
                .filter(insight -> insight.riskScore() >= 0.7)
                .toList();

        // Find agents with low-risk scores (<=0.4)
        List<AgentInsight> lowRiskAgents = insights.stream()
                .filter(agentInsight -> agentInsight.riskScore() <= 0.4)
                .toList();

        if (!highRiskAgents.isEmpty() && !lowRiskAgents.isEmpty()) {
            logger.info("High-risk agents challenging low-risk agents");

            // High-risk agents present their case
            for (AgentInsight highRisk : highRiskAgents) {
                String challenge = String.format(
                        "Agent %s found high risk (%.2f). What do you think about: %s",
                        highRisk.agentName(), highRisk.riskScore(), highRisk.reasoning()
                );

                // Ask low-risk agents to respond to the challenge
                for (AgentInsight lowRisk : lowRiskAgents) {
                    FraudAgent agent = getAgentByName(lowRisk.agentName());
                    if (agent != null) {
                        AgentInsight response = agent.collaborate(transaction, challenge);
                        challengeInsights.add(response);
                    }
                }
            }
        }

        return challengeInsights;
    }

    /**
     * Geographic and temporal agents cross-validate each other
     *
     * @param transaction trx
     * @param insights    list of AgentInsights
     * @return insight list
     */
    private List<AgentInsight> facilitateCrossValidation(Transaction transaction, List<AgentInsight> insights) {

        List<AgentInsight> validationInsights = new ArrayList<>();

        // Geographic analyst validates temporal findings
        AgentInsight geoInsight = findInsightByAgent(insights, "GEOGRAPHIC_ANALYST");
        AgentInsight timeInsight = findInsightByAgent(insights, "TEMPORAL_ANALYST");

        if (geoInsight != null && timeInsight != null) {
            String geoQuestion = String.format(
                    "Geographic analysis shows: %s. Does this align with the timing patterns you found?",
                    geoInsight.reasoning()
            );

            AgentInsight temporalResponse = temporalAnalyst.collaborate(transaction, geoQuestion);
            validationInsights.add(temporalResponse);

            String timeQuestion = String.format(
                    "Temporal analysis shows: %s. Does this make geographic sense?",
                    timeInsight.reasoning()
            );

            AgentInsight geoResponse = geographicAnalyst.collaborate(transaction, timeQuestion);
            validationInsights.add(geoResponse);
        }

        return validationInsights;
    }

    /**
     * Build final consensus among all agents
     *
     * @param transaction try
     * @param insights    list of AgentInsight
     * @return insights
     */
    private AgentInsight buildAgentConsensus(Transaction transaction, List<AgentInsight> insights) {

        // Create summary of all agent findings
        String agentSummary = insights.stream()
                .map(insight -> String.format("%s (Risk: %.2f): %s",
                        insight.agentName(),
                        insight.riskScore(),
                        insight.reasoning()))
                .collect(Collectors.joining("\n"));

        // Ask AI to synthesize consensus
        String consensusPrompt = String.format("""
                        You are the lead fraud investigator reviewing findings from your team of 5 specialists.
                        
                        Transaction: %s
                        
                        Agent Findings:
                        %s
                        
                        Based on all agent analyses, provide a final consensus:
                        - Do the agents generally agree or disagree?
                        - What's the overall fraud risk?
                        - What are the key factors driving the decision?
                        
                        Format:
                        RISK_SCORE: [0.0-1.0]
                        REASONING: [Consensus analysis]
                        RECOMMENDATION: [Final action]
                        """, transaction.toAnalysisText(),
                agentSummary);

        try {
            String consensusResponse = chatModel.call(consensusPrompt);

            return AgentInsight.create(
                    "Consensus Building",
                    "CONSENSUS_ORCHESTRATOR",
                    consensusResponse,
                    AgenticFraudUtils.extractRiskScore(consensusResponse),
                    AgenticFraudUtils.extractReasoning(consensusResponse),
                    AgenticFraudUtils.extractRecommendation(consensusResponse)
            );
        } catch (Exception e) {
            logger.error("Error building consensus: {}", e.getMessage());
            return AgentInsight.create(
                    "Consensus Building",
                    "CONSENSUS_ORCHESTRATOR",
                    "Error buidling consensus:" + e.getMessage(),
                    0.5,
                    "Technical error occured during consensus building",
                    "Manual review required"
            );
        }
    }

    // Phase 3: Synthesize all insights into final fraud decision
    private FraudDecision synthesizeFinalDecision(Transaction transaction,
                                                  List<AgentInsight> individualInsights,
                                                  List<AgentInsight> collaborativeInsights) {
        logger.debug("Phase 3: Synthesizing final decision");

        // Combine all insights
        List<AgentInsight> allInsights = new ArrayList<>();
        allInsights.addAll(individualInsights);
        allInsights.addAll(collaborativeInsights);

        // Calculate weighted risk score
        double finalRiskScore = calculateWeightedRiskScore(allInsights);

        // Determine fraud decision
        boolean isFraudulent = finalRiskScore >= 0.6; // Threshold for fraud

        // Calculate confidence based on agent agreement
        double confidence = calculateConfidence(allInsights, isFraudulent);

        // Generate a human-readable explanation
        String explanation = generateExplanation(transaction, allInsights, finalRiskScore);

        // Create a final decision
        if (isFraudulent) {
            return FraudDecision.fraudulent(
                    transaction.transactionId(),
                    confidence,
                    "Multi-agent consensus indicates fraud",
                    explanation,
                    allInsights
            );
        } else {
            return FraudDecision.legitimate(
                    transaction.transactionId(),
                    confidence,
                    allInsights
            );
        }

    }

    private String generateExplanation(Transaction transaction, List<AgentInsight> insights,
                                       double finalRiskScore) {
        String prompt = String.format("""
                        Generate a clear explanation for this fraud decision that a bank analyst can understand.
                        
                        Transaction: %s
                        Final Risk Score: %.2f
                        
                        Agent Analysis Summary:
                        %s
                        
                        Write a concise explanation covering:
                        1. What the agents found
                        2. Why this led to the decision
                        3. Key risk factors
                        4. Recommended next steps
                        
                        Keep it professional but accessible.
                        """,
                transaction.toAnalysisText(),
                finalRiskScore,
                insights.stream()
                        .map(i -> String.format("- %s: %s", i.agentName(), i.reasoning()))
                        .collect(Collectors.joining("\n"))
        );

        try {
            return chatModel.call(prompt);
        } catch (Exception e) {
            return "Multiple AI agents analyzed this transaction. Risk score: " + finalRiskScore;
        }

    }

    private double calculateConfidence(List<AgentInsight> insights, boolean isFraudulent) {
        // Calculate confidence based on how much agents agree
        long agreeingAgents = insights.stream()
                .mapToLong(insight -> (insight.indicatesFraud() == isFraudulent) ? 1 : 0)
                .sum();

        double agreementRatio = (double) agreeingAgents / insights.size();

        // High agreement = high confidence
        if (agreementRatio >= 0.8) return 0.9;
        if (agreementRatio >= 0.6) return 0.7;
        if (agreementRatio >= 0.4) return 0.5;
        return 0.3;
    }

    private double calculateWeightedRiskScore(List<AgentInsight> insights) {
        // weight different agents based on their specialization relevance
        double totalScore = 0;
        double totalWeight = 0;

        for (AgentInsight insight : insights) {
            double weight = getAgentWeight(insight.agentName());
            totalScore += insight.riskScore() * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? totalScore / totalWeight : 0.5;
    }

    private double getAgentWeight(String agentName) {

        // Give different weights based on agent type
        return switch (agentName) {
            case "BEHAVIOR_ANALYST" -> 1.2; // High weight - behavior is key
            case "PATTERN_DETECTOR" -> 1.3; // Highest weight - patterns are critical
            case "RISK_ASSESSOR" -> 1.1; // Important for final decision
            case "GEOGRAPHIC_ANALYST" -> 1.0; // Standard weight
            case "TEMPORAL_ANALYST" -> 1.0; // Standard weight
            default -> 0.8; // Lower weight for consensus/collaboration insights
        };
    }

    private AgentInsight findInsightByAgent(List<AgentInsight> insights, String agentName) {
        return insights.stream()
                .filter(insight -> agentName.equals(insight.agentName()))
                .findFirst()
                .orElse(null);
    }

    private FraudAgent getAgentByName(String agentName) {
        return switch (agentName) {
            case "BEHAVIOUR_ANALYST" -> behaviorAnalyst;
            case "PATTERN_DETECTOR" -> patternDetector;
            case "RISK_ASSESSOR" -> riskAssessor;
            case "GEOGRAPHIC_ANALYST" -> geographicAnalyst;
            case "TEMPORAL_ANALYST" -> temporalAnalyst;
            default -> null;
        };
    }

    private FraudDecision createErrorDecision(Transaction transaction, Exception e) {
        return FraudDecision.fraudulent(
                transaction.transactionId(),
                0.5,
                "Technical error during analysis",
                "Error occurred: " + e.getMessage() + ". Manual review required.",
                List.of()
        );
    }

    private void logAgentSummary(List<AgentInsight> insights) {
        for (AgentInsight insight : insights) {
            logger.info("  {} → Risk: {:.2f}, Confidence: {:.2f}",
                    insight.agentName(), insight.riskScore(), insight.confidence());
        }
    }
}
