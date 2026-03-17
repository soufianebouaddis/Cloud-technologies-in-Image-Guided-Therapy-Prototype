#include <grpcpp/grpcpp.h>
#include "imaging.grpc.pb.h"
#include "medical_kernel.h"
#include <iostream>
#include <vector>

using grpc::Server;
using grpc::ServerBuilder;
using grpc::ServerContext;
using grpc::Status;

using imaging::ImagingService;
using imaging::ImageRequest;
using imaging::ImageResponse;

class ImagingServiceImpl final : public ImagingService::Service {
    Status ProcessImage(ServerContext* context,
                        const ImageRequest* request,
                        ImageResponse* response) override {

        int width = request->width();
        int height = request->height();
        const std::string& raw = request->image();

        size_t expected = width * height * sizeof(uint16_t);
        if (raw.size() != expected) {
            return Status(grpc::INVALID_ARGUMENT,
                "Expected " + std::to_string(expected) + " bytes, got " + std::to_string(raw.size()));
        }

        const auto* input = reinterpret_cast<const uint16_t*>(raw.data());
        std::vector<uint16_t> output(width * height);

        process_medical_image(input, output.data(), width, height, 3.0f, 30.0f);

        response->set_image(reinterpret_cast<const char*>(output.data()), expected);

        std::cout << "Denoised frame: " << width << "x" << height << std::endl;
        return Status::OK;
    }
};

int main(int argc, char** argv) {
    std::string address = "0.0.0.0:50051";

    ImagingServiceImpl service;
    ServerBuilder builder;
    builder.AddListeningPort(address, grpc::InsecureServerCredentials());
    builder.RegisterService(&service);

    std::unique_ptr<Server> server(builder.BuildAndStart());
    std::cout << "cpp-service listening on " << address << std::endl;
    server->Wait();

    return 0;
}
