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

# Stop any existing containers and remove volumes
echo "Cleaning up any existing containers..."
docker-compose down -v 2>/dev/null || true

# Start Kafka infrastructure
echo "Starting Kafka + Kafka UI..."
docker-compose up -d

echo ""
echo "Waiting for containers to be healthy..."
echo "   (This takes about 30-40 seconds)"
echo ""

# Wait with better feedback
MAX_WAIT=90
WAITED=0

while [ $WAITED -lt $MAX_WAIT ]; do
  # Check if kafka is healthy
  if docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
    echo ""
    echo "Kafka is ready!"
    break
  fi

  # Check if kafka container exited
  if ! docker ps | grep -q kafka; then
    echo ""
    echo "Kafka container stopped unexpectedly!"
    echo ""
    echo "Checking Kafka logs:"
    docker-compose logs kafka | tail -20
    echo ""
    echo "Try running: docker-compose up -d && docker-compose logs -f"
    exit 1
  fi

  echo -n "."
  sleep 3
  WAITED=$((WAITED + 3))
done

if [ $WAITED -ge $MAX_WAIT ]; then
  echo ""
  echo "Kafka failed to start within $MAX_WAIT seconds"
  echo ""
  echo "Checking logs:"
  docker-compose logs kafka | tail -30
  exit 1
fi

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
echo "    Get key: https://console.groq.com/keys (you can get free key but it wouldn't have sufficient tokens)"
echo ""
echo "2️  Start the Spring Boot application:"
echo "    mvn spring-boot:run"
echo ""
echo "3️  In a NEW terminal, run test scenarios:"
echo "    mvn test-compile exec:java \\"
echo "      -Dexec.mainClass=\"com.agenticfraud.engine.testing.TestDataGenerator\""
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " USEFUL LINKS:"
echo ""
echo "   Kafka UI:     http://localhost:8090"
echo "   Spring Boot:  http://localhost:8080"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"