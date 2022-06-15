package es.omarall.service.realtime;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Heartbeat {
    private String client;
    private String server;
}
