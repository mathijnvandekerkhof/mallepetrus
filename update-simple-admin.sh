#!/bin/bash
# Simple JIPTV Admin Dashboard Update Script
clear
echo "🎨 JIPTV Simple Admin Dashboard Update"
echo "======================================"
BRANCH=${1:-main}
echo "Branch: $BRANCH"
echo
read -p "Update simple admin dashboard to '$BRANCH' and rebuild? (y/N): " confirm
if [[ ! $confirm =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 0
fi
echo
echo "▶ Updating repository..."
git fetch origin
git reset --hard origin/$BRANCH
echo "▶ Building Docker image..."
cd simple-admin
docker build -t jiptv-admin-simple:latest .
echo
echo "✅ Done! Deploy 'jiptv-admin-simple' stack in Portainer."
echo "   Use simple-admin/portainer-stack.yml"