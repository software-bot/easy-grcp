package com.local.tools.controller;


import com.mongodb.MongoGridFSException;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

@Slf4j
@RestController
@RequestMapping("mongo-files")
@RequiredArgsConstructor
public class MongoFileController {
    private final GridFSBucket gridFSBucket;

    @DeleteMapping("{fileName:.+}")
    public void delete(@PathVariable String fileName) {
        GridFSFile fileInfo = this.gridFSBucket.find(Filters.eq("filename", fileName)).first();
        if (fileInfo != null) {
            this.gridFSBucket.delete(fileInfo.getId());
        }
    }

    @GetMapping("{fileName:.+}")
    public ResponseEntity<String> get(@PathVariable String fileName) {
        WeakReference<String> fileContent = null;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            this.gridFSBucket.downloadToStream(fileName, outputStream);
            fileContent = new WeakReference<>(outputStream.toString(StandardCharsets.UTF_8));

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

    @GetMapping
    public String listFiles() {
        StringJoiner sb = new StringJoiner("\n");
        this.gridFSBucket.find().forEach(f -> sb.add(f.getFilename()));
        return sb.toString();
    }
}