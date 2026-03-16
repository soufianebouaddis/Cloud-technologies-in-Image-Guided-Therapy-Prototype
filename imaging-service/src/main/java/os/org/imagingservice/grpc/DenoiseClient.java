package os.org.imagingservice.grpc;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import os.org.imagingservice.grpc.proto.ImageRequest;
import os.org.imagingservice.grpc.proto.ImageResponse;
import os.org.imagingservice.grpc.proto.ImagingServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DenoiseClient {

    private static final Logger log = LoggerFactory.getLogger(DenoiseClient.class);

    @Value("${grpc.cpp-service.host:localhost}")
    private String host;

    @Value("${grpc.cpp-service.port:50051}")
    private int port;

    private ManagedChannel channel;
    private ImagingServiceGrpc.ImagingServiceBlockingStub stub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        stub = ImagingServiceGrpc.newBlockingStub(channel);
        log.info("gRPC channel opened to {}:{}", host, port);
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public byte[] denoise(byte[] pixelData, int width, int height) {
        ImageRequest request = ImageRequest.newBuilder()
                .setWidth(width)
                .setHeight(height)
                .setImage(ByteString.copyFrom(pixelData))
                .build();

        ImageResponse response = stub.processImage(request);
        return response.getImage().toByteArray();
    }
}
