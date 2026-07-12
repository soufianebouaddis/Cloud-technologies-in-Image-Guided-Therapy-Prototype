package os.org.simulator.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks connected browser (React) sessions and fan-outs pipeline results to them.
 */
@Component
public class BrowserSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(BrowserSessionRegistry.class);

    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    public void add(WebSocketSession session) {
        sessions.add(session);
    }

    public void remove(WebSocketSession session) {
        sessions.remove(session);
    }

    public int size() {
        return sessions.size();
    }

    /** Push a JSON payload to every connected browser. */
    public void broadcast(String json) {
        sessions.removeIf(s -> !s.isOpen());
        for (WebSocketSession session : sessions) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                log.warn("Failed to push to browser session {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}
