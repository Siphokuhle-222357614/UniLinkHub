package za.co.unilinkhub;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import za.co.unilinkhub.realtime.RealtimeHub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The live event stream delivers notifications and chat messages as they happen. */
class RealtimeTest extends ApiTestSupport {

    @Test
    void sellerReceivesNewMessagesLive() throws Exception {
        String[] seller = seller();
        String listingId = product(seller[0], seller[1], "Live chat cookies", 5);

        MvcResult stream = mockMvc.perform(get("/api/stream").header("Authorization", seller[0]).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(post("/api/conversations").header("Authorization", student()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"listingId\":\"%s\",\"body\":\"Are these still warm?\"}".formatted(listingId)))
                .andExpect(status().isCreated());

        String events = stream.getResponse().getContentAsString();
        assertThat(events).contains("event:ready");
        assertThat(events).contains("event:notification").contains("New message from");
        assertThat(events).contains("event:message").contains("Are these still warm?");
    }

    /**
     * A tab that was closed without saying goodbye leaves a dead connection behind; on Tomcat even
     * closing it throws. That must not fail the request that caused the event (the sender saw an
     * error for a message that was actually sent) or stop the user's other tabs getting it.
     */
    @Test
    void aDeadConnectionDoesNotBreakDeliveryToOtherTabs() {
        List<String> delivered = new ArrayList<>();
        AtomicInteger created = new AtomicInteger();
        RealtimeHub hub = new RealtimeHub() {
            @Override
            protected SseEmitter newEmitter(long timeoutMillis) {
                boolean dead = created.incrementAndGet() == 1;
                return new SseEmitter(timeoutMillis) {
                    @Override
                    public void send(SseEventBuilder builder) throws IOException {
                        if (dead && created.get() > 1) throw new IOException("An established connection was aborted");
                        delivered.add(dead ? "dead tab" : "open tab");
                    }

                    @Override
                    public synchronized void completeWithError(Throwable ex) {
                        throw new IllegalStateException("AsyncContext used after an error");
                    }
                };
            }
        };
        UUID user = UUID.randomUUID();
        hub.connect(user); // the tab that will be closed
        hub.connect(user); // the tab still open
        delivered.clear();

        assertThatCode(() -> hub.publish(user, "message", Map.of("body", "Two boxes left")))
                .doesNotThrowAnyException();
        assertThat(delivered).containsExactly("open tab");
        assertThat(hub.openConnections(user)).isEqualTo(1);
    }

    @Test
    void theStreamNeedsALogin() throws Exception {
        mockMvc.perform(get("/api/stream").accept(MediaType.TEXT_EVENT_STREAM)).andExpect(status().isUnauthorized());
    }
}
