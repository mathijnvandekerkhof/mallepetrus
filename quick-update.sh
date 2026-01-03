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
CYAN='\033[0;36m'
NC='\033[0m'

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                   ⚡ JIPTV Quick Update                       ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo

# Function to get available branches (clean)
get_branches() {
    git fetch origin --quiet 2>/dev/null || true
    git branch -r | grep -v HEAD | sed 's/origin\///' | sed 's/^[[:space:]]*//' | sort | uniq
}

# Function to show branches with numbers
show_branches() {
    local branches=("$@")
    local current_branch=$(git branch --show-current)
    
    echo -e "${CYAN}🌿 Available branches:${NC}"
    for i in "${!branches[@]}"; do
        local marker=""
        if [ "${branches[$i]}" = "$current_branch" ]; then
            marker=" ${GREEN}(current)${NC}"
        fi
        printf "   ${BLUE}%2d)${NC} %s%s\n" $((i+1)) "${branches[$i]}" "$marker"
    done
}

# Get available branches into array
branches=()
while IFS= read -r branch; do
    if [ -n "$branch" ]; then
        branches+=("$branch")
    fi
done < <(get_branches)

if [ ${#branches[@]} -eq 0 ]; then
    echo -e "${RED}❌ Error: No remote branches found!${NC}"
    exit 1
fi

# Determine branch to use
if [ -n "$1" ]; then
    # Check if argument is a number
    if [[ "$1" =~ ^[0-9]+$ ]] && [ "$1" -ge 1 ] && [ "$1" -le ${#branches[@]} ]; then
        BRANCH="${branches[$((1-1))]}"
        echo -e "${BLUE}ℹ${NC} Selected branch by number: ${BRANCH}"
    else
        # Treat as branch name
        BRANCH=$1
        echo -e "${BLUE}ℹ${NC} Selected branch by name: ${BRANCH}"
    fi
else
    # Show branches and ask for selection
    show_branches "${branches[@]}"
    echo
    read -p "Select branch by number (1-${#branches[@]}) or press Enter for current: " selection
    
    if [ -z "$selection" ]; then
        BRANCH=$(git branch --show-current)
        echo -e "${BLUE}ℹ${NC} Using current branch: ${BRANCH}"
    elif [[ "$selection" =~ ^[0-9]+$ ]] && [ "$selection" -ge 1 ] && [ "$selection" -le ${#branches[@]} ]; then
        BRANCH="${branches[$((selection-1))]}"
        echo -e "${BLUE}ℹ${NC} Selected branch: ${BRANCH}"
    else
        echo -e "${RED}❌ Invalid selection!${NC}"
        exit 1
    fi
fi

echo
echo -e "${CYAN}▶${NC} [1/4] Fetching latest changes..."
git fetch origin

echo -e "${CYAN}▶${NC} [2/4] Switching to branch: ${BRANCH}"
git checkout $BRANCH

echo -e "${CYAN}▶${NC} [3/4] Pulling latest changes..."
git pull origin $BRANCH

echo -e "${CYAN}▶${NC} [4/4] Building Docker image..."
docker build -t jiptv:latest .

echo
echo -e "${GREEN}✅ Update completed successfully!${NC}"
echo
echo -e "${CYAN}🔄 Next Steps:${NC}"
echo "   1. Go to Portainer: https://dock.mallepetrus.nl"
echo "   2. Navigate to Stacks → jiptv-app"
echo "   3. Click 'Restart' to use the new image"