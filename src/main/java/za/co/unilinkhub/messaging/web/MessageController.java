package za.co.unilinkhub.messaging.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.messaging.application.ConversationSummaryView;
import za.co.unilinkhub.messaging.application.MessageDTO;
import za.co.unilinkhub.messaging.application.MessageService;
import za.co.unilinkhub.security.CurrentUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    public record StartConversationRequest(@NotNull UUID listingId, @NotBlank String body) {
    }

    public record ReplyRequest(@NotBlank String body) {
    }

    @PostMapping("/api/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationSummaryView start(@CurrentUser UUID buyerId, @Valid @RequestBody StartConversationRequest request) {
        return messageService.startConversation(buyerId, request.listingId(), request.body());
    }

    @GetMapping("/api/conversations")
    public List<ConversationSummaryView> mine(@CurrentUser UUID userId) {
        return messageService.listMine(userId);
    }

    @GetMapping("/api/conversations/unread-count")
    public Map<String, Long> unreadCount(@CurrentUser UUID userId) {
        return Map.of("count", messageService.unreadCount(userId));
    }

    @GetMapping("/api/conversations/{id}/messages")
    public List<MessageDTO> messages(@CurrentUser UUID userId, @PathVariable UUID id) {
        return messageService.getMessages(id, userId);
    }

    @PostMapping("/api/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDTO reply(@CurrentUser UUID userId, @PathVariable UUID id, @Valid @RequestBody ReplyRequest request) {
        return messageService.reply(id, userId, request.body());
    }
}
