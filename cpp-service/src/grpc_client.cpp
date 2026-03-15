#include <grpcpp/grpcpp.h>
#include "imaging.grpc.pb.h"

using grpc::Channel;
using grpc::ClientContext;
using grpc::Status;

using imaging::ImagingService;
using imaging::ImageRequest;
using imaging::ImageResponse;

class ImagingClient {

private:
    std::unique_ptr<ImagingService::Stub> stub_;

public:

    ImagingClient(std::shared_ptr<Channel> channel)
        : stub_(ImagingService::NewStub(channel)) {}

    std::vector<uint16_t> processRemote(
        const uint16_t* input,
        int width,
        int height)
    {

        ImageRequest request;

        request.set_width(width);
        request.set_height(height);

        request.set_image(
            reinterpret_cast<const char*>(input),
            width * height * sizeof(uint16_t));

        ImageResponse response;

        ClientContext context;

        Status status = stub_->ProcessImage(&context, request, &response);

        if (!status.ok())
            throw std::runtime_error("gRPC call failed");

        const std::string& data = response.image();

        std::vector<uint16_t> output(width * height);

        memcpy(output.data(), data.data(), data.size());

        return output;
    }
};