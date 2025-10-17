#!/bin/bash
set -e

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "STARTING KAFKA INFRASTRUCTURE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check if docker is available
if ! docker info > /dev/null 2>&1; then
  echo "Docker not ready yet, waiting..."
  sleep 5
fi

# Stop any existing containers
echo "Cleaning up any existing containers..."
docker-compose down -v 2>/dev/null || true

# Start Kafka infrastructure
echo "Starting Docker containers (Kafka, Kafka UI)..."
docker-compose up -d

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
echo "   (This takes about 30-40 seconds)"

MAX_WAIT=90
WAITED=0

while ! docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do
  if [ $WAITED -ge $MAX_WAIT ]; then
    echo ""
    echo "Kafka failed to start within $MAX_WAIT seconds"
    echo "   Try running: docker-compose logs kafka"
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

# Show container status
echo ""
echo "Container Status:"
docker-compose ps

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "INFRASTRUCTURE READY!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "NEXT STEPS:"
echo ""
echo "1️  Set your Groq API key (required):"
echo "    export GROQ_API_KEY='gsk_your_key_here'"
echo "    👉 Get key: https://console.groq.com/keys"
echo ""
echo "2️  Start the Spring Boot application:"
echo "    mvn spring-boot:run"
echo ""
echo "3️  In a NEW terminal, run test scenarios:"
echo "    mvn test-compile exec:java \\"
echo "      -Dexec.mainClass=\"com.agenticfraud.engine.testing.TestDataGenerator\""
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "USEFUL LINKS:"
echo ""
echo "   Kafka UI:     http://localhost:8090"
echo "   Spring Boot:  http://localhost:8080"
echo "   Health:       http://localhost:8080/api/fraud-detection/health"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"