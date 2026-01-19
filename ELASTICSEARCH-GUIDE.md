# Elasticsearch Integration Guide

## Overview
This document search system now uses Elasticsearch for indexing and searching documents based on both metadata and content.

## Architecture
- **MySQL**: Stores document metadata and file information
- **Elasticsearch**: Indexes document content and metadata for fast full-text search
- **RabbitMQ**: Message queue for asynchronous document processing
- **Document Management Service**: REST API for document upload
- **Indexer Worker**: Processes documents, extracts text using Apache Tika, and indexes to Elasticsearch

## What's Been Added

### 1. Elasticsearch Service (docker-compose.yml)
- Running on port 9200
- Configured with single-node setup for development
- Security disabled for simplicity
- Health check enabled

### 2. Elasticsearch Document Model
- **DocumentSearchIndex**: Elasticsearch entity with fields:
  - `id`: Document ID (from MySQL)
  - `fileName`: Searchable file name
  - `contentType`: Document MIME type
  - `fileSize`: File size in bytes
  - `content`: Full extracted text content (searchable)
  - `uploadedAt`, `indexedAt`: Timestamps
  - `status`: Document processing status
  - `filePath`: File storage path

### 3. Indexing Process
The indexer-worker now:
1. Receives document processing message from RabbitMQ
2. Extracts text content using Apache Tika
3. Saves extracted text to MySQL
4. **Indexes document to Elasticsearch** with metadata AND content

### 4. Search APIs
New REST endpoints on indexer-worker (port 8081):

#### Search by query (searches both filename and content)
```bash
GET http://localhost:8081/api/search?q=your_search_term
```

#### Search by filename only
```bash
GET http://localhost:8081/api/search/filename?name=report
```

#### Search by content only
```bash
GET http://localhost:8081/api/search/content?text=important
```

#### Search by content type
```bash
GET http://localhost:8081/api/search/type?contentType=application/pdf
```

#### Advanced search (multiple criteria)
```bash
GET http://localhost:8081/api/search/advanced?fileName=report&content=sales&contentType=application/pdf
```

#### Get document by ID
```bash
GET http://localhost:8081/api/search/document/{id}
```

## Running the System

### 1. Start all services
```bash
cd /Users/i560653/Desktop/document-search
docker-compose up --build
```

### 2. Verify services are running
```bash
docker-compose ps
```

All services should show as "healthy" or "running":
- mysql (port 3306)
- elasticsearch (port 9200)
- rabbitmq (port 5672, management UI on 15672)
- document-management-service (port 8080)
- indexer-worker (port 8081)

### 3. Check Elasticsearch is running
```bash
curl http://localhost:9200/_cluster/health
```

## Testing the Complete Flow

### Step 1: Upload a document
```bash
curl -X POST \
  http://localhost:8080/api/documents \
  -F "file=@/path/to/your/document.pdf"
```

Response:
```json
{
  "id": 1,
  "fileName": "document.pdf",
  "contentType": "application/pdf",
  "fileSize": 12345,
  "uploadedAt": "2026-01-17T10:30:00",
  "status": "UPLOADED",
  "message": "Document uploaded successfully and queued for indexing"
}
```

### Step 2: Wait for indexing to complete
The document will be automatically:
1. Picked up by RabbitMQ
2. Processed by indexer-worker
3. Text extracted using Apache Tika
4. Indexed to Elasticsearch

Check logs:
```bash
docker logs indexer-worker -f
```

### Step 3: Search for documents
```bash
# Search by any term (searches filename and content)
curl "http://localhost:8081/api/search?q=contract"

# Search by filename
curl "http://localhost:8081/api/search/filename?name=report"

# Search by content
curl "http://localhost:8081/api/search/content?text=important"

# Advanced search
curl "http://localhost:8081/api/search/advanced?fileName=2024&content=sales"
```

### Step 4: Verify data in Elasticsearch
```bash
# Check index exists
curl http://localhost:9200/documents

# Search all documents
curl "http://localhost:9200/documents/_search?pretty"

# Count documents
curl http://localhost:9200/documents/_count
```

## Example Test Scenarios

### Test 1: Upload and Search PDF
```bash
# Upload a PDF
curl -X POST http://localhost:8080/api/documents \
  -F "file=@sample.pdf"

# Wait 5-10 seconds for indexing

# Search by content
curl "http://localhost:8081/api/search?q=keyword_in_pdf"
```

### Test 2: Upload and Search Text File
```bash
# Upload a text file
curl -X POST http://localhost:8080/api/documents \
  -F "file=@sample.txt"

# Search by filename
curl "http://localhost:8081/api/search/filename?name=sample"
```

### Test 3: Upload Multiple Files and Search
```bash
# Upload multiple documents
curl -X POST http://localhost:8080/api/documents -F "file=@doc1.pdf"
curl -X POST http://localhost:8080/api/documents -F "file=@doc2.docx"
curl -X POST http://localhost:8080/api/documents -F "file=@doc3.txt"

# Search across all documents
curl "http://localhost:8081/api/search?q=common_term"
```

## Monitoring and Management

### RabbitMQ Management UI
- URL: http://localhost:15672
- Username: guest
- Password: guest

### Elasticsearch
- Health: http://localhost:9200/_cluster/health
- All indices: http://localhost:9200/_cat/indices?v
- Documents index: http://localhost:9200/documents/_search?pretty

### Check Docker logs
```bash
# All services
docker-compose logs -f

# Specific service
docker logs document-management-service -f
docker logs indexer-worker -f
docker logs document-elasticsearch -f
```

## Troubleshooting

### Elasticsearch not starting
- Increase Docker memory limit (Elasticsearch needs at least 2GB)
- Check logs: `docker logs document-elasticsearch`

### Documents not being indexed
1. Check RabbitMQ queue: http://localhost:15672
2. Check indexer-worker logs: `docker logs indexer-worker -f`
3. Verify file exists in shared volume: `docker exec indexer-worker ls -la /app/document-storage`

### Search not returning results
1. Verify document was indexed: `curl http://localhost:9200/documents/_search?pretty`
2. Check document status in MySQL: Document status should be "INDEXED"
3. Wait a few seconds after upload for indexing to complete

## Stopping the System
```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

## Configuration Files Modified
- `docker-compose.yml`: Added Elasticsearch service
- `indexer-worker/pom.xml`: Added Spring Data Elasticsearch dependency
- `indexer-worker/src/main/resources/application-docker.properties`: Added Elasticsearch connection
- New files created:
  - `DocumentSearchIndex.java`: Elasticsearch entity
  - `DocumentSearchRepository.java`: Elasticsearch repository
  - `DocumentSearchService.java`: Search service
  - `DocumentSearchController.java`: Search REST API
  - `elasticsearch-settings.json`: Elasticsearch analyzer configuration

