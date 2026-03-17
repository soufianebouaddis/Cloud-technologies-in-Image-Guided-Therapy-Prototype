cmake_minimum_required(VERSION 3.15)
project(MedicalKernel CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# OpenMP
find_package(OpenMP REQUIRED)

# Protobuf & gRPC via Homebrew (CONFIG mode to avoid target conflicts)
find_package(gRPC CONFIG REQUIRED)
find_package(Protobuf CONFIG REQUIRED)

# Proto file
set(PROTO_FILE ${CMAKE_CURRENT_SOURCE_DIR}/proto/imaging.proto)
set(PROTO_OUT ${CMAKE_CURRENT_BINARY_DIR}/generated)
file(MAKE_DIRECTORY ${PROTO_OUT})

# Find protoc and grpc_cpp_plugin
find_program(PROTOC protoc REQUIRED)
find_program(GRPC_CPP_PLUGIN grpc_cpp_plugin REQUIRED)

# Generate protobuf and gRPC sources
add_custom_command(
    OUTPUT
        ${PROTO_OUT}/imaging.pb.cc
        ${PROTO_OUT}/imaging.pb.h
        ${PROTO_OUT}/imaging.grpc.pb.cc
        ${PROTO_OUT}/imaging.grpc.pb.h
    COMMAND ${PROTOC}
        --proto_path=${CMAKE_CURRENT_SOURCE_DIR}/proto
        --cpp_out=${PROTO_OUT}
        --grpc_out=${PROTO_OUT}
        --plugin=protoc-gen-grpc=${GRPC_CPP_PLUGIN}
        ${PROTO_FILE}
    DEPENDS ${PROTO_FILE}
    COMMENT "Generating protobuf/gRPC C++ sources"
)

add_executable(medical_kernel
    src/medical_kernel.cpp
    src/grpc_server.cpp
    ${PROTO_OUT}/imaging.pb.cc
    ${PROTO_OUT}/imaging.grpc.pb.cc
)

target_include_directories(medical_kernel PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${PROTO_OUT}
)

target_link_libraries(medical_kernel
    PRIVATE
    gRPC::grpc++
    protobuf::libprotobuf
    OpenMP::OpenMP_CXX
)

target_compile_options(medical_kernel PRIVATE -O3 -march=native)
