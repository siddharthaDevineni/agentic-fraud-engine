package com.agenticfraud.engine.agents;

import com.agenticfraud.engine.models.Transaction;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class TemporalAnalyst extends AbstractFraudAgent {

  protected TemporalAnalyst(ChatModel chatModel) {
    super(chatModel);
  }

  @Override
  protected String buildAnalysisPrompt(Transaction transaction) {
    return String.format(
        """
                You are an expert Temporal Pattern Analyst specializing in fraud detection.

                Analyze the timing patterns and temporal risks for this transaction:
                %s

                Examine:
                1. Transaction time vs. customer's typical active hours
                2. Day of week and seasonal patterns
                3. Velocity patterns (frequency and timing of recent transactions)
                4. Holiday and weekend behavior anomalies
                5. Time zone inconsistencies with location
                6. Rapid-fire transaction sequences indicating automation

                Consider temporal fraud indicators:
                - Off-hours activity (middle of night transactions)
                - Unnatural transaction timing (too precise, too rapid)
                - Time zone manipulation attempts
                - Batch processing patterns indicating bot activity

                Provide your analysis in this format:
                RISK_SCORE: [0.0-1.0]
                REASONING: [Temporal patten analysis and anomalies]
                RECOMMENDATION: [Time-based risk mitigations]

                Focus on timing inconsistencies or suspicious temporal patterns.
                """,
        transaction.toAnalysisText());
  }

  @Override
  protected void initializeKnowledge() {
    knowledgeBase.put("normal_hours", "6am-11pm_local_time");
    knowledgeBase.put("velocity_thresholds", "max_transactions_per_hour");
    knowledgeBase.put("automation_indicators", "sub_second_intervals");
    knowledgeBase.put("timezone_risks", "location_time_mismatches");
  }

  @Override
  public String getSpecialization() {
    return "Temporal Pattern Analysis";
  }

  @Override
  public String getAgentId() {
    return "TEMPORAL_ANALYST";
  }
}
