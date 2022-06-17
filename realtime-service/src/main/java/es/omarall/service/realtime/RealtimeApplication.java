package es.omarall.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@RestController
@EnableScheduling
@Slf4j
public class RealtimeApplication {

    private static final String HB_STREAM_KEY = "heartbeat";
    private final String PRESENCE_STREAM_KEY = "presence";

    public static void main(String[] args) {
        SpringApplication.run(RealtimeApplication.class, args);
    }

    private final java.util.Map<String, SseEmitter> localPushRegistry = new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final Subscription subscription;

    public RealtimeApplication(StringRedisTemplate redisTemplate,
                               final ObjectMapper objectMapper,
                               StreamMessageListenerContainer streamMessageListenerContainer) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;

        this.subscription = streamMessageListenerContainer.receive(StreamOffset.fromStart(PRESENCE_STREAM_KEY),
                (message) -> {
                    try {
                        Presence presence = objectMapper.convertValue(message.getValue(), Presence.class);
                        log.info("Presence received: {}", presence);
                        this.notifyPresence(presence);
                    } catch (Throwable t) {
                        log.error("Invalid message: Stream: {} - Value: {}", message.getStream(), message.getValue());
                    }
                });
    }

    @GetMapping("/{name}")
    SseEmitter presenceStream(@PathVariable final String name) throws IOException {
        SseEmitter sse = new SseEmitter(Long.MAX_VALUE);
//        SseEmitter sse = new SseEmitter();
        sse.onCompletion(() -> this.removeSession(name));
        sse.onTimeout(() -> this.removeSession(name));
        this.handleNewSession(name, sse);
        return sse;
    }

    private void handleNewSession(String name, SseEmitter sse) {
        localPushRegistry.put(name, sse);
        log.debug("SSE session created for {} - Current Sessions count: {}", name, localPushRegistry.size());
        this.publishHeartbeatMessageForClient(name);
    }

    private void removeSession(String name) {
        localPushRegistry.remove(name);
        log.debug("SSE session removed for {} - Current Sessions count: {}", name, localPushRegistry.size());
    }

    @Scheduled(fixedDelay = 7000L)
    void periodicSignal() {
        localPushRegistry.keySet().parallelStream().forEach(this::publishHeartbeatMessageForClient);
    }

    @Scheduled(fixedDelay = 5000L)
    void HB() {
        localPushRegistry.keySet().parallelStream().forEach(this::sendUp);
    }

    /**
     * Send ONLINE clients to a client with a given key.
     */
    private void publishHeartbeatMessageForClient(String clientId) {
        Heartbeat heartbeat = Heartbeat.builder()
                .client(clientId)
                .build();
        StringRecord record = StringRecord.of(this.objectMapper.convertValue(heartbeat, Map.class)).withStreamKey(HB_STREAM_KEY);
        redisTemplate.opsForStream().add(record);
    }

    /**
     * Send ONLINE clients to a client with a given key.
     */
    private void sendUp(String clientId) {
        try {
            this.localPushRegistry.get(clientId).send("UP");
        } catch (Throwable t) {
            log.debug("Cannot notify HB to {}", clientId);
        }
    }

    /**
     * Notifies a presence message to all other sessions
     */
    private void notifyPresence(Presence presence) {
        localPushRegistry.entrySet().parallelStream().filter(item -> !item.getKey().equals(presence.getClient()))
                .forEach(item -> {
                    this.sendPresenceMessage(item.getKey(), presence);
                });
    }

    private void sendPresenceMessage(String targetClientKey, Presence message) {
        try {
            localPushRegistry.get(targetClientKey).send(message);
        } catch (Throwable t) {
            log.debug("Cannot notify {} is {} to {}", message.getClient(), message.getStatus(), targetClientKey);
        }
    }
}


