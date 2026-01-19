#!/bin/bash

# Test script for Document Management Service

echo "🚀 Testing Document Management Service"
echo "======================================"
echo ""

# Check if service is running
echo "1️⃣ Checking if service is available..."
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || echo "Service not responding"
echo ""

# Create a test document
echo "2️⃣ Creating test file..."
echo "This is a test document for the Document Management Service" > test-document.txt
echo "✅ Test file created: test-document.txt"
echo ""

# Upload the document
echo "3️⃣ Uploading document via POST /documents..."
response=$(curl -s -X POST http://localhost:8080/documents \
  -F "file=@test-document.txt" \
  -w "\nHTTP_STATUS:%{http_code}")

http_status=$(echo "$response" | grep HTTP_STATUS | cut -d: -f2)
response_body=$(echo "$response" | sed '/HTTP_STATUS/d')

echo "Response Status: $http_status"
echo "Response Body:"
echo "$response_body" | jq '.' 2>/dev/null || echo "$response_body"
echo ""

# Clean up
echo "4️⃣ Cleaning up test file..."
rm -f test-document.txt
echo "✅ Test complete!"
echo ""
echo "📊 To view RabbitMQ Management UI: http://localhost:15672 (guest/guest)"
echo "📊 To check MySQL: docker exec -it document-mysql mysql -u docuser -pdocpassword document_management"

