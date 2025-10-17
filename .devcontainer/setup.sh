#!/bin/bash
set -e

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "AGENTIC FRAUD ENGINE - Initial Setup"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Install additional tools
echo "Installing additional tools..."
sudo apt-get update -qq
sudo apt-get install -y -qq jq curl netcat > /dev/null 2>&1

# Check Java version
echo "Java version:"
java -version

# Check Maven version
echo ""
echo "Maven version:"
mvn -version

# Build the application (download dependencies)
echo ""
echo "Building application and downloading dependencies..."
echo "   (This may take 2-3 minutes on first run)"
mvn clean compile -DskipTests -q

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Initial setup complete!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Next steps will run automatically..."
echo ""