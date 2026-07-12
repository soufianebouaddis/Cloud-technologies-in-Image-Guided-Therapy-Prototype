package os.org.simulator.config;

import os.org.simulator.gateway.BrowserWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class GatewayWebSocketConfig implements WebSocketConfigurer {

    // DICOM uploads from the browser are multi-megabyte; raise buffers above the 8 KB default.
    private static final int MAX_BUFFER = 16 * 1024 * 1024;

    private final BrowserWebSocketHandler browserHandler;

    public GatewayWebSocketConfig(BrowserWebSocketHandler browserHandler) {
        this.browserHandler = browserHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(browserHandler, "/ws/stream").setAllowedOriginPatterns("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(MAX_BUFFER);
        container.setMaxTextMessageBufferSize(MAX_BUFFER);
        return container;
    }
}
