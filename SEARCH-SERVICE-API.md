# Document Search Service API

## Overview
The Document Search Service provides a powerful hybrid search endpoint that searches across both document metadata (filename) and content, with multi-tenancy support.

## Endpoint

### GET /api/search

Performs hybrid search across document metadata and content, filtered by tenant ID.

**URL:** `http://localhost:8082/api/search`

**Method:** `GET`

**Query Parameters:**
- `q` (optional): Search query string. Searches both filename and content fields.
- `tenant` (required): Tenant ID for multi-tenancy filtering.

**Response:** JSON array of search results

## Search Result Format

```json
[
  {
    "id": "1",
    "fileName": "document.pdf",
    "contentType": "application/pdf",
    "fileSize": 12345,
    "contentSnippet": "First 200 characters of content...",
    "uploadedAt": "2026-01-18T10:30:00",
    "indexedAt": "2026-01-18T10:30:15",
    "status": "INDEXED",
    "filePath": "/app/document-storage/document.pdf",
    "tenantId": "default",
    "score": 1.5
  }
]
```

## Usage Examples

### Example 1: Search by keyword
Search for documents containing "contract" in either filename or content:
```bash
curl "http://localhost:8082/api/search?q=contract&tenant=default"
```

### Example 2: Search by multiple words
```bash
curl "http://localhost:8082/api/search?q=annual%20report&tenant=default"
```

### Example 3: Get all documents for a tenant
When no query is provided, returns all documents for the tenant:
```bash
curl "http://localhost:8082/api/search?tenant=default"
```

### Example 4: Multi-tenant search
Search within a specific tenant's documents:
```bash
curl "http://localhost:8082/api/search?q=invoice&tenant=company-a"
curl "http://localhost:8082/api/search?q=invoice&tenant=company-b"
```

## How It Works

### Hybrid Search
The search performs a hybrid search that:
1. **Searches filename field**: Matches query against document filenames
2. **Searches content field**: Matches query against extracted document text
3. **Combines results**: Returns documents matching either filename OR content
4. **Filters by tenant**: Only returns documents belonging to the specified tenant
5. **Ranks by relevance**: Results include a relevance score

### Architecture
```
User Request
    ↓
document-search-service (port 8082)
    ↓
Elasticsearch (port 9200)
    ↓
Returns ranked results
```

### Data Flow
1. Document uploaded via `document-management-service` (port 8080)
2. Message sent to RabbitMQ
3. `indexer-worker` (port 8081) processes the document:
   - Extracts text using Apache Tika
   - Stores metadata in MySQL
   - Indexes to Elasticsearch with tenantId
4. `document-search-service` (port 8082) queries Elasticsearch for search requests

## Multi-Tenancy

The system supports multi-tenancy through the `tenantId` field:
- Each document is tagged with a `tenantId` during indexing
- Search endpoint requires a `tenant` parameter
- Results are automatically filtered by tenant
- Tenants cannot see each other's documents

**Default Tenant:** Documents indexed without a specific tenant use `tenantId="default"`

## Running the Service

### Start with Docker Compose
```bash
cd /Users/i560653/Desktop/document-search
docker-compose up --build
```

### Verify Service is Running
```bash
# Check service status
docker-compose ps

# Check service health
curl http://localhost:8082/actuator/health

# Test search endpoint
curl "http://localhost:8082/api/search?tenant=default"
```

## Complete End-to-End Test

### Step 1: Upload a document
```bash
curl -X POST http://localhost:8080/api/documents \
  -F "file=@sample.txt"
```

Response:
```json
{
  "id": 1,
  "fileName": "sample.txt",
  "status": "UPLOADED",
  "message": "Document uploaded successfully and queued for indexing"
}
```

### Step 2: Wait for indexing
Wait 5-10 seconds for the document to be processed and indexed to Elasticsearch.

Check indexing status:
```bash
docker logs indexer-worker -f
```

Look for log messages:
```
Successfully indexed document to Elasticsearch: 1
```

### Step 3: Search for the document
```bash
# Search by filename
curl "http://localhost:8082/api/search?q=sample&tenant=default"

# Search by content
curl "http://localhost:8082/api/search?q=keyword_from_file&tenant=default"

# Get all documents
curl "http://localhost:8082/api/search?tenant=default"
```

### Step 4: Verify in Elasticsearch
```bash
# Check document in Elasticsearch
curl "http://localhost:9200/documents/_search?q=tenantId:default&pretty"
```

## Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| document-management-service | 8080 | Upload documents |
| indexer-worker | 8081 | Process & index documents |
| **document-search-service** | **8082** | **Search documents** |
| Elasticsearch | 9200 | Data store |
| RabbitMQ | 5672 | Message queue |
| RabbitMQ Management | 15672 | Admin UI |
| MySQL | 3306 | Metadata storage |

## Error Responses

### Missing tenant parameter
```bash
curl "http://localhost:8082/api/search?q=test"
```
Response: `400 Bad Request`

### Empty tenant parameter
```bash
curl "http://localhost:8082/api/search?q=test&tenant="
```
Response: `400 Bad Request`

## Performance Considerations

- **Elasticsearch**: Handles full-text search efficiently
- **Relevance Scoring**: Results include relevance scores for ranking
- **Content Snippets**: Only first 200 characters returned for performance
- **Tenant Filtering**: Applied at query level for security and performance

## Integration with Other Services

### document-management-service (port 8080)
- Handles document uploads
- Stores files on filesystem
- Sends indexing messages to RabbitMQ

### indexer-worker (port 8081)
- Listens to RabbitMQ queue
- Extracts text using Apache Tika
- Indexes documents to Elasticsearch with tenantId
- Also provides search APIs (can use either service)

### document-search-service (port 8082)
- Dedicated search service
- Query-only (no write operations)
- Can be scaled independently
- Optimized for search performance

## Monitoring

### Check Elasticsearch Index
```bash
# View all documents
curl "http://localhost:9200/documents/_search?pretty"

# Count documents by tenant
curl "http://localhost:9200/documents/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "size": 0,
  "aggs": {
    "by_tenant": {
      "terms": { "field": "tenantId" }
    }
  }
}'
```

### Check Service Logs
```bash
# Search service logs
docker logs document-search-service -f

# All services
docker-compose logs -f
```

## Advanced Usage

### Future Enhancements
The service can be extended to support:
- Advanced filtering (by content type, date range, file size)
- Faceted search (aggregations by type, date, etc.)
- Fuzzy matching for typo tolerance
- Phrase matching for exact phrases
- Highlighting search terms in results
- Pagination for large result sets
- Sorting options (by date, relevance, filename)

## Troubleshooting

### No results returned
1. Check document was indexed: `curl "http://localhost:9200/documents/_search?pretty"`
2. Verify tenantId matches: Documents are indexed with `tenantId="default"`
3. Check search query matches content or filename
4. Wait for indexing to complete (5-10 seconds after upload)

### Service not starting
1. Check Elasticsearch is running: `docker logs document-elasticsearch`
2. Verify port 8082 is available: `lsof -i :8082`
3. Check service logs: `docker logs document-search-service`

### Connection errors
1. Ensure all services are in the same Docker network
2. Verify Elasticsearch health: `curl http://localhost:9200/_cluster/health`
3. Check docker-compose.yml configuration

