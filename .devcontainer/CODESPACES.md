# Running in GitHub Codespaces

## Quick Start (2 minutes)

1. **Click the Codespaces button** in the README
2. **Wait ~90 seconds** for environment to start
3. **Set your API key** (required):
```bash
   export GROQ_API_KEY='your-key-here'
```
4. **Start the app**:
```bash
   mvn spring-boot:run
```
5. **Run test scenario** (in a new terminal):  
Simply run the class TestDataGenerator   
or
```bash
   mvn test-compile exec:java \
     -Dexec.mainClass="com.agenticfraud.engine.testing.TestDataGenerator"
```

---

## What Happens Automatically

When Codespaces starts:

- Java 21 + Maven installed  
- Docker containers start (Kafka, Zookeeper, Kafka UI)  
- Kafka topics created  
- Dependencies downloaded  
- Ports forwarded (8080, 8090, 9092)

---

## Getting a Groq API Key (FREE)

1. Go to [https://console.groq.com/keys](https://console.groq.com/keys)
2. Sign up (with a credit card but much cheaper than any other paid API, billed on usage basis)
3. Click "Create API Key"
4. Copy the key
5. In Codespaces terminal:
```bash
   export GROQ_API_KEY='gsk_your_key_here'
```

**Alternative:** Use OpenAI (more costlier)

---

## Step-by-Step Walkthrough

### 1. Check Infrastructure Status
```bash
# View Docker containers
docker-compose ps

# Should show:
# zookeeper    - Up (healthy)
# kafka        - Up (healthy)  
# kafka-ui     - Up (healthy)
```

### 2. Verify Kafka Topics
```bash
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Should show:
# transactions
# customerProfiles
# fraud-alerts
# human-review
# approved-transactions
# analyst-feedback
```

### 3. Start Spring Boot Application
```bash
# Terminal 1: Start the app
export GROQ_API_KEY='your-key-here'
mvn spring-boot:run

# Wait for: "Started AgenticFraudEngineApplication"
```

### 4. Generate Test Data
```bash
# Terminal 2: Run test data generator
mvn test-compile exec:java \
  -Dexec.mainClass="com.agenticfraud.engine.testing.TestDataGenerator"
  
# or just simply run the class TestDataGenerator

# Choose scenario:
# 2 - High velocity attack (RECOMMENDED for first demo)
```

### 6. View Results in Kafka UI

1. Click the **Kafka UI** port notification (or go to Ports tab → port 8090)
2. Navigate to **Topics** → **fraud-alerts**
3. Click **Messages** tab
4. See AI fraud detection in action! 🎉

---

## Test Scenarios Explained

### Scenario 2: High Velocity Attack (RECOMMENDED)
- Generates 15 rapid transactions in 3 seconds
- Triggers streaming velocity intelligence
- Result: **fraud-alerts** topic (99%+ confidence)
- **Best for demos** - clear fraud pattern

### Scenario 3: Unusual Amount Fraud
- Generates transactions 5x above customer average
- Tests customer profile intelligence
- Result: **human-review** topic (70-80% confidence)
- Shows edge case handling

### Scenario 1: Normal Transactions
- Generates legitimate transactions
- Tests normal flow
- Result: **approved-transactions** topic
- Shows false positive avoidance

---

## Troubleshooting

### "Kafka not ready"
```bash
# Check Kafka status
docker logs kafka --tail 50

# Restart if needed
docker-compose restart kafka
```

### "Port already in use"
```bash
# Stop all containers
docker-compose down

# Restart
docker-compose up -d
```

### "API key not set"
```bash
# Check if set
echo $GROQ_API_KEY

# Set it
export GROQ_API_KEY='your-key-here'

# Make it permanent (current session)
echo "export GROQ_API_KEY='your-key-here'" >> ~/.bashrc
source ~/.bashrc
```

### "Maven build errors"
```bash
# Clean and rebuild
mvn clean install
```

---

## Viewing Logs
```bash
# Spring Boot logs

# Kafka logs
docker logs kafka --tail 100 -f

# All containers
docker-compose logs -f
```

---

## Stopping Everything
```bash
# Stop Spring Boot: Ctrl+C in Terminal 1

# Stop Kafka infrastructure
docker-compose down

# Free up resources (stops Codespace)
# File → Close Remote Connection
```
---

## Need Help?

- Check logs: `docker-compose logs`
- Kafka UI: http://localhost:8090
- Issues: Open a GitHub issue with logs