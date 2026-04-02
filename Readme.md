Starting the application

1. `mvn clean package`
2. `docker-compose up --build`

Using the application

1. `curl -F "file=@test.txt" http://localhost:8080/upload` - Upload a file to mongo DB
    1. Upload from your local to `Sender` service happens via REST without TLS.
    2. `Sender` submits the file to `Receiver` via gRCP with MTLS. Certs are self-signed.
    3. `Receiver` writes file chunks on fly to `Mongo GridFS`
    4. Whole file is not loaded to memory, only the chunks.
2. `curl http://localhost:8081/mongo-files` - List all files stored in MongoDB (REST API)
3. `curl http://localhost:8081/mongo-files/test.txt` - Download file content as String. Whole file is send as a
   response.
4. `curl -X DELETE http://localhost:8081/mongo-files/test.txt` - Delete a file




