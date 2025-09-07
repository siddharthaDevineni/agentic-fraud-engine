package com.agenticfraud.engine.models;

import java.time.LocalDateTime;

public record AgentInsight(String agentType, String agentName, String analysis, double riskScore, double confidence,
                           String reasoning, String recommendation, LocalDateTime timestamp) {

    public static AgentInsight create(String agentType, String agentName, String analysis, double riskScore,
                                      String reasoning, String recommendation) {
        return new AgentInsight(agentType, agentName, analysis, riskScore, Math.min(riskScore, 1.0), reasoning,
                recommendation, LocalDateTime.now());
    }

    public boolean indicatesFraud() {
        return riskScore > 0.6;
    }

    public boolean isHighConfidence() {
        return confidence > 0.8;
    }
}