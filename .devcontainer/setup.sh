#!/bin/bash
set -e

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "AGENTIC FRAUD ENGINE - Initial Setup"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Install additional tools
echo "Installing additional tools..."
sudo apt-get update -qq
sudo apt-get install -y -qq jq curl netcat-openbsd > /dev/null 2>&1

echo "Installing docker-compose..."
sudo curl -SL https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-x86_64 \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verify installations
echo ""
echo "Java version:"
java -version 2>&1 | head -1

echo ""
echo "Maven version:"
mvn -version | head -1

echo ""
echo "Docker version:"
docker --version

echo ""
echo "Docker Compose version:"
docker-compose --version

# Download Maven dependencies
echo ""
echo "🔨 Downloading dependencies..."
echo "   (This may take 2-3 minutes on first run)"
mvn dependency:resolve -q 2>/dev/null || mvn dependency:resolve

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Initial setup complete!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Next steps will run automatically..."
echo ""