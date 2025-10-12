Agentic Fraud Engine

> **Intelligent Fraud Detection where AI Agents get Smarter with Kafka Streaming Context**

Fraud detection system combining:

- **Multi-Agent AI** - 5 specialized fraud detection agents collaboration
- **Streaming Intelligence** - Real-time Kafka context enhances AI
- **Intelligent Routing** - Dynamic decision flows based on confidence
- **Learning Loops** - Continuous improvement from analyst feedback

[Demo Video] [Architecture] [Live Demo] [Blog Post]

<img src="demo.gif" width="800" alt="Real-time fraud detection in action"/>

## See It In Action (30-second demo)

Watch 5 AI agents detect a high-velocity attack in real-time with Kafka streaming context.

## The Problem

Traditional fraud detection systems are **blind**:

- AI models analyze transactions in isolation
- No real-time context about customer behavior
- Can't detect velocity-based attacks (rapid-fire transactions)
- High false positive rates

**Example:** A $2,500 transaction looks normal in isolation. But with the following streaming context:

- Customer average: $50
- 15 transactions in the last 5 minutes

- → Result: **Card testing attack detected!**

## The Solution: Streaming-Intelligent AI

This system combines **Kafka Streams real-time context** with **Multi-Agent AI** to make fraud detection smarter.  

**Streaming-Intelligent AI**: Kafka streams enrich AI agents with:

- 5-minute velocity windows → Detect rapid-fire attacks
- Customer profile context → Spot unusual amounts instantly
- Real-time collaboration → Agents debate suspicious patterns

### Architecture Overview

<img src="docs/architecture_agentic_fraud.png" width="1200" alt="Streaming-Intelligent Architecture"/>

### How It Works:
#### **Streaming Enrichment Layer**
Every transaction gets enriched with real-time context before AI analysis:

```java
// Traditional approach: Transaction alone
Transaction txn = {amount: $2500, merchant: "Online Store"}

// This approach: Streaming-enriched transaction
EnrichedTransaction = {
  transaction: {amount: $2500, merchant: "Online Store"},
  customerProfile: {averageAmount: $50, riskLevel: "HIGH"},
  velocity: 15 transactions in last 5 minutes  ← Kafka Streams intelligence
}

## Results

- **High Velocity Attack**: `15 transactions/5min` → FRAUD (95% confidence)
- **Unusual Amount**: `$2,50` on `$50` avg customer → HUMAN REVIEW (72% confidence)
- **Normal Transaction**: `$48` grocery → APPROVED (91% confidence)

[Show 3 side-by-side screenshots of Kafka UI topics]