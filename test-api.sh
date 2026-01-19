#!/bin/bash

echo "================================"
echo "Document Management API Test"
echo "================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if services are running
echo "1. Checking if services are running..."
echo "-----------------------------------"
docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "api-gateway|document-management|document-search|indexer-worker|mysql|elasticsearch|rabbitmq"
echo ""

# Check API Gateway health
echo "2. Checking API Gateway health..."
echo "-----------------------------------"
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/api/health)
if [ "$HEALTH" == "200" ]; then
    echo -e "${GREEN}✓ API Gateway is healthy (HTTP $HEALTH)${NC}"
else
    echo -e "${RED}✗ API Gateway not responding (HTTP $HEALTH)${NC}"
    echo "Tip: Run 'docker-compose logs api-gateway' to check logs"
fi
echo ""

# Create test user
echo "3. Creating test user account..."
echo "-----------------------------------"
SIGNUP_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost:8000/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser_'$(date +%s)'",
    "password": "password123",
    "email": "test'$(date +%s)'@tenant1.com",
    "tenantId": "tenant1",
    "role": "USER"
  }')

HTTP_CODE=$(echo "$SIGNUP_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
RESPONSE_BODY=$(echo "$SIGNUP_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "201" ]; then
    echo -e "${GREEN}✓ User created successfully (HTTP $HTTP_CODE)${NC}"
    echo "Response: $RESPONSE_BODY"
    TOKEN=$(echo "$RESPONSE_BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo ""
    echo -e "${YELLOW}JWT Token: $TOKEN${NC}"
else
    echo -e "${RED}✗ User creation failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $RESPONSE_BODY"
    echo ""
    echo "Trying to login with existing user..."
    LOGIN_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost:8000/api/auth/login \
      -H "Content-Type: application/json" \
      -d '{
        "username": "testuser",
        "password": "password123"
      }')

    HTTP_CODE=$(echo "$LOGIN_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    RESPONSE_BODY=$(echo "$LOGIN_RESPONSE" | sed '/HTTP_CODE/d')

    if [ "$HTTP_CODE" == "200" ]; then
        echo -e "${GREEN}✓ Login successful (HTTP $HTTP_CODE)${NC}"
        echo "Response: $RESPONSE_BODY"
        TOKEN=$(echo "$RESPONSE_BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    else
        echo -e "${RED}✗ Login failed (HTTP $HTTP_CODE)${NC}"
        echo "Response: $RESPONSE_BODY"
        exit 1
    fi
fi
echo ""

# Check if we got a token
if [ -z "$TOKEN" ]; then
    echo -e "${RED}✗ No JWT token received. Cannot proceed with tests.${NC}"
    exit 1
fi

# Create a test file
echo "4. Creating test document..."
echo "-----------------------------------"
echo "This is a test document for the document management system. It contains searchable content." > /tmp/test-document.txt
echo -e "${GREEN}✓ Test file created: /tmp/test-document.txt${NC}"
echo ""

# Upload document
echo "5. Testing POST /documents (Upload Document)..."
echo "-----------------------------------"
UPLOAD_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost:8000/documents \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/tmp/test-document.txt")

HTTP_CODE=$(echo "$UPLOAD_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
RESPONSE_BODY=$(echo "$UPLOAD_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "201" ] || [ "$HTTP_CODE" == "202" ]; then
    echo -e "${GREEN}✓ Document uploaded successfully (HTTP $HTTP_CODE)${NC}"
    echo "Response: $RESPONSE_BODY"
else
    echo -e "${RED}✗ Document upload failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $RESPONSE_BODY"
fi
echo ""

# Wait for indexing
echo "6. Waiting for document to be indexed..."
echo "-----------------------------------"
echo "Waiting 5 seconds for indexing to complete..."
sleep 5
echo -e "${GREEN}✓ Wait complete${NC}"
echo ""

# Search documents
echo "7. Testing GET /search (Search Documents)..."
echo "-----------------------------------"
SEARCH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "http://localhost:8000/search?q=test&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$SEARCH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
RESPONSE_BODY=$(echo "$SEARCH_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}✓ Search successful (HTTP $HTTP_CODE)${NC}"
    echo "Response: $RESPONSE_BODY"
else
    echo -e "${RED}✗ Search failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $RESPONSE_BODY"
fi
echo ""

# Check Elasticsearch
echo "8. Checking Elasticsearch indices..."
echo "-----------------------------------"
ES_RESPONSE=$(curl -s http://localhost:9200/_cat/indices?v 2>/dev/null)
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Elasticsearch is accessible${NC}"
    echo "$ES_RESPONSE"
else
    echo -e "${RED}✗ Elasticsearch not accessible${NC}"
fi
echo ""

# Check document count in Elasticsearch
echo "9. Checking indexed documents in Elasticsearch..."
echo "-----------------------------------"
ES_COUNT=$(curl -s "http://localhost:9200/documents/_search?size=0" 2>/dev/null | grep -o '"total":{"value":[0-9]*' | grep -o '[0-9]*$')
if [ ! -z "$ES_COUNT" ]; then
    echo -e "${GREEN}✓ Total documents in Elasticsearch: $ES_COUNT${NC}"
else
    echo -e "${YELLOW}⚠ Could not retrieve document count${NC}"
fi
echo ""

# Check MySQL
echo "10. Checking MySQL database..."
echo "-----------------------------------"
MYSQL_CHECK=$(docker exec document-mysql mysql -u docuser -pdocpassword document_management -e "SELECT COUNT(*) as count FROM documents;" 2>/dev/null | tail -n1)
if [ ! -z "$MYSQL_CHECK" ]; then
    echo -e "${GREEN}✓ Documents in MySQL: $MYSQL_CHECK${NC}"
else
    echo -e "${YELLOW}⚠ Could not check MySQL${NC}"
fi
echo ""

# Check RabbitMQ
echo "11. Checking RabbitMQ queues..."
echo "-----------------------------------"
RMQ_CHECK=$(curl -s -u guest:guest http://localhost:15672/api/queues 2>/dev/null | grep -o '"name":"[^"]*"' | head -5)
if [ ! -z "$RMQ_CHECK" ]; then
    echo -e "${GREEN}✓ RabbitMQ queues:${NC}"
    echo "$RMQ_CHECK"
else
    echo -e "${YELLOW}⚠ Could not check RabbitMQ${NC}"
fi
echo ""

echo "================================"
echo "Test Summary"
echo "================================"
echo ""
echo "Your JWT Token for manual testing:"
echo -e "${YELLOW}$TOKEN${NC}"
echo ""
echo "To manually test uploads:"
echo "curl -X POST http://localhost:8000/documents \\"
echo "  -H \"Authorization: Bearer $TOKEN\" \\"
echo "  -F \"file=@yourfile.txt\""
echo ""
echo "To manually test search:"
echo "curl -X GET \"http://localhost:8000/search?q=test\" \\"
echo "  -H \"Authorization: Bearer $TOKEN\""
echo ""

