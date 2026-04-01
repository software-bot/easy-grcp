package com.local.tools.observer;

import com.local.tools.grpc.FileChunk;
import com.local.tools.grpc.UploadStatus;
import io.grpc.stub.StreamObserver;

public interface FileStreamObserver extends StreamObserver<FileChunk> {
    FileStreamObserver observe(StreamObserver<UploadStatus> responseObserver);
}
