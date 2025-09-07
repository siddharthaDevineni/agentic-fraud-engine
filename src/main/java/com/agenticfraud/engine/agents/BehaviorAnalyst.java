package com.agenticfraud.engine.agents;

import com.agenticfraud.engine.models.Transaction;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class BehaviorAnalyst extends AbstractFraudAgent {

    public BehaviorAnalyst(ChatModel chatModel) {
        super(chatModel);
    }

    @Override
    protected String buildAnalysisPrompt(Transaction transaction) {
        return String.format("""
                You are an expert Customer Behavior Analyst specializing in fraud detection.
                
                Analyze this transaction for behavior anomalies:
                %s
                
                Focus on:
                1. Transaction timing patterns (unusual hours, frequency)
                2. Amount deviations from typical spending
                3. Location/merchant consistency with past behavior
                4. Payment method and channel analysis
                
                Consider normal variations vs. suspicious deviations.
                
                Provide your analysis in this format:
                RISK_SCORE: [0.0-1.0]
                REASONING: [Your detailed behavioral analysis]
                RECOMMENDATION: [Specific action based on behavioral patterns]
                
                Be thorough but concise. Focus on behavioral red flags or normal patterns.
                """, transaction.toAnalysisText());
    }

    @Override
    protected void initializeKnowledge() {
        knowledgeBase.put("normal_transaction_hours", "6-23");
        knowledgeBase.put("suspicious_frequency_threshold", "5_per_hour");
        knowledgeBase.put("amount_deviation_threshold", "3x_average");
        knowledgeBase.put("location_change_threshold", "500_miles");
    }

    @Override
    public String getSpecialization() {
        return "Customer Behavior Analysis";
    }

    @Override
    public String getAgentId() {
        return "BEHAVIOR_ANALYST";
    }
}
