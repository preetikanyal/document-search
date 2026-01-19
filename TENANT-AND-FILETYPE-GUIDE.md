# Tenant-Based Multi-Tenancy and File Type Support Guide

## Summary of Changes

### 1. **File Type Support**
Added `fileType` field to track the file extension (pdf, doc, docx, txt, etc.) separately from `contentType` (MIME type).

### 2. **Tenant-Based Multi-Tenancy**
All operations (upload, indexing, searching) now support `tenantId` for data isolation between different tenants.

### 3. **Content Storage Strategy**
- **MySQL**: Stores only metadata (fileName, filePath, contentType, fileType, fileSize, tenantId, status, timestamps)
- **Elasticsearch**: Stores full document content for fast full-text search
- **Removed**: `extractedText` field from MySQL (no longer redundantly storing content in both places)

---

## Supported File Types

### Apache Tika 2.9.1 supports 100+ file formats:

#### Documents
- **PDF**: `.pdf` - Portable Document Format
- **Microsoft Word**: `.doc`, `.docx`, `.dot`, `.dotx`
- **OpenDocument Text**: `.odt`, `.ott`
- **Rich Text Format**: `.rtf`
- **Plain Text**: `.txt`

#### Spreadsheets
- **Microsoft Excel**: `.xls`, `.xlsx`, `.xlsm`, `.xltx`
- **OpenDocument Spreadsheet**: `.ods`, `.ots`
- **CSV**: `.csv`

#### Presentations
- **Microsoft PowerPoint**: `.ppt`, `.pptx`, `.pptm`, `.potx`
- **OpenDocument Presentation**: `.odp`, `.otp`

#### Images (with OCR capability when configured)
- **JPEG**: `.jpg`, `.jpeg`
- **PNG**: `.png`
- **TIFF**: `.tiff`, `.tif`
- **GIF**: `.gif`
- **BMP**: `.bmp`

#### Web & Markup
- **HTML**: `.html`, `.htm`
- **XML**: `.xml`
- **JSON**: `.json`

#### Archives
- **ZIP**: `.zip`
- **TAR**: `.tar`, `.tar.gz`
- **RAR**: `.rar`
- **7-Zip**: `.7z`

#### Email
- **Outlook**: `.msg`, `.pst`
- **Email**: `.eml`

#### Other Formats
- **Markdown**: `.md`
- **iWork Pages**: `.pages`
- **iWork Numbers**: `.numbers`
- **iWork Keynote**: `.key`
- **Visio**: `.vsd`, `.vsdx`
- **AutoCAD**: `.dwg`

**Note**: Text extraction quality varies by format. Binary formats require their respective parsers in the Tika library.

---

## API Changes

### Upload Document (Document Management Service - Port 8080)

**Endpoint**: `POST /documents`

**Parameters**:
- `file` (required): The file to upload
- `tenantId` (optional, default: "default"): Tenant identifier

**Example**:
```bash
# Upload with default tenant
curl -X POST http://localhost:8080/documents \
  -F "file=@document.pdf"

# Upload with specific tenant
curl -X POST http://localhost:8080/documents \
  -F "file=@document.pdf" \
  -F "tenantId=tenant1"
```

---

### Search Documents (Indexer Worker - Port 8081)

All search endpoints now require or accept `tenantId` parameter for multi-tenant isolation.

#### 1. General Search (searches both filename and content)
```bash
GET /api/search?q={query}&tenantId={tenantId}

# Example
curl "http://localhost:8081/api/search?q=contract&tenantId=tenant1"
```

#### 2. Search by File Name
```bash
GET /api/search/filename?name={filename}&tenantId={tenantId}

# Example
curl "http://localhost:8081/api/search/filename?name=report&tenantId=tenant1"
```

#### 3. Search by Content
```bash
GET /api/search/content?text={searchText}&tenantId={tenantId}

# Example
curl "http://localhost:8081/api/search/content?text=important&tenantId=tenant1"
```

#### 4. Search by Content Type (MIME type)
```bash
GET /api/search/type?contentType={mimeType}&tenantId={tenantId}

# Example
curl "http://localhost:8081/api/search/type?contentType=application/pdf&tenantId=tenant1"
```

#### 5. **NEW** - Search by File Type (extension)
```bash
GET /api/search/filetype?fileType={extension}&tenantId={tenantId}

# Example - find all PDFs
curl "http://localhost:8081/api/search/filetype?fileType=pdf&tenantId=tenant1"

# Example - find all Excel files
curl "http://localhost:8081/api/search/filetype?fileType=xlsx&tenantId=tenant1"
```

#### 6. **NEW** - Get All Documents for a Tenant
```bash
GET /api/search/tenant?tenantId={tenantId}

# Example
curl "http://localhost:8081/api/search/tenant?tenantId=tenant1"
```

#### 7. Advanced Search (multiple criteria)
```bash
GET /api/search/advanced?fileName={name}&content={text}&contentType={mime}&fileType={ext}&tenantId={tenantId}

# Example - find PDFs containing "sales" in 2024
curl "http://localhost:8081/api/search/advanced?fileName=2024&content=sales&fileType=pdf&tenantId=tenant1"
```

#### 8. Get Document by ID
```bash
GET /api/search/document/{id}?tenantId={tenantId}

# Example
curl "http://localhost:8081/api/search/document/1?tenantId=tenant1"
```

**Security Note**: Document access is validated against tenant ownership. Users can only access documents belonging to their tenant.

---

## Database Schema Changes

### MySQL `documents` table now includes:

```sql
CREATE TABLE documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_type VARCHAR(50),              -- NEW: pdf, docx, txt, etc.
    file_size BIGINT NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,    -- NEW: tenant identifier
    uploaded_at DATETIME NOT NULL,
    status VARCHAR(50),
    indexed_at DATETIME,
    -- REMOVED: extracted_text column
);
```

### Elasticsearch `documents` index includes:

```json
{
  "id": "1",
  "fileName": "report.pdf",
  "contentType": "application/pdf",
  "fileType": "pdf",                    // NEW
  "fileSize": 1024000,
  "tenantId": "tenant1",                // ALREADY EXISTED, NOW USED
  "content": "Full extracted text...",  // ONLY stored here, not in MySQL
  "uploadedAt": "2026-01-18T10:00:00",
  "indexedAt": "2026-01-18T10:00:05",
  "status": "INDEXED",
  "filePath": "/app/document-storage/uuid_report.pdf"
}
```

---

## Multi-Tenant Isolation

### How it Works:
1. **Upload**: Documents are tagged with `tenantId` when uploaded
2. **Indexing**: `tenantId` is indexed in Elasticsearch for fast filtering
3. **Searching**: All searches are filtered by `tenantId` automatically
4. **Security**: Users can only access documents belonging to their tenant

### Benefits:
- **Data Isolation**: Tenants cannot see each other's documents
- **Shared Infrastructure**: All tenants share the same Elasticsearch cluster and MySQL database
- **Performance**: Elasticsearch efficiently filters by tenant using keyword fields
- **Scalability**: Can support thousands of tenants

---

## Testing Examples

### Test 1: Upload documents for different tenants
```bash
# Upload for tenant1
curl -X POST http://localhost:8080/documents \
  -F "file=@contract.pdf" \
  -F "tenantId=tenant1"

# Upload for tenant2
curl -X POST http://localhost:8080/documents \
  -F "file=@invoice.pdf" \
  -F "tenantId=tenant2"
```

### Test 2: Search within tenant boundaries
```bash
# Tenant1 searches (will only see their contract.pdf)
curl "http://localhost:8081/api/search?q=contract&tenantId=tenant1"

# Tenant2 searches (will only see their invoice.pdf)
curl "http://localhost:8081/api/search?q=invoice&tenantId=tenant2"
```

### Test 3: Search by file type
```bash
# Find all PDFs for tenant1
curl "http://localhost:8081/api/search/filetype?fileType=pdf&tenantId=tenant1"

# Find all Word documents for tenant2
curl "http://localhost:8081/api/search/filetype?fileType=docx&tenantId=tenant2"
```

### Test 4: Advanced search with multiple filters
```bash
# Find Excel files from 2024 containing "budget" for tenant1
curl "http://localhost:8081/api/search/advanced?fileName=2024&content=budget&fileType=xlsx&tenantId=tenant1"
```

---

## Migration Notes

### If you have existing data:

1. **Add columns to MySQL**:
```sql
ALTER TABLE documents ADD COLUMN file_type VARCHAR(50);
ALTER TABLE documents ADD COLUMN tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE documents DROP COLUMN extracted_text;
```

2. **Update existing fileType values**:
```sql
UPDATE documents 
SET file_type = LOWER(SUBSTRING_INDEX(file_name, '.', -1))
WHERE file_type IS NULL;
```

3. **Re-index existing documents**: Elasticsearch will automatically get the new fields when re-indexing.

---

## Content Storage Best Practices

### Why content is NOT stored in MySQL:

1. **Performance**: MySQL is not optimized for full-text search on large text fields
2. **Scalability**: Storing large text in MySQL increases database size and slows queries
3. **Search Speed**: Elasticsearch is purpose-built for full-text search (100x faster)
4. **Storage Efficiency**: Elasticsearch compresses text content efficiently
5. **Query Flexibility**: Elasticsearch provides advanced search features (fuzzy, proximity, highlighting)

### Data Flow:
```
File Upload → MySQL (metadata) → RabbitMQ → Indexer Worker
                                              ↓
                                    Apache Tika (extract text)
                                              ↓
                                    Elasticsearch (full content)
```

---

## Performance Considerations

1. **Tenant Filtering**: Using `tenantId` as a Keyword field ensures fast filtering
2. **Index Size**: Large documents increase Elasticsearch index size
3. **Search Speed**: Searching across millions of documents per tenant is still fast
4. **Memory**: Elasticsearch requires adequate heap memory (recommended: 2GB minimum)

---

## Security Recommendations

1. **Production**: Implement proper authentication and tenant validation at API gateway level
2. **Tenant Validation**: Validate tenantId against authenticated user's organization
3. **Elasticsearch Security**: Enable Elasticsearch security features in production
4. **File Access**: Validate tenant ownership before allowing file downloads
5. **Audit Logging**: Log all tenant access attempts for compliance

---

## Troubleshooting

### Issue: TenantId not being set
**Solution**: Ensure tenantId is passed in upload request, or default value is used

### Issue: Search returns documents from other tenants
**Solution**: Check that tenantId parameter is being passed correctly in search requests

### Issue: FileType showing as "unknown"
**Solution**: Ensure files have proper extensions when uploading

### Issue: No search results
**Solution**: 
1. Verify document was indexed: Check Elasticsearch directly
2. Confirm tenantId matches between upload and search
3. Wait a few seconds after upload for indexing to complete

