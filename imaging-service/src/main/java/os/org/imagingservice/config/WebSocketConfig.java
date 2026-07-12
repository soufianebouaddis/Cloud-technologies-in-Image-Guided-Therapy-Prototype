package os.org.imagingservice.config;

import os.org.imagingservice.handler.DicomWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    // DICOM frames are multi-megabyte; the default 8 KB buffer rejects them with close code 1009.
    private static final int MAX_BINARY_BUFFER = 16 * 1024 * 1024;
    private static final int MAX_TEXT_BUFFER = 1 * 1024 * 1024;

    private final DicomWebSocketHandler dicomWebSocketHandler;

    public WebSocketConfig(DicomWebSocketHandler dicomWebSocketHandler) {
        this.dicomWebSocketHandler = dicomWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(dicomWebSocketHandler, "/ws/dicom").setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(MAX_BINARY_BUFFER);
        container.setMaxTextMessageBufferSize(MAX_TEXT_BUFFER);
        return container;
    }
}
