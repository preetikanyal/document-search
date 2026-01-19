# Document Search Service

## Overview

The Document Search Service is a comprehensive, production-ready microservice-based application designed for enterprise document management. It provides secure document upload, storage, indexing, and full-text search capabilities with multi-tenancy support. The system uses Elasticsearch for powerful full-text search, MySQL for metadata persistence, and RabbitMQ for reliable asynchronous processing.

### Key Features
- ✅ **Multi-tenant Architecture**: Complete data isolation per tenant
- ✅ **Asynchronous Processing**: Non-blocking document indexing via message queues
- ✅ **Full-Text Search**: Powerful search across document content and metadata
- ✅ **Hybrid Search**: Combines metadata filtering with content search
- ✅ **JWT Authentication**: Secure token-based authentication
- ✅ **RESTful APIs**: Clean, well-documented REST endpoints
- ✅ **Containerized Deployment**: Docker-based deployment for easy scaling
- ✅ **Multiple File Format Support**: PDF, DOC, DOCX, TXT, XLSX, PPT
- ✅ **Relevance Scoring**: Search results ranked by relevance

## Architecture

### System Components

The application follows a microservices architecture with the following components:

#### 1. **API Gateway** (Port 8000)
- **Purpose**: Single entry point for all client requests
- **Responsibilities**:
    - JWT token validation and authentication
    - Request routing to appropriate microservices
    - CORS handling
    - Rate limiting and security enforcement
- **Technology**: Spring Boot, Spring Security, JWT
- **Endpoints**: `/api/auth/*`, `/api/health`, proxies to other services

#### 2. **Document Management Service** (Port 8080)
- **Purpose**: Handles document upload and storage
- **Responsibilities**:
    - Receive and validate uploaded files
    - Store files on local filesystem with UUID naming
    - Save document metadata to MySQL database
    - Publish indexing messages to RabbitMQ
    - Enforce tenant-based access control
- **Technology**: Spring Boot, Spring Data JPA, MySQL
- **Storage**: Local filesystem at `/app/document-storage/`
- **Database**: MySQL table `documents` for metadata

#### 3. **Document Search Service** (Port 8082)
- **Purpose**: Provides search functionality across indexed documents
- **Responsibilities**:
    - Execute search queries against Elasticsearch
    - Filter results by tenant ID
    - Return relevance-scored results
    - Support multi-field search (filename, content)
- **Technology**: Spring Boot, Elasticsearch Java Client
- **Index**: Elasticsearch index `documents` with mappings for full-text search

#### 4. **Indexer Worker** (Port 8081)
- **Purpose**: Asynchronously index documents for search
- **Responsibilities**:
    - Consume messages from RabbitMQ queue
    - Extract text content from various file formats
    - Index content and metadata to Elasticsearch
    - Update document status in MySQL
    - Handle indexing failures with retry logic
- **Technology**: Spring Boot, Apache POI, Apache PDFBox, Elasticsearch
- **Queue**: RabbitMQ queue `document.indexing.queue`

#### 5. **MySQL Database** (Port 3306)
- **Purpose**: Persistent storage for structured data
- **Data Stored**:
    - User accounts and credentials
    - Document metadata (filename, file type, size, upload date)
    - Document status (UPLOADED, INDEXING, INDEXED, FAILED)
    - Tenant information
- **Schema**: Multiple tables with foreign key relationships

#### 6. **Elasticsearch** (Port 9200)
- **Purpose**: Full-text search engine
- **Features**:
    - Inverted index for fast text search
    - Relevance scoring and ranking
    - Multi-field search capabilities
    - Aggregations and filtering
- **Index Structure**: Documents indexed with fields: id, fileName, contentType, content, tenantId, uploadedAt, status

#### 7. **RabbitMQ** (Port 5672, Management: 15672)
- **Purpose**: Message broker for asynchronous processing
- **Features**:
    - Reliable message delivery
    - Message persistence
    - Dead letter queue for failed messages
    - Work queue pattern for load distribution
- **Queue**: `document.indexing.queue` with durable configuration

### Data Flow

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ 1. Upload Document (with JWT)
       ▼
┌─────────────────┐
│  API Gateway    │ ◄─── JWT Validation
│   (Port 8000)   │
└──────┬──────────┘
       │ 2. Route to Service
       ▼
┌─────────────────────────┐
│ Document Management     │
│   Service (Port 8080)   │
└──────┬─────────┬────────┘
       │         │
       │ 3. Save │ 4. Store File
       ▼         ▼
   ┌───────┐  ┌──────────────┐
   │ MySQL │  │  Filesystem  │
   └───────┘  └──────────────┘
       │
       │ 5. Publish Message
       ▼
   ┌──────────┐
   │ RabbitMQ │
   └─────┬────┘
         │ 6. Consume Message
         ▼
   ┌─────────────────┐
   │ Indexer Worker  │
   │  (Port 8081)    │
   └─────┬───────────┘
         │ 7. Extract & Index
         ▼
   ┌──────────────┐
   │ Elasticsearch│ ◄─── 8. Search Query
   │  (Port 9200) │
   └──────────────┘
         ▲
         │ 9. Search Request
         │
   ┌─────────────────────┐
   │ Document Search     │
   │ Service (Port 8082) │
   └─────────────────────┘
```

### Authentication Flow

```
1. User → API Gateway: POST /api/auth/signup (username, password, email, tenantId)
2. API Gateway → MySQL: Create user record with hashed password
3. API Gateway → User: Return JWT token

4. User → API Gateway: POST /api/auth/login (username, password)
5. API Gateway → MySQL: Verify credentials
6. API Gateway: Generate JWT token (includes username, tenantId, role)
7. API Gateway → User: Return JWT token

8. User → API Gateway: Any protected endpoint with "Authorization: Bearer <token>"
9. API Gateway: Validate JWT signature and expiration
10. API Gateway: Extract tenantId from token
11. API Gateway → Downstream Service: Forward request with user context
```

## Supported File Types

| File Type | Extension | Content Extraction | Library Used |
|-----------|-----------|-------------------|--------------|
| Plain Text | .txt | Full text | Native Java |
| PDF | .pdf | Text extraction | Apache PDFBox |
| Word | .doc, .docx | Text extraction | Apache POI |
| Excel | .xls, .xlsx | Cell content | Apache POI |
| PowerPoint | .ppt, .pptx | Slide text | Apache POI |

**Note**: Binary content (images, charts) within documents is not indexed.

## Prerequisites

### Required Software
- **Docker Desktop** 4.0+ (for macOS)
- **Docker Compose** 2.0+
- **Java 17+** (for local development only)
- **Maven 3.9+** (for local development only)
- **Minimum 4GB RAM** allocated to Docker
- **Minimum 10GB disk space**

### Port Requirements
Ensure the following ports are available:
- **8000**: API Gateway
- **8080**: Document Management Service
- **8081**: Indexer Worker
- **8082**: Document Search Service
- **3306**: MySQL
- **5672**: RabbitMQ (AMQP)
- **9200**: Elasticsearch (HTTP)
- **9300**: Elasticsearch (Transport)
- **15672**: RabbitMQ Management UI

## API Endpoints & Example CURLs

### Authentication Endpoints

#### 1. Sign Up (Register New User)
**POST /api/auth/signup**

Creates a new user account and returns a JWT token.

**Example curl:**
```sh
curl -v -X POST "http://localhost:8000/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "",
    "password": "",
    "email": "",
    "tenantId": "tenant1",
    "role": "USER"
  }'
```

**Success Response (HTTP 201):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiVVNFUiIsInRlbmFudElkIjoidGVuYW50MSIsInN1YiI6InRlc3R1c2VyMSIsImlhdCI6MTc2ODgxMzExMywiZXhwIjoxNzY4ODk5NTEzfQ.o8Gs9h4hxy91jQwgdq4XyidElnpDLyws9psU3qNQ52M",
  "type": "Bearer",
  "username": "",
  "tenantId": "tenant1",
  "role": "USER",
  "expiresIn": 86400000
}
```

**Failure Responses:**

| HTTP Code | Response | Reason |
|-----------|----------|--------|
| 400 | `{"error": "Username is already taken"}` | Username exists |
| 400 | `{"error": "Email is already registered"}` | Email exists |
| 400 | `{"error": "Username must be between 3 and 50 characters"}` | Validation failed |
| 400 | `{"error": "Password must be at least 6 characters"}` | Weak password |
| 400 | `{"error": "Email must be valid"}` | Invalid email format |

#### 2. Login (Get Authentication Token)
**POST /api/auth/login**

Authenticates a user and returns a JWT token.

**Example curl:**
```sh
curl -v -X POST "http://localhost:8000/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "",
    "password": ""
  }'
```

**Success Response (HTTP 200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiVVNFUiIsInRlbmFudElkIjoidGVuYW50MSIsInN1YiI6InRlc3R1c2VyMSIsImlhdCI6MTc2ODgxMzExMywiZXhwIjoxNzY4ODk5NTEzfQ.o8Gs9h4hxy91jQwgdq4XyidElnpDLyws9psU3qNQ52M",
  "type": "Bearer",
  "username": "",
  "tenantId": "tenant1",
  "role": "USER",
  "expiresIn": 86400000
}
```

**Failure Responses:**

| HTTP Code | Response | Reason |
|-----------|----------|--------|
| 401 | `{"error": "Invalid username or password"}` | Wrong credentials |
| 401 | `{"error": "Account is disabled"}` | User account disabled |

**Note:** Copy the token value from the response and use it in subsequent requests.

### System Endpoints

#### 3. Health Check
**GET /api/health**

Check if the API Gateway is running.

**Example:**
```sh
curl -X GET "http://localhost:8000/api/health"
```

**Success Response (HTTP 200):**
```json
{
  "status": "UP",
  "service": "API Gateway"
}
```

### Document Management Endpoints

#### 4. Upload Document
**POST /api/documents**

Uploads a document, stores it on the filesystem, saves metadata to MySQL, and queues it for indexing.

**Example curl:**
```sh
# Create a test file first
echo "This is a test document with searchable content about invoices and payments" > test-document.txt

# Upload it
curl -v -X POST "http://localhost:8000/api/documents" \
  -H "Authorization: Bearer <your_token>" \
  -F "file=@test-document.txt" \
  -F "tenantId=tenant1"
```

**Success Response (HTTP 202 Accepted):**
```json
{
  "documentId": 4,
  "fileName": "test-document.txt",
  "status": "UPLOADED",
  "uploadedAt": "2026-01-19T13:16:13.162377878",
  "message": "Document uploaded successfully and queued for indexing"
}
```

**Failure Responses:**

| HTTP Code | Response | Reason |
|-----------|----------|--------|
| 400 | `{"error": "File is required"}` | No file provided |
| 400 | `{"error": "File type not supported"}` | Unsupported format |
| 400 | `{"error": "File size exceeds maximum limit"}` | File too large |
| 403 | `{"error": "Tenant mismatch"}` | TenantId doesn't match user's tenant |
| 401 | `{"error": "Invalid or expired token"}` | Authentication failed |
| 413 | `{"error": "Payload too large"}` | Request size exceeds limit |

**Processing Flow:**
1. File received and validated (type, size)
2. Saved to filesystem with UUID-based name
3. Metadata saved to MySQL with status "UPLOADED"
4. Message published to RabbitMQ for indexing
5. Response returned immediately (async processing)
6. Indexer Worker picks up message and processes
7. Content extracted and indexed to Elasticsearch
8. Status updated to "INDEXED" in MySQL

### Search Endpoints

#### 5. Search Documents
**GET /api/search?q={query}&tenant={tenantId}**

Performs a full-text search across document metadata and content.

**Request Headers:**
- `Authorization: Bearer <your_token>` (Required)

**Query Parameters:**
- `q`: Search query (Required, minimum 1 character)
- `tenant`: Tenant ID for filtering (Required)
- `page`: Page number for pagination (Optional, default: 0)
- `size`: Results per page (Optional, default: 10, max: 100)

**Example - Basic Search:**
```sh
curl -v -X GET "http://localhost:8000/api/search?q=invoice&tenant=tenant1" \
  -H "Authorization: Bearer <your_token>"
```

**Example - Multi-word Search:**
```sh
curl -v -X GET "http://localhost:8000/api/search?q=invoice%20payment&tenant=tenant1" \
  -H "Authorization: Bearer <your_token>"
```

**Example - Paginated Search:**
```sh
curl -v -X GET "http://localhost:8000/api/search?q=report&tenant=tenant1&page=0&size=20" \
  -H "Authorization: Bearer <your_token>"
```

**Success Response (HTTP 200):**
```json
{
  "results": [
    {
      "id": "4",
      "fileName": "test-document.txt",
      "contentType": "text/plain",
      "fileSize": 125,
      "contentSnippet": "This is a test document about invoices and payments...",
      "uploadedAt": "2026-01-19T13:16:13.162",
      "indexedAt": "2026-01-19T13:16:13.191",
      "status": "INDEXED",
      "filePath": "/app/document-storage/14adf328-0302-4bd9-a29b-5278145f395f_test-document.txt",
      "tenantId": "tenant1",
      "score": 1.182321548461914
    }
  ],
  "totalResults": 1,
  "query": "invoice",
  "tenantId": "tenant1",
  "message": "Found 1 document(s) matching your query",
  "searchTimeMs": 45
}
```

**Empty Results (HTTP 200):**
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

**Failure Responses:**

| HTTP Code | Response | Reason |
|-----------|----------|--------|
| 400 | `{"error": "Query parameter 'q' is required"}` | Missing query |
| 400 | `{"error": "Tenant parameter is required"}` | Missing tenant |
| 403 | `{"error": "Access denied to tenant data"}` | Tenant mismatch |
| 500 | `{"error": "Search service unavailable"}` | Elasticsearch down |

**Search Features:**
- ✅ Full-text search across document content
- ✅ Filename matching
- ✅ Multi-field search with boosting
- ✅ Relevance scoring (higher score = better match)
- ✅ Tenant-based filtering (data isolation)
- ✅ Content snippets in results
- ✅ Fast performance (<100ms typical)

## Relevance Ranking and Scoring

### Understanding Relevance Scores

Every search result includes a **score** field that represents how relevant the document is to your search query. Higher scores indicate better matches. The system uses Elasticsearch's sophisticated relevance algorithms to ensure the most pertinent documents appear first.

**Example Search Result with Score:**
```json
{
  "id": "4",
  "fileName": "financial-report-2025.txt",
  "contentType": "text/plain",
  "contentSnippet": "Annual Financial Report 2025 - Revenue grew...",
  "score": 2.4563891,
  "uploadedAt": "2026-01-19T13:16:13.162",
  "status": "INDEXED",
  "tenantId": "tenant1"
}
```

### How Relevance Scoring Works

The system uses **Elasticsearch's BM25 algorithm** (Best Matching 25), which is the industry-standard for relevance ranking. BM25 is an improvement over the classic TF-IDF (Term Frequency-Inverse Document Frequency) algorithm.

#### Key Factors in Score Calculation

1. **Term Frequency (TF)**: How often the search term appears in the document
    - Documents with more occurrences of your search terms rank higher
    - Example: A document mentioning "invoice" 5 times scores higher than one mentioning it once

2. **Inverse Document Frequency (IDF)**: How rare the term is across all documents
    - Rare terms contribute more to the score than common terms
    - Example: "Q4-revenue" is more valuable than common words like "report"

3. **Field Length Normalization**: Shorter documents with term matches rank higher
    - Prevents long documents from dominating results simply by having more words
    - A 100-word document with 3 matches may score higher than a 10,000-word document with 5 matches

4. **Field Boosting**: Some fields are weighted more heavily than others
    - **fileName**: Boosted by 2x (matches in filenames are more significant)
    - **content**: Standard weight (matches in document body)
    - This means finding "invoice" in the filename counts twice as much as in the content

5. **Saturation**: Diminishing returns for repeated terms
    - BM25 prevents score inflation from excessive term repetition
    - 10 occurrences doesn't score 10x higher than 1 occurrence

### Interpreting Score Values

| Score Range | Interpretation | Meaning |
|-------------|---------------|---------|
| **3.0+** | Excellent match | Query terms appear multiple times, likely in filename and content |
| **1.5 - 3.0** | Good match | Query terms present in important fields with good frequency |
| **0.5 - 1.5** | Moderate match | Query terms appear but less prominently |
| **< 0.5** | Weak match | Query terms barely present or very common across all documents |

**Note**: Absolute score values are relative and depend on your document corpus. Compare scores within the same search result set.

### Multi-Term Query Scoring

When you search for multiple terms (e.g., "financial report"), the scoring behavior depends on the query type:

**Example: Searching "financial report"**
```sh
curl -X GET "http://localhost:8000/api/search?q=financial%20report&tenant=tenant1" \
  -H "Authorization: Bearer $TOKEN"
```

Elasticsearch uses **OR logic by default**:
- Documents containing "financial" OR "report" are returned
- Documents with BOTH terms score higher
- Term proximity matters (terms closer together = higher score)

**Scoring breakdown for "financial report":**
- Document A: Contains both "financial" and "report" (5-10 words apart) → **Score: 2.8**
- Document B: Contains "financial" multiple times, no "report" → **Score: 1.4**
- Document C: Contains "report" once, no "financial" → **Score: 0.9**
- Document D: Contains both terms in the filename → **Score: 4.2** (field boost applied)

### Field-Specific Boosting Strategy

The search implementation applies different weights to different fields:

```
fileName^2.0        → 200% weight (2x importance)
content^1.0         → 100% weight (baseline)
```

**Real-World Example:**

Search query: "invoice"

| Document | Filename | Content Contains "invoice" | Score | Explanation |
|----------|----------|----------------------------|-------|-------------|
| Doc A | `invoice-march.pdf` | Yes, 3 times | **3.5** | Filename match + content matches |
| Doc B | `monthly-report.pdf` | Yes, 5 times | **2.1** | Only content matches (more occurrences) |
| Doc C | `invoice-template.docx` | No | **1.8** | Strong filename match, no content |
| Doc D | `expenses.xlsx` | Yes, 1 time | **0.8** | Only one content mention |

**Observation**: Doc A ranks highest despite Doc B having more occurrences, because the filename match is heavily weighted.

### Practical Ranking Examples

#### Example 1: Exact Filename Match
```sh
# Upload a document named "quarterly-invoice-summary.txt"
# Search for "invoice"

Result:
{
  "fileName": "quarterly-invoice-summary.txt",
  "score": 3.2,  # High score due to filename match
  "contentSnippet": "Total invoices processed: 450"
}
```

#### Example 2: Content-Only Match
```sh
# Document: "financial-data.txt" containing "invoice" 8 times
# Search for "invoice"

Result:
{
  "fileName": "financial-data.txt",
  "score": 2.1,  # Good score from content frequency
  "contentSnippet": "Invoice processing... invoice reconciliation... invoices paid..."
}
```

#### Example 3: Multi-Term Relevance
```sh
# Search for "employee vacation policy"
# Three documents returned

Results sorted by score:
1. "employee-handbook.pdf" (contains all 3 terms) → score: 4.1
2. "vacation-policy.docx" (contains 2 terms) → score: 2.7
3. "hr-policies.txt" (contains 1 term: "policy") → score: 0.9
```

### Factors That Affect Ranking

#### Positive Factors (Increase Score)
✅ **Query term in filename**: Strong positive signal (2x boost)
✅ **Multiple query term occurrences**: More mentions = higher score (with diminishing returns)
✅ **All query terms present**: Documents matching all search terms rank higher
✅ **Terms close together**: Proximity matters for multi-term queries
✅ **Rare/unique terms**: Uncommon words contribute more to relevance
✅ **Shorter documents**: Better score normalization for concise content

#### Negative Factors (Decrease Score)
❌ **Very common terms**: Words appearing in most documents (e.g., "document", "file")
❌ **Very long documents**: Score dilution from excessive content
❌ **Missing query terms**: Partial matches score lower
❌ **Terms far apart**: Multi-term queries benefit from proximity

### Optimizing Search Relevance

#### For Better Search Results (User Tips)

1. **Use specific terms**: "Q4-revenue-report" beats "report"
2. **Include filename keywords**: If you remember filename details, include them
3. **Use multiple relevant terms**: "financial invoice payment" better than just "financial"
4. **Avoid very common words**: Skip terms like "document", "file", "the"
5. **Use exact phrases** (when searching by filename): More specific queries yield better results

## Viewing Logs

### Real-time Logs
```sh
# View all logs in real-time
docker-compose logs -f

# View specific service logs
docker-compose logs -f api-gateway
docker-compose logs -f document-management-service
docker-compose logs -f document-search-service
docker-compose logs -f indexer-worker

# View last 100 lines
docker-compose logs --tail=100 api-gateway

# View logs with timestamps
docker-compose logs -f -t api-gateway
```

### Log Analysis
```sh
# Search for errors
docker-compose logs | grep ERROR

# Search for specific document ID
docker-compose logs | grep "documentId: 123"

# Export logs to file
docker-compose logs > all-services.log

# View Elasticsearch logs
docker-compose logs elasticsearch | tail -50
```

## Monitoring and Management

### RabbitMQ Management UI
- **URL**: http://localhost:15672
- **Username**: guest
- **Password**: guest
- **Features**:
    - View queues and message counts
    - Monitor message rates
    - Manage exchanges and bindings
    - View connection status

### Elasticsearch Monitoring
```sh
# Check cluster health
curl http://localhost:9200/_cluster/health?pretty

# View index statistics
curl http://localhost:9200/documents/_stats?pretty

# Count indexed documents
curl http://localhost:9200/documents/_count?pretty

# View index mapping
curl http://localhost:9200/documents/_mapping?pretty

# Search directly (bypass API)
curl http://localhost:9200/documents/_search?q=test&pretty
```

### MySQL Monitoring
```sh
# Connect to MySQL
docker exec -it document-mysql mysql -u root -prootpassword

# View databases
SHOW DATABASES;

# Use document database
USE document_search;

# View tables
SHOW TABLES;

# Check document count
SELECT COUNT(*) FROM documents;

# View recent uploads
SELECT id, file_name, status, uploaded_at FROM documents ORDER BY uploaded_at DESC LIMIT 10;

# Check users
SELECT username, email, tenant_id FROM users;
```

## Stopping Services

```sh
# Stop all services (preserves data)
docker-compose down

# Stop and remove all data (volumes)
docker-compose down -v

# Stop specific service
docker-compose stop api-gateway

# Remove all containers and networks
docker-compose down --remove-orphans

# Full cleanup (including images)
docker-compose down -v --rmi all
```

## Development and Debugging

### Local Development Setup
```sh
# Build only (without starting)
docker-compose build

# Start specific service
docker-compose up api-gateway

# Run with custom profile
docker-compose --profile dev up

# Override environment variables
SPRING_PROFILES_ACTIVE=dev docker-compose up
```

### Debugging
```sh
# Access container shell
docker exec -it api-gateway /bin/sh

# View container logs
docker logs api-gateway --follow

# Inspect container
docker inspect api-gateway

# Check environment variables
docker exec api-gateway env

# Network debugging
docker network ls
docker network inspect document-search_default
```

#### Production-Readiness Analysis

### Scalability
- [ ] Deploy to Kubernetes with HPA
- [ ] Set up Elasticsearch cluster (6+ nodes)
- [ ] Implement MySQL read replicas
- [ ] Migrate to S3/GCS for file storage
- [ ] Deploy RabbitMQ cluster
- [ ] Set up Redis caching layer
- [ ] Configure load balancers

### Resilience
- [ ] Implement circuit breakers (Resilience4j)
- [ ] Add retry logic with exponential backoff
- [ ] Configure health checks and probes
- [ ] Set up graceful shutdown
- [ ] Implement dead letter queues
- [ ] Enable database failover
- [ ] Add chaos engineering tests

### Security
- [ ] Implement OAuth 2.0 / OIDC
- [ ] Enable encryption at rest (database, S3, ES)
- [ ] Enable TLS for all services
- [ ] Implement rate limiting
- [ ] Add comprehensive audit logging
- [ ] Integrate secrets management (Vault/AWS Secrets)
- [ ] Enable DDoS protection (WAF)
- [ ] Conduct security penetration testing
- [ ] Implement RBAC with fine-grained permissions

### Observability
- [ ] Set up Prometheus + Grafana
- [ ] Deploy ELK stack for logging
- [ ] Implement distributed tracing (Jaeger)
- [ ] Configure alerting (PagerDuty)
- [ ] Create runbooks for incidents

### Documentation
- [ ] API documentation (OpenAPI/Swagger)
- [ ] Architecture diagrams
- [ ] Runbooks for operations
- [ ] Disaster recovery procedures
- [ ] Security policies and compliance docs

---
**Last Updated**: January 19, 2026
**Version**: 1.0.0
