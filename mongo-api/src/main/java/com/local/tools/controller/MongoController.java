package com.local.tools.controller;


import com.mongodb.MongoGridFSException;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MongoController {
    private final GridFSBucket gridFSBucket;

    @DeleteMapping("{fileName:.+}")
    public void delete(@PathVariable String fileName) {
        GridFSFile fileInfo = this.gridFSBucket.find(Filters.eq("filename", fileName)).first();
        if (fileInfo != null) {
            this.gridFSBucket.delete(fileInfo.getId());
        }
    }

    @GetMapping("data/{fileName:.+}")
    public ResponseEntity<String> get(@PathVariable String fileName) {
        WeakReference<String> fileContent = null;
        // 1. Open an InputStream directly to the file in MongoDB
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 1. Tell Mongo to dump the entire file into our memory stream
            this.gridFSBucket.downloadToStream(fileName, outputStream);

            // 2. Convert the raw bytes into a UTF-8 String
            fileContent = new WeakReference<>(outputStream.toString(StandardCharsets.UTF_8));
            // 3. Return the string as the HTTP response body
            return ResponseEntity.ok(fileContent.get());

        } catch (MongoGridFSException e) {
            log.error("File not found in database: {}", fileName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found.");
        } catch (Exception e) {
            log.error("Error reading file into memory", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process file.");
        } finally {
            if (fileContent != null) {
                fileContent.clear();
            }
        }
    }

    @GetMapping("list-files")
    public String listFiles() {
        StringJoiner sb = new StringJoiner(", ");
        this.gridFSBucket.find().forEach(f -> sb.add(f.getFilename()));
        return sb.toString();
    }
}