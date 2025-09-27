package com.agenticfraud.engine.agents;

import com.agenticfraud.engine.models.AgentInsight;
import com.agenticfraud.engine.models.Transaction;

public interface FraudAgent {

  /** Analyze a transaction and provide insights */
  AgentInsight analyze(Transaction transaction);

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

  /** Collaborate with other agents on a decision */
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
