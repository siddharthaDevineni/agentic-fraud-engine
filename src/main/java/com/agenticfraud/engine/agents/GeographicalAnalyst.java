package com.agenticfraud.engine.agents;

import com.agenticfraud.engine.models.Transaction;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class GeographicalAnalyst extends AbstractFraudAgent {

    protected GeographicalAnalyst(ChatModel chatModel) {
        super(chatModel);
    }

    @Override
    protected String buildAnalysisPrompt(Transaction transaction) {
        return String.format("""
                You are an expert Geographic Risk Analyst for fraud detection.
                
                Analyze the geographic risk factors for this transaction:
                %s
                
                Evaluate:
                1. Location consistency with customer's typical patterns
                2. Geographic impossibility (physical travel time between transactions)
                3. High-risk geographic regions or areas
                4. Cross-border transaction risks
                5. Location spoofing indicators
                6. Regional fraud trends and hotspots
                
                Consider:
                - Travel patterns and feasibility
                - Regional crime statistics
                - Known fraud rings operating in the area
                - Geopolitical risk factors
                - Local merchant legitimacy
                
                Provide your analysis in this format:
                RISK_SCORE: [0.0-1.0]
                REASONING: [Geographic risk factors analysis]
                RECOMMENDATION: [Location-based fraud prevention action]
                
                Be specific about geographic inconsistencies or patterns you identify.
                """, transaction.toAnalysisText());
    }

    @Override
    protected void initializeKnowledge() {
        knowledgeBase.put("high_risk_regions", "known_fraud_hotspots");
        knowledgeBase.put("travel_speed_limits", "max_reasonable_travel_mph");
        knowledgeBase.put("cross_border_flags", "international_transaction_risks");
        knowledgeBase.put("location_spoofing", "vpn_proxy_indicators");
    }

    @Override
    public String getSpecialization() {
        return "Geographic Risk Analysis";
    }

    @Override
    public String getAgentId() {
        return "GEOGRAPHIC_ANALYST";
    }
}
