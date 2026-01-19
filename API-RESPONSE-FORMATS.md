# API Response Formats - Document Management System

## Overview
All REST API endpoints now return structured JSON responses with clear success and failure messages, HTTP status codes, and detailed error information.

---

## 📤 POST /api/documents (Upload Document)

### Success Response (HTTP 202 Accepted)
```json
{
  "documentId": 123,
  "fileName": "example.pdf",
  "status": "UPLOADED",
  "uploadedAt": "2026-01-19T10:30:45.123",
  "message": "Document uploaded successfully and queued for indexing"
}
```

### Error Responses

#### Empty File (HTTP 400)
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "File is required and cannot be empty",
  "timestamp": "2026-01-19T10:30:45.123",
  "path": "/api/documents"
}
```

#### File Too Large (HTTP 413)
```json
{
  "status": 413,
  "error": "Payload Too Large",
  "message": "File size exceeds maximum allowed size of 50MB",
  "timestamp": "2026-01-19T10:30:45.123",
  "path": "/api/documents"
}
```

#### Invalid Filename (HTTP 400)
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "File must have a valid filename",
  "timestamp": "2026-01-19T10:30:45.123",
  "path": "/api/documents"
}
```

#### Server Error (HTTP 500)
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to upload document: [error details]",
  "timestamp": "2026-01-19T10:30:45.123",
  "path": "/api/documents"
}
```

---

## 🔍 GET /api/search?q={query}

### Success Response (HTTP 200 OK)
```json
{
  "results": [
    {
      "documentId": 123,
      "fileName": "example.pdf",
      "content": "This is the document content...",
      "tenantId": "tenant1",
      "fileType": "pdf",
      "uploadedAt": "2026-01-19T10:30:45.123",
      "score": 0.95
    },
    {
      "documentId": 124,
      "fileName": "report.docx",
      "content": "Another matching document...",
      "tenantId": "tenant1",
      "fileType": "docx",
      "uploadedAt": "2026-01-19T09:15:30.456",
      "score": 0.82
    }
  ],
  "totalResults": 2,
  "query": "example search",
  "tenantId": "tenant1",
  "message": "Found 2 document(s) matching your query",
  "searchTimeMs": 45
}
```

### Success Response - No Results (HTTP 200 OK)
```json
{
  "results": [],
  "totalResults": 0,
  "query": "nonexistent",
  "tenantId": "tenant1",
  "message": "No documents found matching your query",
  "searchTimeMs": 12
}
```

### Error Responses

#### Missing Query Parameter (HTTP 400)
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Search query 'q' is required and cannot be empty",
  "timestamp": "2026-01-19T10:30:45.123",
  "path": "/api/search"
}
```

#### Query Too Long (HTTP 400)
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Search query is too long. Maximum allowed length is 500 characters.",
  "timestamp": "2026-01-19T10:30:45.123",
  "path": "/api/search"
}
```

#### Server Error (HTTP 500)
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to perform search: [error details]",
  "timestamp": "2026-01-19T10:30:45.123",
  "path": "/api/search"
}
```

---

## 🔐 Authentication Errors

### Missing or Invalid JWT Token (HTTP 403)
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. Valid JWT token required.",
  "timestamp": "2026-01-19T10:30:45.123",
  "path": "/api/documents"
}
```

### Expired Token (HTTP 401)
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token has expired",
  "timestamp": "2026-01-19T10:30:45.123",
  "path": "/api/documents"
}
```

---

## Testing Examples

### Test Document Upload with curl

#### Successful Upload
```bash
TOKEN="your_jwt_token_here"

curl -v -X POST http://localhost:8000/api/documents \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@sample.txt"

# Expected Output:
# < HTTP/1.1 202 Accepted
# {
#   "documentId": 1,
#   "fileName": "sample.txt",
#   "status": "UPLOADED",
#   "uploadedAt": "2026-01-19T10:30:45.123",
#   "message": "Document uploaded successfully and queued for indexing"
# }
```

#### Empty File Error
```bash
curl -v -X POST http://localhost:8000/api/documents \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/dev/null"

# Expected Output:
# < HTTP/1.1 400 Bad Request
# {
#   "status": 400,
#   "error": "Bad Request",
#   "message": "File is required and cannot be empty",
#   ...
# }
```

### Test Search with curl

#### Successful Search
```bash
TOKEN="your_jwt_token_here"

curl -v -X GET "http://localhost:8000/api/search?q=test" \
  -H "Authorization: Bearer $TOKEN"

# Expected Output:
# < HTTP/1.1 200 OK
# {
#   "results": [...],
#   "totalResults": 2,
#   "query": "test",
#   "tenantId": "tenant1",
#   "message": "Found 2 document(s) matching your query",
#   "searchTimeMs": 45
# }
```

#### Missing Query Parameter
```bash
curl -v -X GET "http://localhost:8000/api/search" \
  -H "Authorization: Bearer $TOKEN"

# Expected Output:
# < HTTP/1.1 400 Bad Request
# {
#   "status": 400,
#   "error": "Bad Request",
#   "message": "Search query 'q' is required and cannot be empty",
#   ...
# }
```

---

## HTTP Status Codes Reference

### Success Codes
- **200 OK** - Search completed successfully
- **202 Accepted** - Document upload accepted and queued for processing

### Client Error Codes
- **400 Bad Request** - Invalid request parameters or missing required fields
- **401 Unauthorized** - Authentication required or token expired
- **403 Forbidden** - Valid authentication but insufficient permissions
- **413 Payload Too Large** - File size exceeds maximum limit (50MB)

### Server Error Codes
- **500 Internal Server Error** - Unexpected server error
- **503 Service Unavailable** - Backend service temporarily unavailable

---

## Response Fields Explained

### Document Upload Response
- `documentId` - Unique identifier for the uploaded document
- `fileName` - Original filename of the uploaded document
- `status` - Current status (e.g., "UPLOADED", "PROCESSING", "INDEXED")
- `uploadedAt` - Timestamp when document was uploaded
- `message` - Human-readable status message

### Search Response
- `results` - Array of matching documents with metadata and content snippets
- `totalResults` - Total number of documents found
- `query` - The search query that was executed
- `tenantId` - Tenant ID for which search was performed
- `message` - Human-readable result summary
- `searchTimeMs` - Time taken to perform the search (in milliseconds)

### Error Response
- `status` - HTTP status code
- `error` - HTTP status text (e.g., "Bad Request", "Internal Server Error")
- `message` - Detailed error message explaining what went wrong
- `timestamp` - When the error occurred
- `path` - API endpoint where the error occurred

---

## Validation Rules

### Document Upload
- ✅ File is required and cannot be empty
- ✅ Maximum file size: 50MB
- ✅ File must have a valid filename
- ✅ Valid JWT token with tenantId required

### Search
- ✅ Query parameter 'q' is required
- ✅ Maximum query length: 500 characters
- ✅ Valid JWT token with tenantId required

---

## Logging

All endpoints now include detailed logging at multiple levels:

### API Gateway Logs
```
[API Gateway] User 'john' from tenant 'tenant1' uploading document
[API Gateway] Forwarding document 'report.pdf' (2048576 bytes) to document-management-service
[API Gateway] Document upload successful for user: john
```

### Backend Service Logs
```
Processing document upload: report.pdf (2048576 bytes) for tenant: tenant1
Document uploaded successfully: ID=123, fileName=report.pdf, tenant=tenant1
```

### Error Logs
```
[API Gateway] Empty file upload attempt by user: john
Error processing document upload for tenant tenant1: File size exceeds limit
```

Check logs using:
```bash
# API Gateway logs
docker-compose logs -f api-gateway

# Document Management Service logs
docker-compose logs -f document-management-service

# Search Service logs
docker-compose logs -f document-search-service
```

