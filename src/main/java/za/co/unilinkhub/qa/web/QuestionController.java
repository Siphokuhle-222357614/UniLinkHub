package za.co.unilinkhub.qa.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.qa.application.QuestionService;
import za.co.unilinkhub.qa.application.QuestionStatsDTO;
import za.co.unilinkhub.qa.application.QuestionView;
import za.co.unilinkhub.security.CurrentUser;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    public record AskRequest(@NotBlank String questionText) {
    }

    public record AnswerRequest(@NotBlank String answerText) {
    }

    @PostMapping("/api/listings/{id}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionView ask(@CurrentUser UUID askerId, @PathVariable UUID id, @Valid @RequestBody AskRequest request) {
        return questionService.ask(askerId, id, request.questionText());
    }

    @GetMapping("/api/listings/{id}/questions")
    public List<QuestionView> forListing(@PathVariable UUID id) {
        return questionService.listForListing(id);
    }

    @PostMapping("/api/questions/{id}/answer")
    public QuestionView answer(@CurrentUser UUID sellerId, @PathVariable UUID id, @Valid @RequestBody AnswerRequest request) {
        return questionService.answer(id, sellerId, request.answerText());
    }

    @GetMapping("/api/questions/pending")
    public List<QuestionView> pending(@CurrentUser UUID sellerId) {
        return questionService.pendingForSeller(sellerId);
    }

    @GetMapping("/api/questions/mine")
    public List<QuestionView> mine(@CurrentUser UUID sellerId) {
        return questionService.listForSeller(sellerId);
    }

    @PostMapping("/api/questions/{id}/flag")
    public void flag(@PathVariable UUID id) {
        questionService.flag(id);
    }

    @GetMapping("/api/admin/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<QuestionView> adminList(@RequestParam(defaultValue = "false") boolean flaggedOnly) {
        return questionService.adminList(flaggedOnly);
    }

    @DeleteMapping("/api/admin/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void adminRemove(@CurrentUser UUID adminId, @PathVariable UUID id) {
        questionService.adminRemove(id, adminId);
    }

    @GetMapping("/api/admin/questions/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public QuestionStatsDTO adminStats() {
        return questionService.adminStats();
    }
}
