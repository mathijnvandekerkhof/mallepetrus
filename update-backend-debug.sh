#!/bin/bash
# JIPTV Backend Update Script - Debug Version
set -e  # Exit on error

clear
echo "🚀 JIPTV Backend Update (Debug)"
echo "==============================="
BRANCH=${1:-main}
echo "Branch: $BRANCH"
echo

echo "Current directory: $(pwd)"
echo "Git status:"
git status --short || echo "Git status failed"
echo

read -p "Update backend to '$BRANCH' and rebuild? (y/N): " confirm
echo "You entered: '$confirm'"

if [[ ! $confirm =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 0
fi

echo
echo "▶ Updating repository..."
git fetch origin
git reset --hard origin/$BRANCH

echo "▶ Building Docker image..."
docker build -t jiptv:latest .

echo
echo "✅ Done! Restart 'jiptv-app' stack in Portainer."