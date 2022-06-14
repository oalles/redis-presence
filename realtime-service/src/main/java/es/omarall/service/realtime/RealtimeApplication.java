package es.omarall.service.realtime;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@RestController
@EnableScheduling
@Slf4j
public class RealtimeApplication {

    public static final String ONLINE = "ONLINE";
    public static final String OFFLINE = "OFFLINE";

    public static void main(String[] args) {
        SpringApplication.run(RealtimeApplication.class, args);
    }

    private final java.util.Map<String, SseEmitter> localPushRegistry = new ConcurrentHashMap<>();

    @GetMapping("/{name}")
    SseEmitter presenceStream(@PathVariable String name) {
        SseEmitter sse = new SseEmitter(Long.MAX_VALUE);
        sse.onCompletion(() -> {
            localPushRegistry.remove(name);
            this.notifyClientWentOffline(name);
            log.debug("Map Size: {}", localPushRegistry.size());
        });
        localPushRegistry.put(name, sse);
        log.debug("SSE session created for {}", name);
        log.debug("Map Size: {}", localPushRegistry.size());
        this.notifyClientWentOnline(name);
        return sse;
    }

    @Scheduled(fixedDelay = 5000L)
    void periodicSignal() {
        localPushRegistry.keySet().forEach(clientKey -> this.notifyOnlineClientsTo(clientKey));
    }

    /**
     * Send ONLINE clients to a client with a given key.
     *
     * @param clientKey
     */
    private void notifyOnlineClientsTo(String clientKey) {
        localPushRegistry.entrySet().parallelStream().filter(item -> !item.getKey().equals(clientKey))
                .forEach(item -> {
                    PresenceMessage message = PresenceMessage.builder()
                            .id(item.getKey())
                            .status(ONLINE)
                            .build();
                    this.sendPresenceMessage(clientKey, message);
                });
    }

    /**
     * Notifies a given client went offline to all other online clients
     */
    private void notifyClientWentOffline(String clientKey) {
        this.notifyClientStatus(clientKey, OFFLINE);
    }

    /**
     * Notifies a given client went online to all other online clients
     */
    private void notifyClientWentOnline(String clientKey) {
        this.notifyClientStatus(clientKey, ONLINE);
    }

    /**
     * Notifies a given client went status to all other online clients
     */
    private void notifyClientStatus(String clientKey, String status) {
        localPushRegistry.entrySet().parallelStream().filter(item -> !item.getKey().equals(clientKey))
                .forEach(item -> {
                    PresenceMessage message = PresenceMessage.builder()
                            .id(clientKey)
                            .status(status)
                            .build();
                    this.sendPresenceMessage(item.getKey(), message);
                });
    }

    private void sendPresenceMessage(String targetClientKey, PresenceMessage message) {
        try {
            localPushRegistry.get(targetClientKey).send(message);
        } catch (Throwable t) {
            log.debug("Cannot notify {} is {} to {}", message.getId(), message.getStatus(), targetClientKey);
        }
    }
}

@Builder
@Data
class PresenceMessage {
    private String id;
    private String status;
}
