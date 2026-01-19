#!/bin/bash
# Simple test script - run this to verify your endpoints are working

echo "Step 1: Check if services are running"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "Step 2: Test API Gateway health endpoint (no auth needed)"
curl -v http://localhost:8000/api/health
echo ""

echo ""
echo "Step 3: Create a user and get JWT token"
echo "Run this command and save the token from the response:"
echo ""
echo 'curl -X POST http://localhost:8000/api/auth/signup \'
echo '  -H "Content-Type: application/json" \'
echo '  -d '"'"'{'
echo '    "username": "user1",'
echo '    "password": "password123",'
echo '    "email": "user1@tenant1.com",'
echo '    "tenantId": "tenant1",'
echo '    "role": "USER"'
echo "  }'"'"
echo ""

echo "Step 4: After you get the token, set it as a variable:"
echo 'TOKEN="paste_your_token_here"'
echo ""

echo "Step 5: Upload a document (NOTE: endpoint is /api/documents)"
echo 'curl -v -X POST http://localhost:8000/api/documents \'
echo '  -H "Authorization: Bearer $TOKEN" \'
echo '  -F "file=@sample.txt"'
echo ""

echo "Step 6: Wait 5 seconds for indexing, then search (NOTE: endpoint is /api/search)"
echo 'sleep 5'
echo 'curl -v -X GET "http://localhost:8000/api/search?q=test" \'
echo '  -H "Authorization: Bearer $TOKEN"'
echo ""
