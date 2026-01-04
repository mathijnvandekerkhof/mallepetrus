#!/bin/bash
# JIPTV Admin Dashboard Update Script
clear
echo "🎨 JIPTV Admin Dashboard Update"
echo "==============================="
BRANCH=${1:-main}
echo "Branch: $BRANCH"
echo
read -p "Update admin dashboard to '$BRANCH' and rebuild? (y/N): " confirm
if [[ ! $confirm =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 0
fi
echo
echo "▶ Updating repository..."
git fetch origin
git reset --hard origin/$BRANCH
echo "▶ Building Docker image..."
cd jiptv-admin-dashboard
NEXT_PUBLIC_API_URL=https://api.mallepetrus.nl docker build -t jiptv-admin:latest .
echo
echo "✅ Done! Restart 'jiptv-admin' stack in Portainer."