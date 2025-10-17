#!/bin/bash
set -e

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "STARTING KAFKA INFRASTRUCTURE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Start Kafka infrastructure in background
echo "Starting Docker containers (Zookeeper, Kafka, Kafka UI)..."
docker-compose up -d

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
echo "   (This takes about 30-40 seconds)"

MAX_WAIT=60
WAITED=0

while ! docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do
  if [ $WAITED -ge $MAX_WAIT ]; then
    echo "Kafka failed to start within $MAX_WAIT seconds"
    exit 1
  fi

  echo -n "."
  sleep 2
  WAITED=$((WAITED + 2))
done

echo ""
echo "Kafka is ready!"

# Create topics
echo ""
echo "Creating Kafka topics..."
bash setup-topics.sh

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "INFRASTRUCTURE READY!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "NEXT STEPS:"
echo ""
echo "1  Set your Groq API key (required):"
echo "    export GROQ_API_KEY='your-api-key-here'"
echo ""
echo "2  Start the Spring Boot application:"
echo "    mvn spring-boot:run"
echo ""
echo "3️  Run test scenarios:"
echo "    mvn test-compile exec:java \\"
echo "      -Dexec.mainClass=\"com.agenticfraud.engine.testing.TestDataGenerator\""
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "USEFUL LINKS:"
echo ""
echo "   Kafka UI:     http://localhost:8090"
echo "   Spring Boot:  http://localhost:8080"
echo "   API Health:   http://localhost:8080/api/fraud-detection/health"
echo "   Agents Info:  http://localhost:8080/api/fraud-detection/agents/info"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"