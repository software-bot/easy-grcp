package com.local.tools;


import com.google.protobuf.ByteString;
import com.local.tools.grpc.FileChunk;
import com.local.tools.grpc.FileServiceGrpc;
import com.local.tools.grpc.UploadStatus;
import com.local.tools.observer.FileUploadStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
public class SenderController {

    private final FileServiceGrpc.FileServiceStub fileServiceStub;

    public SenderController(@GrpcClient("file-service") FileServiceGrpc.FileServiceStub fileServiceStub) {
        this.fileServiceStub = fileServiceStub;
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        FileUploadStreamObserver fileUploadStreamObserver = new FileUploadStreamObserver();
        StreamObserver<FileChunk> requestObserver = this.fileServiceStub.uploadFile(fileUploadStreamObserver);

        AtomicInteger chunkNumber = new AtomicInteger(0);
        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[1048576]; // 1MB chunks
            while (inputStream.read(buffer) != -1) {
                FileChunk chunk = FileChunk.newBuilder()
                        .setFilename(file.getOriginalFilename())
                        .setContent(ByteString.copyFrom(buffer))
                        .build();

                log.info("Uploading file chunk: {}", chunkNumber.incrementAndGet());
                requestObserver.onNext(chunk);
            }

            log.info("Sending completion notification to server");
            requestObserver.onCompleted();

            log.info("Waiting for server to finish");
            fileUploadStreamObserver.await();
            log.info("Server has finished");
        } catch (Exception e) {
            requestObserver.onError(e);
            return "Failed: " + e.getMessage();
        }

        return Optional.ofNullable(fileUploadStreamObserver.getFinalStatus())
                .map(UploadStatus::getSuccess)
                .map(success -> "Upload finished with isSuccess : " + success)
                .orElse("Upload failed silently");
    }
}