package com.local.tools.observer;

import com.local.tools.grpc.FileChunk;
import com.local.tools.grpc.UploadStatus;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSUploadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@Scope("prototype")
public class MongoOutputStreamObserver implements StreamObserver<FileChunk> {
    private final AtomicInteger chunkCounter;
    private final GridFSBucket gridFSBucket;
    private StreamObserver<UploadStatus> responseObserver;
    private GridFSUploadStream outputStream;

    protected MongoOutputStreamObserver(@Autowired GridFSBucket gridFSBucket) {
        this.chunkCounter = new AtomicInteger(0);
        this.gridFSBucket = gridFSBucket;
    }

    public MongoOutputStreamObserver observe(StreamObserver<UploadStatus> responseObserver) {
        this.responseObserver = responseObserver;
        return this;
    }

    @Override
    public void onNext(FileChunk chunk) {
        int chunkNumber = this.chunkCounter.incrementAndGet();
        log.info("Received file chunk: {}", chunkNumber);
        try {
            if (this.outputStream == null) {
                this.outputStream = initializeOutputStream(chunk.getFilename());
            }
            chunk.getContent().writeTo(this.outputStream);
        } catch (IOException e) {
            onServerError(e);
        }
    }

    public GridFSUploadStream initializeOutputStream(String fileName) {
        GridFSUploadOptions options = new GridFSUploadOptions()
                .chunkSizeBytes(1048576)
                .metadata(new Document("type", "text"));

        return this.gridFSBucket.openUploadStream(fileName, options);
    }

    protected void onServerError(Throwable t) {
        try {
            onError(t);
        } finally {
            notifyClientAboutError(t);
        }
    }

    private void notifyClientAboutError(Throwable t) {
        this.responseObserver.onError(io.grpc.Status.INTERNAL
                .withDescription("Server failed to write data: " + t.getMessage())
                .withCause(t)
                .asRuntimeException());
    }

    @Override
    public void onCompleted() {
        log.info("Completed receiving a file, closing output stream");
        this.outputStream.close();

        log.info("Sending upload status to client");
        this.responseObserver.onNext(UploadStatus.newBuilder().setSuccess(true).build());
        log.info("Sending completion notification to client");
        this.responseObserver.onCompleted();
    }

    @Override
    public void onError(Throwable t) {
        log.error("Error receiving file from client", t);
        this.outputStream.abort();
    }
}