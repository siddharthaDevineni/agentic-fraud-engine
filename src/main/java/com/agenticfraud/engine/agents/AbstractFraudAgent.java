package com.agenticfraud.engine.agents;

import com.agenticfraud.engine.models.AgentInsight;
import com.agenticfraud.engine.models.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractFraudAgent implements FraudAgent {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final ChatModel chatModel;
    protected final Map<String, Object> knowledgeBase = new ConcurrentHashMap<>();

    protected AbstractFraudAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
        initializeKnowledge();
    }

    @Override
    public AgentInsight analyze(Transaction transaction) {
        try {
            logger.debug("{} analyzing transaction : {}", getAgentId(), transaction.transactionId());

            String prompt = buildAnalysisPrompt(transaction);
            String analysis = chatModel.call(prompt);

            double riskScore = extractRiskScore(analysis);
            String reasoning = extractReasoning(analysis);
            String recommendation = extractRecommendation(analysis);

            AgentInsight insight = AgentInsight.create(
                    getSpecialization(),
                    getAgentId(),
                    analysis,
                    riskScore,
                    reasoning,
                    recommendation
            );

            logger.info("{} completed analysis for {}: Risk={}, Confidence={}", getAgentId(),
                    transaction.transactionId(), riskScore, insight.confidence());

            return insight;

        } catch (Exception e) {
            logger.error("Error during analysis by {}: {}", getAgentId(), e.getMessage());
            return createErrorInsight(transaction, e);
        }
    }

    @Override
    public AgentInsight collaborate(Transaction transaction, String question, Object... context) {
        try {
            String collaborationPrompt = buildCollaborationPrompt(transaction, question, context);
            String response = chatModel.call(collaborationPrompt);

            double riskScore = extractRiskScore(response);
            String reasoning = extractReasoning(response);

            return AgentInsight.create(
                    getSpecialization(),
                    getAgentId() + "-collaboration",
                    response,
                    riskScore,
                    reasoning,
                    "Collaboration response: " + response.substring(0, Math.min(100, response.length()))
            );
        } catch (Exception e) {
            logger.error("Error during collaboration by {}: {}", getAgentId(), e.getMessage());
            return createErrorInsight(transaction, e);
        }
    }

    @Override
    public void updateKnowledge(String transactionId, boolean actualFraud, String feedback) {
        // Store learning for future improvements
        Map<String, Object> learningData = Map.of(
                "transactionId", transactionId,
                "actualFraud", actualFraud,
                "feedback", feedback,
                "timestamp", System.currentTimeMillis()
        );

        knowledgeBase.put("learning_" + transactionId, learningData);

        logger.info("{} updated knowledge for transaction {}: actualFraud={}", getAgentId(), transactionId, actualFraud);
    }

    // Abstract methods for specialization
    protected abstract String buildAnalysisPrompt(Transaction transaction);

    protected abstract void initializeKnowledge();

    // Helper methods
    protected String buildCollaborationPrompt(Transaction transaction, String question, Object... context) {
        return String.format("""
                        You are a %s fraud detection specialist.
                        Another agent is asking: %s
                        
                        Transaction details: %s
                        Additional context: %s
                        
                        Provide your expert opinion with a risk score (0.0 to 1.0) and reasoning.
                        Format your response as:
                        RISK_SCORE: [0.0-1.0]
                        REASONING: [Your detailed analysis]
                        RECOMMENDATION: [What action to take]
                        """, getSpecialization(), question, transaction.toAnalysisText(),
                context.length > 0 ? String.valueOf(context[0]) : "None");
    }

    protected double extractRiskScore(String analysis) {
        try {
            // Look for RISK_SCORE: pattern
            if (analysis.contains("RISK_SCORE:")) {
                String scorePart = analysis.substring(analysis.indexOf("RISK_SCORE:") + 11);
                String scoreStr = scorePart.split("\\n")[0].trim();
                return Double.parseDouble(scoreStr);
            }

            // Fallback: analyze sentiment and keywords for risk estimation
            String lower = analysis.toLowerCase();
            if (lower.contains("high risk") || lower.contains("fraudulent") || lower.contains("suspicious")) {
                return 0.8;
            } else if (lower.contains("medium risk") || lower.contains("unusual") || lower.contains("concerning")) {
                return 0.6;
            } else if (lower.contains("low risk") || lower.contains("normal") || lower.contains("legitimate")) {
                return 0.2;
            }

            return 0.5; // Default moderate risk
        } catch (Exception e) {
            logger.warn("Could not extract risk score from analysis: {}", e.getMessage());
            return 0.5;
        }
    }

    protected String extractReasoning(String analysis) {
        if (analysis.contains("REASONING:")) {
            String reasoningPart = analysis.substring(analysis.indexOf("REASONING:") + 10);
            return reasoningPart.split("RECOMMENDATION:")[0].trim();
        }
        return analysis.length() > 200 ? analysis.substring(0, 200) + "..." : analysis;
    }

    protected String extractRecommendation(String analysis) {
        if (analysis.contains("RECOMMENDATION:")) {
            return analysis.substring(analysis.indexOf("RECOMMENDATION:") + 15).trim();
        }
        return "Standard fraud review recommended";
    }

    private AgentInsight createErrorInsight(Transaction transaction, Exception e) {
        return AgentInsight.create(
                getSpecialization(),
                getAgentId() + "-error",
                "Analysis failed: " + e.getMessage(),
                0.5, // Neutral score on error
                "Error occurred during analysis: " + e.getMessage(),
                "Manual review required due to analysis error"
        );
    }
}
