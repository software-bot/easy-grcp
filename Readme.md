<h2>Before starting the application</h2>

<h3>1. Generate certificates</h3>

```angular2html
openssl req -x509 -newkey rsa:4096 -days 365 -nodes -keyout ca.key -out ca.crt -subj "/CN=MyLocalCA"

# Generate CSR and Key
openssl req -newkey rsa:4096 -nodes -keyout server.key.tmp -out server.csr -subj "/CN=receiver"

# Sign the Server Cert with the CA
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -days 365

# Convert Server Key to PKCS#8 format for Java
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in server.key.tmp -out server.key

# Generate CSR and Key
openssl req -newkey rsa:4096 -nodes -keyout client.key.tmp -out client.csr -subj "/CN=sender-client"

# Sign the Client Cert with the CA
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out client.crt -days 365

# Convert Client Key to PKCS#8 format for Java
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in client.key.tmp -out client.key
```
If you have path conversion issue, use `//CN` instead of `/CN`

<h3>2. In the Receiver Project (`src/main/resources/certs/`):</h3>
Copy files:

```
ca.crt (To trust the incoming client)
server.crt (To identify itself)
server.key (To decrypt the traffic)
```

<h3>3. In the Sender Project (`src/main/resources/certs/`):</h3>
Copy files:

```
ca.crt (To trust the server)
client.crt (To identify itself to the server)
client.key (To encrypt the traffic)
```
<h2>Starting the application</h2>

1. `mvn clean package`
2. `docker-compose up --build`

<h2>Using the application</h2>

1. `curl -F "file=@test.txt" http://localhost:8080/upload` - Upload a file to mongo DB
    1. Upload from your local to `Sender` service happens via REST without TLS.
    2. `Sender` submits the file to `Receiver` via gRCP with MTLS. Certs are self-signed.
    3. `Receiver` writes file chunks on fly to `Mongo GridFS`
    4. Whole file is not loaded to memory, only the chunks.
2. `curl http://localhost:8081/mongo-files` - List all files stored in MongoDB (REST API)
3. `curl http://localhost:8081/mongo-files/test.txt` - Download file content as String. Whole file is send as a
   response.
4. `curl -X DELETE http://localhost:8081/mongo-files/test.txt` - Delete a file




