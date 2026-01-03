#!/bin/bash

echo "=== Debug Branch Detection ==="

echo "1. Current branch:"
git branch --show-current

echo
echo "2. All local branches:"
git branch

echo
echo "3. All remote branches (raw):"
git branch -r

echo
echo "4. Remote branches (filtered):"
git branch -r | grep -v HEAD | grep -v "origin/HEAD"

echo
echo "5. Remote branches (cleaned):"
git branch -r | grep -v HEAD | grep -v "origin/HEAD" | sed 's/origin\///' | sed 's/^[[:space:]]*//'

echo
echo "6. Final branch list:"
git branch -r 2>/dev/null | \
    grep -v HEAD | \
    grep -v "origin/HEAD" | \
    sed 's/origin\///' | \
    sed 's/^[[:space:]]*//' | \
    sed 's/[[:space:]]*$//' | \
    sort | \
    uniq | \
    grep -v '^$'

echo
echo "7. Count of branches:"
git branch -r 2>/dev/null | \
    grep -v HEAD | \
    grep -v "origin/HEAD" | \
    sed 's/origin\///' | \
    sed 's/^[[:space:]]*//' | \
    sed 's/[[:space:]]*$//' | \
    sort | \
    uniq | \
    grep -v '^$' | \
    wc -l