# Indexer Worker Service

## Overview
The Indexer Worker is a Spring Boot microservice that consumes document indexing messages from RabbitMQ and processes documents by extracting their text content using Apache Tika. It runs as a separate service that scales independently from the document management service.

## Features
- **Asynchronous Processing**: Consumes messages from RabbitMQ queue
- **Text Extraction**: Uses Apache Tika to extract text from various document formats (PDF, Word, Excel, etc.)
- **Database Updates**: Updates document status and stores extracted text in MySQL
- **Error Handling**: Marks documents as FAILED if processing errors occur
- **Scalable**: Can run multiple instances to process documents in parallel

## Architecture
```
Document Management Service → RabbitMQ Queue → Indexer Worker → MySQL
                                    ↓
                        document.index.queue
```

## Message Format
The worker listens for `DocumentIndexMessage` on the `document.index.queue`:

```json
{
  "documentId": 1,
  "fileName": "sample.pdf",
  "filePath": "/app/document-storage/1234567890_sample.pdf",
  "contentType": "application/pdf",
  "fileSize": 102400,
  "uploadedAt": "2026-01-17T10:30:00"
}
```

## Document Processing Flow
1. Receive message from RabbitMQ queue
2. Fetch document from database by ID
3. Update status to `PROCESSING`
4. Read file from shared storage
5. Extract text using Apache Tika
6. Save extracted text to database
7. Update status to `INDEXED` with timestamp
8. If error occurs, mark status as `FAILED`

## Supported Document Formats
Apache Tika supports many formats including:
- PDF
- Microsoft Office (Word, Excel, PowerPoint)
- OpenOffice/LibreOffice
- Plain text
- HTML
- XML
- And many more...

## Configuration

### application.properties (Local Development)
```properties
spring.application.name=indexer-worker
server.port=8081

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/document_management
spring.datasource.username=docuser
spring.datasource.password=docpassword

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

### application-docker.properties (Docker)
```properties
spring.datasource.url=jdbc:mysql://mysql:3306/document_management
spring.rabbitmq.host=rabbitmq
document.storage.path=/app/document-storage
```

## Running the Service

### Local Development
```bash
cd indexer-worker
mvn spring-boot:run
```

### Docker
The service is included in the docker-compose setup:
```bash
docker-compose up --build
```

The indexer-worker will:
- Connect to MySQL at `mysql:3306`
- Connect to RabbitMQ at `rabbitmq:5672`
- Share the document storage volume with document-management-service
- Run on port 8081

## Monitoring

### Check Logs
```bash
# Docker
docker logs -f indexer-worker

# View processing activity
docker logs indexer-worker | grep "Received message"
```

### Check Database
```bash
docker exec -it document-mysql mysql -u docuser -pdocpassword document_management

# View indexed documents
SELECT id, fileName, status, indexedAt, LENGTH(extractedText) as text_length 
FROM documents 
WHERE status = 'INDEXED';
```

### Check RabbitMQ
- Management UI: http://localhost:15672
- Username: `guest`
- Password: `guest`
- View queue: `document.index.queue`

## Scaling
To run multiple workers for parallel processing:

```bash
docker-compose up --scale indexer-worker=3
```

This will start 3 worker instances that will compete for messages from the queue.

## Error Handling
- If a document fails to process, its status is set to `FAILED`
- Failed documents can be retried by re-publishing the message to the queue
- Consider implementing a dead letter queue (DLQ) for production use

## Database Schema
The worker updates the `documents` table with these fields:
- `status`: UPLOADED → PROCESSING → INDEXED (or FAILED)
- `extractedText`: TEXT field containing the extracted content
- `indexedAt`: Timestamp when indexing completed

## Dependencies
- Spring Boot 3.2.0
- Spring AMQP (RabbitMQ)
- Spring Data JPA
- Apache Tika 2.9.1
- MySQL Connector
- Lombok

