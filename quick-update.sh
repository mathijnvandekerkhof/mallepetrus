#!/bin/bash

# Quick JIPTV Update Script
# Usage: ./quick-update.sh [branch-number-or-name]

set -e

clear

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🚀 JIPTV Quick Update${NC}"
echo "=========================="

# Function to get available branches
get_branches() {
    git branch -r | grep -v HEAD | sed 's/origin\///' | sed 's/^[[:space:]]*//' | sort
}

# Function to show branches with numbers
show_branches() {
    local branches=("$@")
    echo -e "${BLUE}Available branches:${NC}"
    for i in "${!branches[@]}"; do
        local current_marker=""
        if [ "${branches[$i]}" = "$(git branch --show-current)" ]; then
            current_marker=" ${GREEN}(current)${NC}"
        fi
        printf "  ${BLUE}%2d)${NC} %s%s\n" $((i+1)) "${branches[$i]}" "$current_marker"
    done
}

# Get available branches into array
branches=()
while IFS= read -r branch; do
    branches+=("$branch")
done < <(get_branches)

if [ ${#branches[@]} -eq 0 ]; then
    echo -e "${RED}Error: No remote branches found!${NC}"
    exit 1
fi

# Determine branch to use
if [ -n "$1" ]; then
    # Check if argument is a number
    if [[ "$1" =~ ^[0-9]+$ ]] && [ "$1" -ge 1 ] && [ "$1" -le ${#branches[@]} ]; then
        BRANCH="${branches[$((1-1))]}"
        echo -e "${BLUE}Selected branch by number:${NC} $BRANCH"
    else
        # Treat as branch name
        BRANCH=$1
        echo -e "${BLUE}Selected branch by name:${NC} $BRANCH"
    fi
else
    # Show branches and ask for selection
    show_branches "${branches[@]}"
    echo
    read -p "Select branch by number (1-${#branches[@]}) or press Enter for current: " selection
    
    if [ -z "$selection" ]; then
        BRANCH=$(git branch --show-current)
        echo -e "${BLUE}Using current branch:${NC} $BRANCH"
    elif [[ "$selection" =~ ^[0-9]+$ ]] && [ "$selection" -ge 1 ] && [ "$selection" -le ${#branches[@]} ]; then
        BRANCH="${branches[$((selection-1))]}"
        echo -e "${BLUE}Selected branch:${NC} $BRANCH"
    else
        echo -e "${RED}Invalid selection!${NC}"
        exit 1
    fi
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