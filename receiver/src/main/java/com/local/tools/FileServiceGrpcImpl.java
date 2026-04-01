package com.local.tools;

import com.local.tools.grpc.FileChunk;
import com.local.tools.grpc.FileServiceGrpc;
import com.local.tools.grpc.UploadStatus;
import com.local.tools.observer.FileStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.ObjectProvider;

@Slf4j
@GrpcService
@AllArgsConstructor
public class FileServiceGrpcImpl extends FileServiceGrpc.FileServiceImplBase {

    private final ObjectProvider<FileStreamObserver> fileStreamObservers;

    @Override
    public StreamObserver<FileChunk> uploadFile(StreamObserver<UploadStatus> responseObserver) {
        return this.fileStreamObservers.getObject().observe(responseObserver);
    }
}
