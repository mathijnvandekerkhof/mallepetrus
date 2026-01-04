#!/bin/bash

# Quick JIPTV Update Script
# Usage: ./quick-update.sh [branch_name_or_number]

clear

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                   ⚡ JIPTV Quick Update                      ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Current status
current_branch=$(git branch --show-current)
echo -e "${CYAN}📍 Current Status:${NC}"
echo "   Repository: $(pwd)"
echo "   Branch: ${current_branch}"
echo "   Last commit: $(git log -1 --pretty=format:'%h - %s (%cr)' 2>/dev/null || echo 'No commits')"
echo

# Get available branches
echo -e "${CYAN}🌿 Available Branches:${NC}"
branches=($(git branch -r 2>/dev/null | grep -v HEAD | sed 's/origin\///' | sed 's/^[[:space:]]*//' | sort | uniq))

for i in "${!branches[@]}"; do
    marker=""
    if [ "${branches[$i]}" = "$current_branch" ]; then
        marker=" ${GREEN}← current${NC}"
    fi
    printf "   ${BLUE}%2d)${NC} %-20s%s\n" $((i+1)) "${branches[$i]}" "$marker"
done
echo

# Branch selection
if [ -n "$1" ]; then
    # Parameter provided
    if [[ "$1" =~ ^[0-9]+$ ]]; then
        # It's a number
        if [ "$1" -ge 1 ] && [ "$1" -le ${#branches[@]} ]; then
            BRANCH="${branches[$((1-1))]}"
            echo -e "${BLUE}ℹ${NC} Using branch #$1: $BRANCH"
        else
            echo -e "${RED}❌${NC} Invalid branch number: $1"
            exit 1
        fi
    else
        # It's a branch name
        BRANCH="$1"
        echo -e "${BLUE}ℹ${NC} Using specified branch: $BRANCH"
    fi
else
    # Interactive selection
    echo -n "Select branch by number (1-${#branches[@]}) or press Enter for current: "
    read selection
    
    if [ -z "$selection" ]; then
        BRANCH="$current_branch"
        echo -e "${BLUE}ℹ${NC} Using current branch: $BRANCH"
    elif [[ "$selection" =~ ^[0-9]+$ ]] && [ "$selection" -ge 1 ] && [ "$selection" -le ${#branches[@]} ]; then
        BRANCH="${branches[$((selection-1))]}"
        echo -e "${BLUE}ℹ${NC} Selected branch: $BRANCH"
    else
        echo -e "${RED}❌${NC} Invalid selection, using current branch"
        BRANCH="$current_branch"
    fi
fi

echo
echo -e "${YELLOW}⚠${NC} This will update to branch '$BRANCH' and rebuild the Docker image."
echo -e "${YELLOW}⚠${NC} Any local changes will be discarded!"
read -p "Continue? (y/N): " confirm

if [[ ! $confirm =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}ℹ${NC} Operation cancelled."
    exit 0
fi

echo
echo -e "${CYAN}▶${NC} [1/4] Fetching latest changes..."
if git fetch origin --quiet; then
    echo -e "${GREEN}✅${NC} Fetch completed"
else
    echo -e "${RED}❌${NC} Fetch failed"
    exit 1
fi

echo -e "${CYAN}▶${NC} [2/4] Resetting local changes and switching to branch: $BRANCH"
# Reset any local changes and switch to branch
if git reset --hard HEAD --quiet && git checkout "$BRANCH" --quiet; then
    echo -e "${GREEN}✅${NC} Reset and branch switch completed"
else
    echo -e "${RED}❌${NC} Reset or branch switch failed"
    exit 1
fi

echo -e "${CYAN}▶${NC} [3/4] Pulling latest changes..."
if git reset --hard origin/$BRANCH --quiet; then
    echo -e "${GREEN}✅${NC} Pull completed (forced update)"
else
    echo -e "${RED}❌${NC} Pull failed"
    exit 1
fi

echo -e "${CYAN}▶${NC} [4/4] Building Docker image: jiptv:latest"
if docker build -t jiptv:latest . --quiet; then
    echo -e "${GREEN}✅${NC} Docker build completed"
else
    echo -e "${RED}❌${NC} Docker build failed"
    exit 1
fi

echo
echo -e "${GREEN}🎉 Quick update completed successfully!${NC}"
echo
echo -e "${CYAN}🔄 Next Steps:${NC}"
echo "   1. Go to Portainer: https://dock.mallepetrus.nl"
echo "   2. Navigate to Stacks → jiptv-app"
echo "   3. Click 'Restart' to deploy the new image"
echo
echo -e "${BLUE}💡 Tip:${NC} You can also run: ${CYAN}./quick-update.sh 1${NC} or ${CYAN}./quick-update.sh main${NC}"