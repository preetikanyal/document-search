# Document Management Service - Docker Setup

## Overview
This setup includes:
- **MySQL 8.0** - Database for storing document metadata and user accounts
- **RabbitMQ 3.12** - Message broker for asynchronous processing
- **Elasticsearch 8.11** - Document indexing and search engine
- **API Gateway** - Main entry point with JWT authentication (Port 8000)
- **Document Management Service** - Spring Boot application (Internal)
- **Document Search Service** - Search API (Internal)
- **Indexer Worker** - Spring Boot worker service for document processing (Internal)

## Prerequisites
- Docker and Docker Compose installed
- Ports 8000, 3306, 5672, 9200, and 15672 available

## Quick Start

### 1. Build and Start All Services
```bash
cd /Users/i560653/Desktop/document-search
docker-compose up --build
```

### 2. Wait for Services to Start
The application will automatically wait for MySQL, RabbitMQ, and Elasticsearch to be healthy before starting.

### 3. Create a User Account (First Time)
```bash
# Signup for tenant1
curl -X POST http://localhost:8000/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "password": "password123",
    "email": "user1@tenant1.com",
    "tenantId": "tenant1",
    "role": "USER"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "user1",
  "tenantId": "tenant1",
  "role": "USER",
  "expiresIn": 86400000
}
```

### 4. Login (Subsequent Requests)
```bash
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "password": "password123"
  }'
```

### 5. Test the Document Upload Endpoint

#### Save your JWT token from signup/login response
```bash
TOKEN="your_jwt_token_here"
```

#### Upload a document (tenantId automatically extracted from token)
```bash
# Create a test file
echo "Hello World" > sample.txt

# Upload the document with authentication
curl -X POST http://localhost:8000/documents \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@sample.txt" \
  -v

# Expected Response: HTTP 202 with JSON containing document details
```

### 6. Search Documents
```bash
# Search documents (tenantId automatically from token)
curl -X GET "http://localhost:8000/search?q=hello" \
  -H "Authorization: Bearer $TOKEN"
```

## Verify Everything is Working

### Check Service Logs
```bash
# API Gateway logs
docker-compose logs -f api-gateway

# Document Management Service logs
docker-compose logs -f document-management-service

# Search Service logs
docker-compose logs -f document-search-service
```

### Check MySQL Database
```bash
# Connect to MySQL
docker exec -it document-mysql mysql -u docuser -pdocpassword document_management

# View users
SELECT id, username, email, tenant_id, role FROM users;

# View uploaded documents
SELECT * FROM documents;
```

### Check Elasticsearch
```bash
# Check Elasticsearch health
curl http://localhost:9200/_cluster/health

# View indexed documents
curl http://localhost:9200/documents/_search?pretty
```

### Check RabbitMQ Management UI
Open in browser: http://localhost:15672
- Username: `guest`
- Password: `guest`

### Check Document Storage
```bash
docker exec -it document-management-service ls -la /app/document-storage
```

## API Endpoints

### Authentication Endpoints (No Auth Required)

#### POST /api/auth/signup
Create a new user account.

**Request:**
```bash
curl -X POST http://localhost:8000/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "password": "password123",
    "email": "user1@example.com",
    "tenantId": "tenant1",
    "role": "USER"
  }'
```

#### POST /api/auth/login
Login and get JWT token.

**Request:**
```bash
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "password": "password123"
  }'
```

### Document Endpoints (Auth Required)

#### POST /documents
Upload a document for storage and indexing.

**Request:**
- Method: `POST`
- Header: `Authorization: Bearer {token}`
- Content-Type: `multipart/form-data`
- Parameter: `file` (multipart file)
- Note: tenantId is automatically extracted from JWT token

**Response:**
- Status: `202 ACCEPTED`
- Body:
```json
{
  "documentId": 1,
  "fileName": "sample.txt",
  "tenantId": "tenant1",
  "status": "UPLOADED",
  "uploadedAt": "2026-01-19T10:30:00",
  "message": "Document uploaded successfully and queued for indexing"
}
```

#### GET /search?q={query}
Search documents by content and metadata.

**Request:**
```bash
curl -X GET "http://localhost:8000/search?q=hello&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

**Response:**
```json
{
  "results": [
    {
      "documentId": 1,
      "fileName": "sample.txt",
      "content": "Hello World",
      "tenantId": "tenant1",
      "score": 0.95
    }
  ],
  "totalResults": 1,
  "page": 0,
  "size": 10
}
```

## Multi-Tenant Setup

### Create Users for Different Tenants
```bash
# Tenant 1 user
curl -X POST http://localhost:8000/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "pass123",
    "email": "alice@tenant1.com",
    "tenantId": "tenant1",
    "role": "USER"
  }'

# Tenant 2 user
curl -X POST http://localhost:8000/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "bob",
    "password": "pass123",
    "email": "bob@tenant2.com",
    "tenantId": "tenant2",
    "role": "USER"
  }'
```

Each user can only access documents from their own tenant!

## Stop Services
```bash
docker-compose down
```

## Stop and Remove All Data
```bash
docker-compose down -v
```

## Troubleshooting

### Service won't start
```bash
# Check logs
docker-compose logs mysql
docker-compose logs rabbitmq
docker-compose logs elasticsearch
docker-compose logs api-gateway
docker-compose logs document-management-service

# Restart services
docker-compose restart
```

### Authentication Issues
```bash
# Check if user exists in database
docker exec -it document-mysql mysql -u docuser -pdocpassword document_management \
  -e "SELECT username, tenant_id, role FROM users;"

# Check API Gateway logs
docker-compose logs api-gateway | grep -i error
```

### Port conflicts
Edit `docker-compose.yml` to change the port mappings if needed.

### Build issues
```bash
# Clean rebuild
docker-compose down -v
docker-compose build --no-cache
docker-compose up
```

### Docker Image Compatibility (Apple Silicon/ARM)
If you see errors like "no match for platform in manifest", ensure all Dockerfiles use:
- `FROM eclipse-temurin:17-jre` (NOT alpine variant)
- `FROM maven:3.9-eclipse-temurin-17` for build stage

## Security Notes

- JWT tokens expire after 24 hours
- Passwords are encrypted with BCrypt
- Each tenant's data is isolated
- Backend services are not exposed directly (only via API Gateway)
- All requests (except /api/auth/*) require valid JWT token
