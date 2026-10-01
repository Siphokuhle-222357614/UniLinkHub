package za.co.unilinkhub.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import za.co.unilinkhub.security.CurrentUser;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RealtimeController {

    private final RealtimeHub hub;

    /**
     * The logged-in user's live event stream. Events: "notification" {category, message} and
     * "message" {conversationId, message}. The frontend opens this with fetch() rather than
     * EventSource, because EventSource can't send the Authorization header.
     */
    @GetMapping(value = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@CurrentUser UUID userId) {
        return hub.connect(userId);
    }
}
