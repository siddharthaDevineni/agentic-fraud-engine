package com.agenticfraud.engine.agents;

import com.agenticfraud.engine.models.Transaction;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class RiskAssessor extends AbstractFraudAgent {

    protected RiskAssessor(ChatModel chatModel) {
        super(chatModel);
    }

    @Override
    protected String buildAnalysisPrompt(Transaction transaction) {
        return String.format("""
                You are an expert Financial Risk Assessor for fraud prevention.
                
                Evaluate the overall fraud risk for this transaction:
                %s
                
                Calculate risk based on:
                1. Transaction amount relative to account limits and history
                2. Merchant risk profile and category
                3. Geographic risk factors
                4. Time-based risk (off-hours, holidays)
                5. Channel risk (online vs. in-person)
                6. Currency and cross-border factors
                
                Consider both financial impact and probability:
                - High amount + high probability = critical risk
                - Low amount + high probability = moderate risk
                - High amount + low probability = monitoring needed
                
                Provide your assessment in this format:
                RISK_SCORE: [0.0-1.0]
                REASONING: [Comprehensive risk calculation methodology]
                RECOMMENDATION: [Risk mitigation action - approve, decline or additional verification]
                
                Provide specific risk factors and their weights in your analysis.
                """, transaction.toAnalysisText());
    }

    @Override
    protected void initializeKnowledge() {
        knowledgeBase.put("high_risk_merchants", "gambling,crypto,cash_advance");
        knowledgeBase.put("risk_multipliers", "international=1.5,off_hours=1.2");
        knowledgeBase.put("amount_thresholds", "low=100,medium=1000,high=5000");
        knowledgeBase.put("geographic_risk", "country_risk_scores");
    }

    @Override
    public String getSpecialization() {
        return "Financial Risk Assessment";
    }

    @Override
    public String getAgentId() {
        return "RISK_ASSESSOR";
    }
}
