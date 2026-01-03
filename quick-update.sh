#!/bin/bash

# Quick JIPTV Update Script
# Usage: ./quick-update.sh [branch-name]

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🚀 JIPTV Quick Update${NC}"
echo "=========================="

# Use provided branch or ask for it
if [ -n "$1" ]; then
    BRANCH=$1
else
    echo "Available branches:"
    git branch -r | grep -v HEAD | sed 's/origin\///' | sort
    echo
    read -p "Enter branch name (default: main): " BRANCH
    BRANCH=${BRANCH:-main}
fi

echo -e "${BLUE}[1/4]${NC} Fetching latest changes..."
git fetch origin

echo -e "${BLUE}[2/4]${NC} Switching to branch: $BRANCH"
git checkout $BRANCH

echo -e "${BLUE}[3/4]${NC} Pulling latest changes..."
git pull origin $BRANCH

echo -e "${BLUE}[4/4]${NC} Building Docker image..."
docker build -t jiptv:latest .

echo -e "${GREEN}✅ Update completed!${NC}"
echo
echo "To restart the application:"
echo "1. Go to Portainer"
echo "2. Restart the jiptv-app stack"