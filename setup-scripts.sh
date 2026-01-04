#!/bin/bash
# Make scripts executable and keep them executable

# Ensure this script is executable (self-healing)
chmod +x "$0" 2>/dev/null || true

echo "🔧 Setting up JIPTV deployment scripts..."

# Make all scripts executable
chmod +x setup-scripts.sh 2>/dev/null || true
chmod +x update-backend.sh 2>/dev/null || true
chmod +x update-admin.sh 2>/dev/null || true
chmod +x mvnw 2>/dev/null || true

echo "✅ Scripts are now executable!"
echo
echo "📋 Available commands:"
echo "  ./update-backend.sh     - Update backend application"
echo "  ./update-admin.sh       - Update admin dashboard"
echo "  ./setup-scripts.sh      - Re-run this setup (if needed)"
echo
echo "💡 Note: Scripts automatically restore permissions after git operations"
echo "🔄 Self-healing: Scripts fix their own permissions when run"