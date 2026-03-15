cmake_minimum_required(VERSION 3.15)
project(MedicalKernel CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# OpenMP
find_package(OpenMP REQUIRED)

# Protobuf
find_package(Protobuf REQUIRED)

# gRPC
find_package(gRPC REQUIRED)

set(PROTO_FILE proto/imaging.proto)

protobuf_generate_cpp(PROTO_SRCS PROTO_HDRS ${PROTO_FILE})
grpc_generate_cpp(GRPC_SRCS GRPC_HDRS ${PROTO_FILE})

# Executable instead of shared library
add_executable(medical_kernel
    medical_kernel.cpp
    grpc_server.cpp
    ${PROTO_SRCS}
    ${GRPC_SRCS}
)

target_link_libraries(medical_kernel
    PRIVATE
    gRPC::grpc++
    protobuf::libprotobuf
    OpenMP::OpenMP_CXX
)

target_compile_options(medical_kernel PRIVATE -O3 -march=native)