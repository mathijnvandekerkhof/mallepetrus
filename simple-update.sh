#!/bin/bash

clear

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                   🚀 JIPTV Simple Update                     ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

# Show current status
echo -e "${CYAN}📍 Current Status:${NC}"
echo "   Repository: $(pwd)"
echo "   Branch: $(git branch --show-current)"
echo

# Simple branch selection
echo -e "${CYAN}🌿 Branch Options:${NC}"
echo "   1) Stay on current branch ($(git branch --show-current))"
echo "   2) Switch to main"
echo "   3) Switch to develop"
echo "   4) Enter custom branch name"
echo

read -p "Select option (1-4): " choice

case $choice in
    1)
        BRANCH=$(git branch --show-current)
        echo -e "${BLUE}ℹ${NC} Using current branch: $BRANCH"
        ;;
    2)
        BRANCH="main"
        echo -e "${BLUE}ℹ${NC} Switching to: $BRANCH"
        ;;
    3)
        BRANCH="develop"
        echo -e "${BLUE}ℹ${NC} Switching to: $BRANCH"
        ;;
    4)
        read -p "Enter branch name: " BRANCH
        echo -e "${BLUE}ℹ${NC} Using custom branch: $BRANCH"
        ;;
    *)
        echo -e "${RED}❌ Invalid choice, using current branch${NC}"
        BRANCH=$(git branch --show-current)
        ;;
esac

echo
echo -e "${CYAN}▶${NC} [1/4] Fetching latest changes..."
if git fetch origin; then
    echo -e "${GREEN}✅${NC} Fetch completed"
else
    echo -e "${RED}❌${NC} Fetch failed, continuing anyway..."
fi

echo -e "${CYAN}▶${NC} [2/4] Switching to branch: $BRANCH"
if git checkout $BRANCH; then
    echo -e "${GREEN}✅${NC} Branch switch completed"
else
    echo -e "${RED}❌${NC} Branch switch failed"
    exit 1
fi

echo -e "${CYAN}▶${NC} [3/4] Pulling latest changes..."
if git pull origin $BRANCH; then
    echo -e "${GREEN}✅${NC} Pull completed"
else
    echo -e "${RED}❌${NC} Pull failed"
    exit 1
fi

echo -e "${CYAN}▶${NC} [4/4] Building Docker image..."
if docker build -t jiptv:latest .; then
    echo -e "${GREEN}✅${NC} Docker build completed"
else
    echo -e "${RED}❌${NC} Docker build failed"
    exit 1
fi

echo
echo -e "${GREEN}🎉 Update completed successfully!${NC}"
echo
echo -e "${CYAN}🔄 Next Steps:${NC}"
echo "   1. Go to Portainer: https://dock.mallepetrus.nl"
echo "   2. Navigate to Stacks → jiptv-app"
echo "   3. Click 'Restart' to use the new image"