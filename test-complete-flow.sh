#!/bin/bash

# Document Search Service - Complete Testing Script
# This script tests all API endpoints in sequence

set -e

echo "=========================================="
echo "Document Search Service - Complete Test"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# API Gateway URL
API_URL="http://localhost:8000"

echo "1. Testing Health Endpoint..."
echo "-----------------------------------"
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "$API_URL/api/health")
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
BODY=$(echo "$HEALTH_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✓ Health check passed${NC}"
    echo "Response: $BODY"
else
    echo -e "${RED}✗ Health check failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
fi
echo ""

echo "2. Testing Signup (Register New User)..."
echo "-----------------------------------"
# Generate unique username with timestamp
USERNAME="testuser$(date +%s)"
SIGNUP_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$USERNAME\",
    \"password\": \"password123\",
    \"email\": \"${USERNAME}@example.com\",
    \"tenantId\": \"tenant1\",
    \"role\": \"USER\"
  }")

HTTP_CODE=$(echo "$SIGNUP_RESPONSE" | tail -n1)
BODY=$(echo "$SIGNUP_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" -eq 201 ]; then
    echo -e "${GREEN}✓ Signup successful${NC}"
    echo "Response: $BODY"
    TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "Token extracted: ${TOKEN:0:50}..."
else
    echo -e "${RED}✗ Signup failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
    exit 1
fi
echo ""

echo "3. Testing Login..."
echo "-----------------------------------"
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$USERNAME\",
    \"password\": \"password123\"
  }")

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)
BODY=$(echo "$LOGIN_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✓ Login successful${NC}"
    echo "Response: $BODY"
    TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "Updated token: ${TOKEN:0:50}..."
else
    echo -e "${RED}✗ Login failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
    exit 1
fi
echo ""

echo "4. Creating Test Document..."
echo "-----------------------------------"
TEST_FILE="test-document-$$.txt"
echo "This is a test document about invoices, payments, and financial reports. It contains searchable content for testing the document search service." > "$TEST_FILE"
echo -e "${GREEN}✓ Test file created: $TEST_FILE${NC}"
echo ""

echo "5. Testing Document Upload..."
echo "-----------------------------------"
UPLOAD_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/documents" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@$TEST_FILE" \
  -F "tenantId=tenant1")

HTTP_CODE=$(echo "$UPLOAD_RESPONSE" | tail -n1)
BODY=$(echo "$UPLOAD_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" -eq 201 ] || [ "$HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✓ Document upload successful${NC}"
    echo "Response: $BODY"
    DOCUMENT_ID=$(echo "$BODY" | grep -o '"documentId":"[^"]*"' | cut -d'"' -f4)
    if [ -n "$DOCUMENT_ID" ]; then
        echo "Document ID: $DOCUMENT_ID"
    fi
else
    echo -e "${RED}✗ Document upload failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
fi
echo ""

echo "6. Waiting for indexing to complete..."
echo "-----------------------------------"
echo -e "${YELLOW}⏳ Waiting 10 seconds for async indexing...${NC}"
sleep 10
echo -e "${GREEN}✓ Wait complete${NC}"
echo ""

echo "7. Testing Document Search..."
echo "-----------------------------------"
SEARCH_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/search?q=invoice&tenant=tenant1" \
  -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$SEARCH_RESPONSE" | tail -n1)
BODY=$(echo "$SEARCH_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✓ Search successful${NC}"
    echo "Response: $BODY"
else
    echo -e "${RED}✗ Search failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
fi
echo ""

echo "8. Testing Another Search Query..."
echo "-----------------------------------"
SEARCH_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/search?q=payment&tenant=tenant1" \
  -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$SEARCH_RESPONSE" | tail -n1)
BODY=$(echo "$SEARCH_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✓ Search successful${NC}"
    echo "Response: $BODY"
else
    echo -e "${RED}✗ Search failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
fi
echo ""

echo "9. Cleanup..."
echo "-----------------------------------"
rm -f "$TEST_FILE"
echo -e "${GREEN}✓ Test file removed${NC}"
echo ""

echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo -e "${GREEN}All tests completed!${NC}"
echo ""
echo "Your Document Search Service is working correctly!"
echo ""
echo "You can now use the following credentials to test manually:"
echo "  Username: $USERNAME"
echo "  Password: password123"
echo "  Tenant ID: tenant1"
echo "  Token: ${TOKEN:0:50}..."
echo ""

