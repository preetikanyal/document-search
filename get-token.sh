#!/bin/bash

echo "====================================="
echo "JWT Token Generation Script"
echo "====================================="
echo ""

# Method 1: Signup (creates new user and returns token)
echo "Method 1: SIGNUP (for new users)"
echo "-----------------------------------"
echo "Creating new user and getting token..."
echo ""

SIGNUP_RESPONSE=$(curl -s -X POST http://localhost:8000/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user'$(date +%s)'",
    "password": "password123",
    "email": "user'$(date +%s)'@tenant1.com",
    "tenantId": "tenant1",
    "role": "USER"
  }')

echo "Response:"
echo "$SIGNUP_RESPONSE" | jq '.'
echo ""

# Extract token from signup response
SIGNUP_TOKEN=$(echo "$SIGNUP_RESPONSE" | jq -r '.token // empty')

if [ ! -z "$SIGNUP_TOKEN" ] && [ "$SIGNUP_TOKEN" != "null" ]; then
    echo "✅ Token obtained via SIGNUP:"
    echo "$SIGNUP_TOKEN"
    echo ""
    echo "Export this token:"
    echo "export TOKEN=\"$SIGNUP_TOKEN\""
    echo ""
else
    echo "❌ Failed to get token from signup"
    echo ""
fi

echo "====================================="
echo ""

# Method 2: Login (for existing users)
echo "Method 2: LOGIN (for existing users)"
echo "-----------------------------------"
echo "Note: This will fail if user doesn't exist. Use signup first."
echo ""
echo "Example login curl:"
echo ""
echo 'curl -X POST http://localhost:8000/api/auth/login \'
echo '  -H "Content-Type: application/json" \'
echo '  -d '"'"'{'
echo '    "username": "youruser",'
echo '    "password": "yourpassword"'
echo "  }'"'"
echo ""

echo "====================================="
echo ""
echo "Quick Test Commands:"
echo ""
echo "# Set your token"
echo "TOKEN=\"$SIGNUP_TOKEN\""
echo ""
echo "# Upload a document"
echo 'curl -X POST http://localhost:8000/api/documents \'
echo '  -H "Authorization: Bearer $TOKEN" \'
echo '  -F "file=@sample.txt"'
echo ""
echo "# Search documents"
echo 'curl -X GET "http://localhost:8000/api/search?q=test" \'
echo '  -H "Authorization: Bearer $TOKEN"'
echo ""

