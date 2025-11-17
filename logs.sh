#!/bin/bash
# =============================================
# Generate logs.json with commit history of current branch
# =============================================

echo "Fetching git logs for current branch..."

# Create JSON-formatted commit data
git log --pretty=format:'{ "hash": "%H", "author": "%an", "date": "%ad", "message": "%s" },' > temp_logs.txt

# Create valid JSON array
echo "[" > logs.json
sed '$ s/,$//' temp_logs.txt >> logs.json
echo "]" >> logs.json

rm temp_logs.txt

echo "logs.json generated successfully!"
