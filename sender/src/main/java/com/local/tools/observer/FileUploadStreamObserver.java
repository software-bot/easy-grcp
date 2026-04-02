package com.local.tools.observer;

import com.local.tools.grpc.UploadStatus;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class FileUploadStreamObserver implements StreamObserver<UploadStatus> {
    private final AtomicReference<UploadStatus> finalStatus = new AtomicReference<>();
    private final CountDownLatch finishLatch = new CountDownLatch(1);

    @Override
    public void onNext(UploadStatus status) {
        log.info("Server send update {}", status);
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

    public void await() throws InterruptedException {
        this.finishLatch.await();
    }

    public UploadStatus getFinalStatus() {
        return this.finalStatus.get();
    }
}
