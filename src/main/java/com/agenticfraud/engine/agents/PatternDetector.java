package com.agenticfraud.engine.agents;

import com.agenticfraud.engine.models.Transaction;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class PatternDetector extends AbstractFraudAgent {

    protected PatternDetector(ChatModel chatModel) {
        super(chatModel);
    }

    @Override
    protected String buildAnalysisPrompt(Transaction transaction) {
        return String.format("""
                You are an expert Fraud Pattern Detection specialist.
                
                Analyze this transaction for known fraud pattern:
                %s
                
                Look for these fraud indicators:
                1. Round number amounts (often used in testing stolen cards)
                2. Unusual merchant categories for the customer
                3. Geographic impossibility (rapid location changes)
                4. Time-based patterns (rapid successive transactions)
                5. Known fraud hotspot locations
                6. Merchant category switching patterns
                
                Cross-reference against known fraud attack vectors:
                - Card testing attacks
                - Account takeover patterns
                - Synthetic identity fraud
                - First-party fraud indicators
                
                Provide your analysis in this format:
                RISK_SCORE: [0.0-1.0]
                REASONING: [Specific patterns identified or ruled out]
                RECOMMENDATION: [Action based on pattern analysis]
                
                Be specific about which patterns you detect or explicitly rule out.
                """, transaction.toAnalysisText());
    }

    @Override
    protected void initializeKnowledge() {
        knowledgeBase.put("round_amounts", "100,200,500,1000");
        knowledgeBase.put("testing_amounts", "1,5,10,25");
        knowledgeBase.put("fraud_hotspots", "high_risk_zip_codes");
        knowledgeBase.put("velocity_thresholds", "rapid_succession_patterns");
    }

    @Override
    public String getSpecialization() {
        return "Fraud Pattern Detection";
    }

    @Override
    public String getAgentId() {
        return "PATTERN_DETECTOR";
    }
}
