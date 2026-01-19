# API Gateway with JWT Authentication Guide

## 🔐 Overview

The API Gateway has been implemented with **JWT-based authentication** where:
- ✅ **TenantId is embedded in JWT token**
- ✅ **All APIs extract tenantId from token automatically**
- ✅ **No need to pass tenantId as parameter anymore**
- ✅ **Backend services are not exposed directly** (secured behind gateway)

---

## 🏗️ Architecture

```
Client → API Gateway (Port 8000) → Backend Services
         ↓
    [JWT Validation]
    [Extract tenantId from token]
    [Forward request with tenantId]
```

### Services:
- **API Gateway**: Port 8000 (Public - only exposed port)
- **Document Management Service**: Internal only (via gateway)
- **Indexer Worker**: Internal only (via gateway)
- **MySQL**: Stores user accounts with tenantId
- **Elasticsearch**: Document search
- **RabbitMQ**: Message queue

---

## 📋 Database Schema

### Users Table (Auto-created)
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,  -- BCrypt hashed
    email VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL, -- User's tenant
    role VARCHAR(50) NOT NULL,        -- USER, ADMIN
    enabled BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    last_login DATETIME
);
```

---

## 🚀 Quick Start

### 1. Build and Run
```bash
cd /Users/i560653/Desktop/document-search
docker-compose up --build
```

### 2. Create a User Account
```bash
# Signup for tenant1
curl -X POST http://localhost:8000/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_tenant1",
    "password": "password123",
    "email": "john@tenant1.com",
    "tenantId": "tenant1",
    "role": "USER"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "john_tenant1",
  "tenantId": "tenant1",
  "role": "USER",
  "expiresIn": 86400000
}
```

### 3. Login
```bash
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_tenant1",
    "password": "password123"
  }'
```

### 4. Use the Token
```bash
# Save token
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# Upload document (tenantId automatically from token)
curl -X POST http://localhost:8000/api/documents \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@mydocument.pdf"

# Search documents (tenantId automatically from token)
curl -X GET "http://localhost:8000/api/search?q=contract" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🔑 Authentication Endpoints

### 1. **Signup** (No auth required)
```bash
POST /api/auth/signup
Content-Type: application/json

{
  "username": "string",      # Required, 3-50 chars
  "password": "string",      # Required, min 6 chars
  "email": "string",         # Required, valid email
  "tenantId": "string",      # Required
  "role": "USER"             # Optional, default: USER
}
```

**Response:**
```json
{
  "token": "jwt-token-here",
  "type": "Bearer",
  "username": "john_tenant1",
  "tenantId": "tenant1",
  "role": "USER",
  "expiresIn": 86400000  // 24 hours in milliseconds
}
```

### 2. **Login** (No auth required)
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "string",
  "password": "string"
}
```

**Response:** Same as signup

### 3. **Health Check** (No auth required)
```bash
GET /api/auth/health
```

---

## 📄 Document Management Endpoints

**All require JWT token in Authorization header**

### Upload Document
```bash
POST /api/documents
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: <binary>
# tenantId is automatically extracted from token!
```

---

## 🔍 Search Endpoints

**All require JWT token and auto-extract tenantId**

### 1. General Search
```bash
GET /api/search?q={query}
Authorization: Bearer {token}
```

### 2. Search by Filename
```bash
GET /api/search/filename?name={filename}
Authorization: Bearer {token}
```

### 3. Search by Content
```bash
GET /api/search/content?text={searchText}
Authorization: Bearer {token}
```

### 4. Search by Content Type
```bash
GET /api/search/type?contentType={mimeType}
Authorization: Bearer {token}
```

### 5. Search by File Type
```bash
GET /api/search/filetype?fileType={extension}
Authorization: Bearer {token}
```

### 6. Get All Documents for Your Tenant
```bash
GET /api/search/tenant
Authorization: Bearer {token}
# Returns all documents for the user's tenant only
```

### 7. Advanced Search
```bash
GET /api/search/advanced?fileName={name}&content={text}&fileType={ext}
Authorization: Bearer {token}
```

### 8. Get Document by ID
```bash
GET /api/search/document/{id}
Authorization: Bearer {token}
# Only returns if document belongs to user's tenant
```

---

## 🧪 Complete Testing Example

### Step 1: Create accounts for different tenants
```bash
# Tenant 1 user
curl -X POST http://localhost:8000/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice_company1",
    "password": "secure123",
    "email": "alice@company1.com",
    "tenantId": "company1"
  }'

# Tenant 2 user
curl -X POST http://localhost:8000/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "bob_company2",
    "password": "secure123",
    "email": "bob@company2.com",
    "tenantId": "company2"
  }'
```

### Step 2: Get tokens
```bash
# Alice's token (company1)
ALICE_TOKEN=$(curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice_company1","password":"secure123"}' \
  | jq -r '.token')

# Bob's token (company2)
BOB_TOKEN=$(curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"bob_company2","password":"secure123"}' \
  | jq -r '.token')
```

### Step 3: Upload documents
```bash
# Alice uploads document (will be tagged with company1)
curl -X POST http://localhost:8000/api/documents \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -F "file=@contract.pdf"

# Bob uploads document (will be tagged with company2)
curl -X POST http://localhost:8000/api/documents \
  -H "Authorization: Bearer $BOB_TOKEN" \
  -F "file=@invoice.pdf"
```

### Step 4: Search - each user only sees their tenant's documents
```bash
# Alice searches (only sees company1 documents)
curl -X GET "http://localhost:8000/api/search?q=contract" \
  -H "Authorization: Bearer $ALICE_TOKEN"

# Bob searches (only sees company2 documents)
curl -X GET "http://localhost:8000/api/search?q=invoice" \
  -H "Authorization: Bearer $BOB_TOKEN"
```

---

## 🔒 Security Features

### JWT Token Contains:
```json
{
  "sub": "alice_company1",      // username
  "tenantId": "company1",        // tenant identifier
  "role": "USER",                // user role
  "iat": 1705670400,             // issued at
  "exp": 1705756800              // expiration (24h)
}
```

### Security Layers:
1. **JWT Validation**: Token signature verified on every request
2. **Token Expiration**: Tokens expire after 24 hours
3. **Tenant Isolation**: TenantId extracted from token, not request parameter
4. **Password Hashing**: BCrypt with salt
5. **Backend Services Hidden**: Not exposed directly, only via gateway
6. **Authorization**: Users can only access their tenant's data

---

## 🚫 What Changed from Before

### Before (Insecure):
```bash
# TenantId passed as parameter - ANYONE could access ANY tenant's data!
curl "http://localhost:8081/api/search?q=contract&tenantId=company1"
```

### Now (Secure):
```bash
# TenantId comes from authenticated JWT token
curl "http://localhost:8000/api/search?q=contract" \
  -H "Authorization: Bearer $TOKEN"
# Gateway extracts tenantId from token automatically
```

---

## ⚙️ Configuration

### JWT Secret (IMPORTANT - Change in Production!)
Edit `api-gateway/src/main/resources/application.properties`:
```properties
jwt.secret=your-very-long-secret-key-at-least-256-bits-long-for-hs256-algorithm-change-this-in-production
jwt.expiration=86400000  # 24 hours in milliseconds
```

**Production**: Use environment variable and generate a strong random key:
```bash
openssl rand -base64 64
```

---

## 📊 Architecture Flow

### Document Upload Flow:
```
1. Client → POST /api/documents + JWT token
2. Gateway validates JWT
3. Gateway extracts tenantId from token
4. Gateway forwards to document-management-service with tenantId
5. Document saved with tenantId
6. Message published to RabbitMQ
7. Indexer-worker indexes with tenantId
```

### Search Flow:
```
1. Client → GET /api/search?q=query + JWT token
2. Gateway validates JWT
3. Gateway extracts tenantId from token
4. Gateway forwards to indexer-worker with tenantId
5. Elasticsearch queries filtered by tenantId
6. Results returned (only user's tenant data)
```

---

## 🛠️ Troubleshooting

### Invalid Token Error
```json
{"error": "Invalid JWT token"}
```
**Solution**: Token expired or invalid. Login again.

### Unauthorized Error
```json
{"timestamp": "...", "status": 401, "error": "Unauthorized"}
```
**Solution**: Missing or invalid Authorization header.

### Username Already Taken
```json
{"error": "Username is already taken"}
```
**Solution**: Choose a different username.

---

## 🔄 Token Refresh (Future Enhancement)

Currently tokens expire after 24 hours. To implement refresh tokens:
1. Add `refresh_token` table
2. Create `/api/auth/refresh` endpoint
3. Return both access and refresh tokens on login

---

## 📝 User Management (Future Enhancement)

Additional endpoints you may want to add:
- `GET /api/users/me` - Get current user info
- `PUT /api/users/me` - Update user profile
- `POST /api/users/change-password` - Change password
- `GET /api/admin/users` - Admin: list all users (requires ADMIN role)
- `PUT /api/admin/users/{id}/disable` - Admin: disable user

---

## 🎯 Best Practices

1. **Always use HTTPS in production**
2. **Change JWT secret before deploying**
3. **Set strong password policies**
4. **Implement rate limiting**
5. **Add token refresh mechanism**
6. **Log authentication attempts**
7. **Monitor for suspicious activity**
8. **Use short token expiration times**

---

## 📚 Summary

✅ **Authentication**: JWT-based with username/password
✅ **Authorization**: TenantId embedded in token
✅ **Multi-tenancy**: Complete data isolation
✅ **Security**: Backend services hidden behind gateway
✅ **Ease of Use**: Clients only need to send token, no manual tenantId
✅ **Scalability**: Stateless JWT tokens

**All APIs now automatically enforce tenant isolation through JWT authentication!**

