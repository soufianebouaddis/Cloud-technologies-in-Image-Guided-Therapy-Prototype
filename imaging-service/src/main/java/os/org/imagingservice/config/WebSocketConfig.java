package os.org.imagingservice.config;

import os.org.imagingservice.handler.DicomWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DicomWebSocketHandler dicomWebSocketHandler;

    public WebSocketConfig(DicomWebSocketHandler dicomWebSocketHandler) {
        this.dicomWebSocketHandler = dicomWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(dicomWebSocketHandler, "/ws/dicom").setAllowedOrigins("*");
    }
}
