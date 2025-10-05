package com.agenticfraud.engine.services;

import com.agenticfraud.engine.agents.*;
import com.agenticfraud.engine.models.*;
import com.agenticfraud.engine.utils.AgenticFraudUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

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

    logger.info("Intelligent Streaming Agent Coordinator initialized - 5 AI investigators ready");
  }

  /**
   * INTELLIGENT STREAMING ANALYSIS: The core method that combines Kafka streaming context with AI
   *
   * <p>Every transaction analysis uses real-time streaming context to make AI smarter. This method
   * is called from FraudDetectionController
   *
   * @param transaction trx
   * @param streamingContext streaming context
   * @return FraudDecision
   */
  public FraudDecision investigateTransaction(
      Transaction transaction, StreamingContext streamingContext) {

    logger.info(
        "Starting intelligent streaming investigation for {} with context: {}",
        transaction.transactionId(),
        streamingContext.getAIContext());

    long startTime = System.currentTimeMillis();

    try {
      // Phase 1: Streaming-Enhanced Parallel Analysis
      List<AgentInsight> streamingIntelligentInsights =
          conductStreamingIntelligentAnalysis(transaction, streamingContext);

      // Phase 2: Agent Collaboration (enhanced with streaming context)
      List<AgentInsight> collaborativeInsights =
          facilitateStreamingCollaboration(
              transaction, streamingContext, streamingIntelligentInsights);

      //  Phase 3: Intelligent Decision Synthesis
      FraudDecision finalDecision =
          synthesizeStreamingIntelligentDecision(
              transaction, streamingContext, streamingIntelligentInsights, collaborativeInsights);

      long duration = System.currentTimeMillis() - startTime;
      logger.info(
          "Intelligent streaming investigation completed in {}ms: {} (confidence: {}%)",
          duration,
          finalDecision.isFraudulent() ? "FRAUD DETECTED" : "LEGITIMATE",
          finalDecision.confidenceScore() * 100);

      return finalDecision;

    } catch (Exception e) {
      logger.error("Error in intelligent streaming investigation: {}", e.getMessage(), e);
      return createErrorDecision(transaction, e);
    }
  }

  /**
   * Phase 1: All agents analyze the transaction with streaming intelligence in parallel. This
   * speeds up analysis while each agent focuses on their specialty
   *
   * @param transaction trx
   * @param streamingContext streaming context
   * @return List<AgentInsight>
   */
  private List<AgentInsight> conductStreamingIntelligentAnalysis(
      Transaction transaction, StreamingContext streamingContext) {

    logger.debug("Phase 1: Launching streaming-intelligent parallel agent analysis");

    // Create async tasks for each agent - Enhanced agent prompts with streaming intelligence
    List<CompletableFuture<AgentInsight>> intelligentAgentTasks =
        List.of(
            // Behavior Analyst with Velocity Intelligence
            CompletableFuture.supplyAsync(
                () -> {
                  String velocityContext =
                      streamingContext.hasHighVelocity()
                          ? String.format(
                              "HIGH VELOCITY ALERT: %d transactions detected",
                              streamingContext.recentTransactionsCount())
                          : "Normal transaction velocity";

                  String contextPrompt =
                      String.format(
                          """
                            STREAMING INTELLIGENCE: %s

                            As a behavior analyst, analyze this transaction with real-time velocity context:
                            %s

                            Focus: How does the streaming velocity pattern affect behavioral risk assessment?
                            """,
                          velocityContext, transaction.toAnalysisText());
                  return behaviorAnalyst.collaborate(transaction, contextPrompt);
                },
                agentExecutor),

            // Pattern Detector with Attack Vector Intelligence
            CompletableFuture.supplyAsync(
                () -> {
                  String attackContext =
                      streamingContext.hasHighVelocity()
                          ? "RAPID-FIRE ATTACK PATTERN DETECTED - Analyze for card testing or credential stuffing"
                          : "Single transaction pattern - Analyze for standalone fraud indicators";

                  String velocityPrompt =
                      String.format(
                          """
                                    ATTACK VECTOR INTELLIGENCE: %s

                                    As a pattern detector, analyze attack patterns with streaming context:
                                    %s

                                    Focus: Does the velocity pattern match known automated attack vectors?
                                    """,
                          attackContext, transaction.toAnalysisText());
                  return patternDetector.collaborate(transaction, velocityPrompt);
                },
                agentExecutor),
            // Risk Assessor with Customer Profile Intelligence
            CompletableFuture.supplyAsync(
                () -> {
                  String profileContext =
                      streamingContext.customerProfile() != null
                          ? String.format(
                              "Customer baseline available: $%.0f avg, %s risk level",
                              streamingContext.customerProfile().averageTransactionAmount(),
                              streamingContext.customerProfile().riskLevel())
                          : "Limited customer profile data - using transaction-level analysis";

                  String profilePrompt =
                      String.format(
                          """
                                    CUSTOMER PROFILE INTELLIGENCE: %s

                                    As a risk assessor, analyze risk with streaming customer context:
                                    %s

                                    Focus: How does this transaction compare to real-time customer behavioral baseline?
                                    """,
                          profileContext, transaction.toAnalysisText());
                  return riskAssessor.collaborate(transaction, profilePrompt);
                },
                agentExecutor),
            // Geographic Analyst with Location Intelligence
            CompletableFuture.supplyAsync(
                () -> {
                  String locationPrompt =
                      String.format(
                          """
                                    STREAMING CONTEXT: %s

                                    As a geographic analyst, analyze location risk with streaming intelligence:
                                    %s

                                    Focus: Geographic risk factors enhanced by real-time context.
                                    """,
                          streamingContext.getAIContext(), transaction.toAnalysisText());
                  return geographicAnalyst.collaborate(transaction, locationPrompt);
                },
                agentExecutor),
            // Temporal Analyst with Timing Intelligence
            CompletableFuture.supplyAsync(
                () -> {
                  String timingPrompt =
                      String.format(
                          """
                                    TEMPORAL INTELLIGENCE: %s

                                    As a temporal analyst, analyze timing patterns with streaming context:
                                    %s

                                    Focus: How do timing patterns correlate with streaming velocity data?
                                    """,
                          streamingContext.hasHighVelocity()
                              ? "High-frequency timing detected"
                              : "Standard timing analysis",
                          transaction.toAnalysisText());
                  return temporalAnalyst.collaborate(transaction, timingPrompt);
                },
                agentExecutor));

    // wait for all agents to complete and collect results of streaming-intelligent insights
    List<AgentInsight> insights =
        intelligentAgentTasks.stream().map(CompletableFuture::join).toList();

    logger.info(
        "Phase 1 Completed: {} agents provided streaming-enhanced insights", insights.size());
    logAgentSummary(insights, streamingContext);

    return insights;
  }

  /**
   * Phase 2: Agent collaboration enhanced with streaming context. Agents collaborate and debate
   * their findings. This is where agents challenge each other and refine their analysis
   */
  private List<AgentInsight> facilitateStreamingCollaboration(
      Transaction transaction,
      StreamingContext streamingContext,
      List<AgentInsight> individualInsights) {

    logger.debug("Phase 2: Facilitating streaming-enhanced agent collaboration");

    List<AgentInsight> collaborativeInsights = new ArrayList<>();

    // Check if streaming context warrants additional collaboration
    if (requiresStreamingCollaboration(individualInsights, streamingContext)) {
      logger.info("Streaming context triggers enhanced agent collaboration");

      // High-velocity collaboration
      if (streamingContext.hasHighVelocity()) {
        collaborativeInsights.addAll(
            facilitateVelocityCollaboration(transaction, streamingContext, individualInsights));
      }

      // Customer profile collaboration
      if (streamingContext.customerProfile() != null) {
        collaborativeInsights.addAll(
            facilitateCustomerProfileCollaboration(
                transaction, streamingContext, individualInsights));
      }

      // Final streaming consensus
      collaborativeInsights.add(
          buildStreamingConsensus(transaction, streamingContext, individualInsights));

    } else {
      logger.info("Standard collaboration sufficient for this streaming context");
      collaborativeInsights.add(
          buildStreamingConsensus(transaction, streamingContext, individualInsights));
    }

    return collaborativeInsights;
  }

  // ================================
  //  INTELLIGENT DECISION SYNTHESIS
  // ================================

  /**
   * Phase 3: Intelligent Decision Synthesis Final decision synthesis using streaming intelligence
   *
   * @param transaction trx
   * @param context streaming context
   * @param collaborativeInsights list of individual AgentInsights
   * @param streamingIntelligentInsights list of collaborated AgentInsights
   */
  private FraudDecision synthesizeStreamingIntelligentDecision(
      Transaction transaction,
      StreamingContext context,
      List<AgentInsight> streamingIntelligentInsights,
      List<AgentInsight> collaborativeInsights) {

    // Combine all insights - streamingIntelligentInsights and collaborativeInsights
    List<AgentInsight> allInsights = new ArrayList<>();
    allInsights.addAll(streamingIntelligentInsights);
    allInsights.addAll(collaborativeInsights);

    // Enhanced decision synthesis using streaming intelligence
    double baseRiskScore = calculateWeightedRiskScore(allInsights);

    // Apply streaming intelligence multipliers
    double streamingIntelligenceBonus = calculateStreamingIntelligenceBonus(transaction, context);

    double finalRiskScore = Math.min(1.0, baseRiskScore + streamingIntelligenceBonus);
    boolean isFraudulent = finalRiskScore >= 0.6;
    double confidence = calculateStreamingConfidence(allInsights, isFraudulent, context);

    String explanation =
        generateStreamingIntelligentExplanation(transaction, context, allInsights, finalRiskScore);

    logger.info(
        "Streaming intelligence applied: Base={}, Bonus={}, Final={}",
        baseRiskScore,
        streamingIntelligenceBonus,
        finalRiskScore);

    return isFraudulent
        ? FraudDecision.fraudulent(
            transaction.transactionId(),
            confidence,
            "AI agents with streaming intelligence detected fraud",
            explanation,
            allInsights)
        : FraudDecision.legitimate(transaction.transactionId(), confidence, allInsights);
  }

  // ================================
  //  STREAMING INTELLIGENCE CALCULATIONS
  // ================================

  private double calculateStreamingIntelligenceBonus(
      Transaction transaction, StreamingContext context) {
    double bonus = 0.0;

    // Velocity intelligence bonus
    if (context.hasHighVelocity()) {
      bonus += 0.25; // High velocity significantly increases risk
      logger.debug("Velocity intelligence: +0.25 risk (high velocity detected)");
    }

    // Customer profile intelligence bonus
    if (context.customerProfile() != null) {
      CustomerProfile profile = context.customerProfile();

      if (profile.isAmountUnusual(transaction.amount())) {
        bonus += 0.20; // Unusual amount for customer
        logger.debug("Profile intelligence: +0.20 risk (unusual amount for customer)");
      }

      if ("HIGH".equals(profile.riskLevel())) {
        bonus += 0.10; // High-risk customer
        logger.debug("Profile intelligence: +0.10 risk (high-risk customer)");
      }
    }

    return bonus;
  }

  private double calculateStreamingConfidence(
      List<AgentInsight> insights, boolean isFraudulent, StreamingContext context) {
    double baseConfidence = calculateConfidence(insights, isFraudulent);

    // Streaming context increases confidence
    double contextBonus = 0.0;

    if (context.hasHighVelocity()) {
      contextBonus += 0.1; // High velocity increases confidence
    }

    if (context.customerProfile() != null) {
      contextBonus += 0.1; // Customer profile increases confidence
    }

    return Math.min(1.0, baseConfidence + contextBonus);
  }

  // ================================
  //  STREAMING EXPLANATION GENERATOR
  // ================================

  private String generateStreamingIntelligentExplanation(
      Transaction transaction,
      StreamingContext context,
      List<AgentInsight> insights,
      double riskScore) {

    StringBuilder explanation = new StringBuilder();
    explanation.append("AI AGENTS ENHANCED WITH STREAMING INTELLIGENCE\n\n");

    // Streaming context summary
    explanation.append("REAL-TIME STREAMING CONTEXT:\n");
    explanation.append("- ").append(context.getAIContext()).append("\n");
    if (context.hasHighVelocity()) {
      explanation.append("-  HIGH VELOCITY DETECTED: Rapid-fire transaction pattern\n");
    }
    if (context.customerProfile() != null) {
      explanation.append("-  CUSTOMER BASELINE: Behavioral profile available\n");
    }
    explanation.append("\n");

    // Agent analysis with streaming enhancement
    explanation.append("AI AGENT ANALYSIS (Enhanced with Streaming Data):\n");
    for (AgentInsight insight : insights) {
      explanation
          .append("- ")
          .append(insight.agentName())
          .append(" (Risk: ")
          .append(String.format("%.1f%%", insight.riskScore() * 100))
          .append("): ")
          .append(insight.reasoning())
          .append("\n");
    }

    // Final streaming-intelligent decision
    explanation.append("\n STREAMING-INTELLIGENT DECISION:\n");
    explanation
        .append("- Final Risk Score: ")
        .append(String.format("%.1f%%", riskScore * 100))
        .append("\n");
    explanation
        .append("- Decision: ")
        .append(riskScore >= 0.6 ? "FRAUD DETECTED" : "LEGITIMATE TRANSACTION")
        .append("\n");
    explanation.append(
        "- Intelligence Sources: Real-time velocity, customer profiles, temporal patterns");

    return explanation.toString();
  }

  // ================================
  // STREAMING-ENHANCED HELPER METHODS
  // ================================

  private boolean requiresStreamingCollaboration(
      List<AgentInsight> insights, StreamingContext context) {
    boolean hasDisagreement = requiresCollaboration(insights);
    boolean hasHighVelocity = context.hasHighVelocity();
    boolean hasCustomerProfile = context.customerProfile() != null;

    return hasDisagreement || hasHighVelocity || hasCustomerProfile;
  }

  private List<AgentInsight> facilitateVelocityCollaboration(
      Transaction transaction, StreamingContext context, List<AgentInsight> insights) {

    // Velocity-focused collaboration between Pattern Detector and Temporal Analyst
    List<AgentInsight> velocityInsights = new ArrayList<>();

    String velocityQuestion =
        String.format(
            "High velocity detected (%d transactions). Does this align with automated attack patterns?",
            context.recentTransactionsCount());

    AgentInsight patternResponse = patternDetector.collaborate(transaction, velocityQuestion);
    AgentInsight temporalResponse = temporalAnalyst.collaborate(transaction, velocityQuestion);

    velocityInsights.add(patternResponse);
    velocityInsights.add(temporalResponse);

    return velocityInsights;
  }

  private List<AgentInsight> facilitateCustomerProfileCollaboration(
      Transaction transaction, StreamingContext context, List<AgentInsight> insights) {

    // Customer profile collaboration between Behavior Analyst and Risk Assessor
    List<AgentInsight> profileInsights = new ArrayList<>();

    String profileQuestion =
        String.format(
            "Customer profile shows $%.0f average transactions, %s risk level. How does this affect your analysis?",
            context.customerProfile().averageTransactionAmount(),
            context.customerProfile().riskLevel());

    AgentInsight behaviorResponse = behaviorAnalyst.collaborate(transaction, profileQuestion);
    AgentInsight riskResponse = riskAssessor.collaborate(transaction, profileQuestion);

    profileInsights.add(behaviorResponse);
    profileInsights.add(riskResponse);

    return profileInsights;
  }

  private boolean requiresCollaboration(List<AgentInsight> insights) {
    if (insights.size() < 2) return false;

    double maxRisk = insights.stream().mapToDouble(AgentInsight::riskScore).max().orElse(0);
    double minRisk = insights.stream().mapToDouble(AgentInsight::riskScore).min().orElse(0);

    // If risk scores vary by more than 0.4, agents should collaborate
    return (maxRisk - minRisk) > 0.4;
  }

  /**
   * High-risk agents challenge low-risk agents If some agents see high risk but others don't, they
   * debate
   *
   * @param transaction Trx
   * @param insights List of AgentsInsight
   * @return List of AgentInsight
   */
  private List<AgentInsight> facilitateHighRiskChallenge(
      Transaction transaction, List<AgentInsight> insights) {

    List<AgentInsight> challengeInsights = new ArrayList<>();

    // Find agents with high-risk scores (>=0.7)
    List<AgentInsight> highRiskAgents =
        insights.stream().filter(insight -> insight.riskScore() >= 0.7).toList();

    // Find agents with low-risk scores (<=0.4)
    List<AgentInsight> lowRiskAgents =
        insights.stream().filter(agentInsight -> agentInsight.riskScore() <= 0.4).toList();

    if (!highRiskAgents.isEmpty() && !lowRiskAgents.isEmpty()) {
      logger.info("High-risk agents challenging low-risk agents");

      // High-risk agents present their case
      for (AgentInsight highRisk : highRiskAgents) {
        String challenge =
            String.format(
                "Agent %s found high risk (%.2f). What do you think about: %s",
                highRisk.agentName(), highRisk.riskScore(), highRisk.reasoning());

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
   * @param insights list of AgentInsights
   * @return insight list
   */
  private List<AgentInsight> facilitateCrossValidation(
      Transaction transaction, List<AgentInsight> insights) {

    List<AgentInsight> validationInsights = new ArrayList<>();

    // Geographic analyst validates temporal findings
    AgentInsight geoInsight = findInsightByAgent(insights, "GEOGRAPHIC_ANALYST");
    AgentInsight timeInsight = findInsightByAgent(insights, "TEMPORAL_ANALYST");

    if (geoInsight != null && timeInsight != null) {
      String geoQuestion =
          String.format(
              "Geographic analysis shows: %s. Does this align with the timing patterns you found?",
              geoInsight.reasoning());

      AgentInsight temporalResponse = temporalAnalyst.collaborate(transaction, geoQuestion);
      validationInsights.add(temporalResponse);

      String timeQuestion =
          String.format(
              "Temporal analysis shows: %s. Does this make geographic sense?",
              timeInsight.reasoning());

      AgentInsight geoResponse = geographicAnalyst.collaborate(transaction, timeQuestion);
      validationInsights.add(geoResponse);
    }

    return validationInsights;
  }

  /**
   * Build a final consensus among all agents using streaming intelligence and agent analyses
   *
   * @param transaction try
   * @param context streaming context
   * @param insights list of AgentInsight
   * @return insights
   */
  private AgentInsight buildStreamingConsensus(
      Transaction transaction, StreamingContext context, List<AgentInsight> insights) {

    // Create streaming-enhanced consensus
    String agentSummary =
        insights.stream()
            .map(
                insight ->
                    String.format(
                        "%s (Risk: %.2f): %s",
                        insight.agentName(), insight.riskScore(), insight.reasoning()))
            .collect(Collectors.joining("\n"));

    String consensusPrompt =
        String.format(
            """
              You are the lead fraud investigator with access to real-time streaming intelligence.

              STREAMING CONTEXT: %s

              Transaction: %s

              Agent Findings:
              %s

              Based on streaming intelligence and agent analyses, provide final consensus:
              - How does streaming context enhance the decision?
              - What's the overall fraud risk with streaming intelligence?
              - Key factors from both AI analysis and streaming data?

              Format:
              RISK_SCORE: [0.0-1.0]
              REASONING: [Streaming-enhanced consensus analysis]
              RECOMMENDATION: [Final action with streaming intelligence]
              """,
            context.getAIContext(), /*@formatter:off*/
            transaction.toAnalysisText(),
            /*@formatter:off*/
            agentSummary);

    try {
      String consensusResponse = chatModel.call(consensusPrompt);
      return AgentInsight.create(
          "Streaming Intelligence Consensus",
          "STREAMING_CONSENSUS_ORCHESTRATOR",
          consensusResponse,
          AgenticFraudUtils.extractRiskScore(consensusResponse),
          AgenticFraudUtils.extractReasoning(consensusResponse),
          AgenticFraudUtils.extractRecommendation(consensusResponse));
    } catch (Exception e) {
      logger.error("Error building streaming consensus: {}", e.getMessage());
      return AgentInsight.create(
          "Streaming Intelligence Consensus",
          "STREAMING_CONSENSUS_ORCHESTRATOR",
          "Error building streaming consensus: " + e.getMessage(),
          0.5,
          "Technical error occurred during streaming consensus building",
          "Manual review required");
    }
  }

  private double calculateConfidence(List<AgentInsight> insights, boolean isFraudulent) {
    // Calculate confidence based on how much agents agree
    long agreeingAgents =
        insights.stream()
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

    // Give different weights based on an agent type
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

  public static FraudDecision createErrorDecision(Transaction transaction, Exception e) {
    return FraudDecision.fraudulent(
        transaction.transactionId(),
        0.5,
        "Technical error during analysis",
        "Error occurred: " + e.getMessage() + ". Manual review required.",
        List.of());
  }

  private void logAgentSummary(List<AgentInsight> insights, StreamingContext streamingContext) {
    logger.info("Streaming Intelligence Summary:");
    logger.info("Context: {}", streamingContext.getAIContext());
    for (AgentInsight insight : insights) {
      logger.info(
          "{} → Risk: {}, Confidence: {}",
          insight.agentName(),
          insight.riskScore(),
          insight.confidence());
    }
  }
}
