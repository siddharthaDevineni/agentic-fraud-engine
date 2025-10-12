package com.agenticfraud.engine.agents;

import com.agenticfraud.engine.models.AgentInsight;
import com.agenticfraud.engine.models.StreamingContext;
import com.agenticfraud.engine.models.Transaction;

public interface FraudAgent {

  /** Phase 1: Analyze transaction (simple analysis) */
  AgentInsight analyze(Transaction transaction);

  /** Phase 1: Analyze transaction with streaming intelligence */
  AgentInsight analyzeWithStreamingContext(Transaction transaction, StreamingContext context);

  /**
   * Get the agent's specialization area
   *
   * @return expertise
   */
  String getSpecialization();

  /**
   * Get the agent's unique identifier
   *
   * @return Id
   */
  String getAgentId();

  /** Phase 2: Collaborate with other agents on a decision */
  AgentInsight collaborate(Transaction transaction, String question, Object... context);

  /**
   * Update the agent's knowledge based on feedback
   *
   * @param transactionId Trx Id
   * @param actualFraud isFraud
   * @param feedback feedback
   */
  void updateKnowledge(String transactionId, boolean actualFraud, String feedback);
}
