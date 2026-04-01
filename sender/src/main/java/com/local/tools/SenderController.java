package com.local.tools;


import com.google.protobuf.ByteString;
import com.local.tools.grpc.FileChunk;
import com.local.tools.grpc.FileServiceGrpc;
import com.local.tools.grpc.UploadStatus;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
public class SenderController {

    private final FileServiceGrpc.FileServiceStub fileServiceStub;

    public SenderController(@GrpcClient("file-service") FileServiceGrpc.FileServiceStub fileServiceStub) {
        this.fileServiceStub = fileServiceStub;
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        AtomicReference<UploadStatus> finalStatus = new AtomicReference<>();
        CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<UploadStatus> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(UploadStatus status) {
                log.info("Server send update status: {}", status);
                finalStatus.set(status);
            }

            @Override
            public void onError(Throwable t) {
                log.info("Server send on error", t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("Server send completed");
                finishLatch.countDown();
            }
        };

        StreamObserver<FileChunk> requestObserver = fileServiceStub.uploadFile(responseObserver);

        AtomicInteger chunkNumber = new AtomicInteger(0);
        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[1048576]; // 1MB chunks
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                FileChunk chunk = FileChunk.newBuilder()
                        .setFilename(file.getOriginalFilename())
                        .setContent(ByteString.copyFrom(buffer, 0, length))
                        .build();

                log.info("Uploading file chunk: {}", chunkNumber.incrementAndGet());
                requestObserver.onNext(chunk);
            }

            log.info("Sending completion notification to server");
            requestObserver.onCompleted();

            log.info("Waiting for server to finish");
            finishLatch.await();
            log.info("Server has finished");
        } catch (Exception e) {
            requestObserver.onError(e);
            return "Failed: " + e.getMessage();
        }

        return Optional.ofNullable(finalStatus.get())
                .map(UploadStatus::getSuccess)
                .map(success -> "Upload finished with isSuccess : " + success)
                .orElse("Upload failed silently");
    }
}