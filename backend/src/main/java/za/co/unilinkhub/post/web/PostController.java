package za.co.unilinkhub.post.web;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.post.application.CommentView;
import za.co.unilinkhub.post.application.PostService;
import za.co.unilinkhub.post.application.PostView;
import za.co.unilinkhub.security.CurrentUser;
import za.co.unilinkhub.security.StudentOnly;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    public record PostRequest(String body, String imageUrl, UUID listingId) {
    }

    public record CommentRequest(String body) {
    }

    public record PinRequest(boolean pinned) {
    }

    public record RemoveRequest(String reason) {
    }

    // ---- Provider page ----

    @GetMapping("/api/businesses/{businessId}/posts")
    public List<PostView> forBusiness(@PathVariable UUID businessId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before) {
        return postService.listForBusiness(businessId, before);
    }

    @PostMapping("/api/businesses/{businessId}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @StudentOnly("post on business pages")
    public PostView create(@CurrentUser UUID userId, @PathVariable UUID businessId, @RequestBody PostRequest request) {
        return postService.create(userId, businessId, request.body(), request.imageUrl(), request.listingId());
    }

    @PatchMapping("/api/posts/{id}")
    @StudentOnly("edit business posts")
    public PostView edit(@CurrentUser UUID userId, @PathVariable UUID id, @RequestBody PostRequest request) {
        return postService.edit(userId, id, request.body(), request.imageUrl(), request.listingId());
    }

    @DeleteMapping("/api/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @StudentOnly("delete business posts - use \"Remove\" in the admin console instead")
    public void delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        postService.delete(userId, id);
    }

    @PostMapping("/api/posts/{id}/pin")
    @StudentOnly("pin business posts")
    public PostView pin(@CurrentUser UUID userId, @PathVariable UUID id, @RequestBody PinRequest request) {
        return postService.setPinned(userId, id, request.pinned());
    }

    // ---- Students ----

    @GetMapping("/api/posts/feed")
    public List<PostView> feed(@CurrentUser UUID userId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before) {
        return postService.feed(userId, before);
    }

    @PostMapping("/api/posts/{id}/like")
    @StudentOnly("like posts")
    public PostView like(@CurrentUser UUID userId, @PathVariable UUID id) {
        return postService.like(userId, id);
    }

    @DeleteMapping("/api/posts/{id}/like")
    @StudentOnly("like posts")
    public PostView unlike(@CurrentUser UUID userId, @PathVariable UUID id) {
        return postService.unlike(userId, id);
    }

    @GetMapping("/api/posts/{id}/comments")
    public List<CommentView> comments(@PathVariable UUID id) {
        return postService.comments(id);
    }

    @PostMapping("/api/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @StudentOnly("comment on posts")
    public CommentView comment(@CurrentUser UUID userId, @PathVariable UUID id, @RequestBody CommentRequest request) {
        return postService.comment(userId, id, request.body());
    }

    // Authors, page owners and admins can all delete comments, so no @StudentOnly here.
    @DeleteMapping("/api/post-comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@CurrentUser UUID userId, @PathVariable UUID id) {
        postService.deleteComment(userId, id);
    }

    @PostMapping("/api/posts/{id}/flag")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @StudentOnly("report posts - use \"Remove\" in the admin console instead")
    public void flag(@CurrentUser UUID userId, @PathVariable UUID id) {
        postService.flag(userId, id);
    }

    // ---- Admin ----

    @GetMapping("/api/admin/posts")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PostView> adminList(@RequestParam(defaultValue = "false") boolean flaggedOnly) {
        return postService.adminList(flaggedOnly);
    }

    @PostMapping("/api/admin/posts/{id}/remove")
    @PreAuthorize("hasRole('ADMIN')")
    public PostView remove(@CurrentUser UUID adminId, @PathVariable UUID id, @RequestBody(required = false) RemoveRequest request) {
        return postService.remove(adminId, id, request == null ? null : request.reason());
    }
}
