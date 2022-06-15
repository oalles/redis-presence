package es.omarall.service.realtime;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Presence {
    private String client;
    private String status;
}
