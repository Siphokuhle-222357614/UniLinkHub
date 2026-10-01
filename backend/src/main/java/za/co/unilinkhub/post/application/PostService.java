package za.co.unilinkhub.post.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.unilinkhub.audit.application.AuditLogService;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.domain.VerificationStatus;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ForbiddenException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.common.exception.TooManyRequestsException;
import za.co.unilinkhub.follow.domain.FollowedBusiness;
import za.co.unilinkhub.follow.repository.FollowedBusinessRepository;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.media.application.ImageService;
import za.co.unilinkhub.moderation.RestrictedItemsPolicy;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.post.domain.BusinessPost;
import za.co.unilinkhub.post.domain.PostComment;
import za.co.unilinkhub.post.domain.PostLike;
import za.co.unilinkhub.post.repository.PostCommentRepository;
import za.co.unilinkhub.post.repository.PostLikeRepository;
import za.co.unilinkhub.post.repository.PostRepository;
import za.co.unilinkhub.security.CurrentUserProvider;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Business posts - a Facebook-style feed on each provider page.
 *
 * Rules, and why: only a business's owner posts on its page; only verified businesses can post
 * (so a brand-new account can't use posts to spam); posts and comments go through the same
 * restricted-items check as listings; and each business gets {@value #DAILY_POST_LIMIT} posts a day
 * so followers' feeds don't get flooded.
 */
@Service
@RequiredArgsConstructor
public class PostService {

    static final int DAILY_POST_LIMIT = 10;
    /** Posts per page. The next page is "older than the last post I have" (a cursor), which stays correct as new posts arrive. */
    static final int PAGE_SIZE = 20;
    private static final int PREVIEW_LENGTH = 80;

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final PostCommentRepository commentRepository;
    private final BusinessRepository businessRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final FollowedBusinessRepository followRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final RestrictedItemsPolicy restrictedItemsPolicy;

    // ------------------------------------------------------------------ reading

    /**
     * A provider page's posts: on the first page the pinned post comes first, then newest. Pass the
     * oldest {@code createdAt} you have as {@code before} to get the next page.
     */
    public List<PostView> listForBusiness(UUID businessId, LocalDateTime before) {
        Business business = findBusiness(businessId);
        boolean ownerOrAdmin = isOwner(business) || CurrentUserProvider.isAdmin();
        List<BusinessPost> visible = postRepository.findByBusinessId(businessId).stream()
                .filter(p -> ownerOrAdmin || !p.isRemoved())
                .toList();
        List<BusinessPost> page = new ArrayList<>();
        if (before == null) {
            visible.stream().filter(BusinessPost::isPinned).findFirst().ifPresent(page::add);
        }
        visible.stream()
                .filter(p -> !(before == null && p.isPinned()))
                .filter(p -> before == null || p.getCreatedAt().isBefore(before))
                .sorted(Comparator.comparing(BusinessPost::getCreatedAt).reversed())
                .limit(PAGE_SIZE)
                .forEach(page::add);
        return toViews(page, Map.of(businessId, business));
    }

    /** The logged-in student's feed: recent posts from businesses they follow. */
    public List<PostView> feed(UUID userId, LocalDateTime before) {
        Set<UUID> followed = followRepository.findByUserId(userId).stream()
                .map(FollowedBusiness::getBusinessId).collect(Collectors.toSet());
        if (followed.isEmpty()) {
            return List.of();
        }
        List<BusinessPost> posts = postRepository.findByBusinessIdIn(followed).stream()
                .filter(p -> !p.isRemoved())
                .filter(p -> before == null || p.getCreatedAt().isBefore(before))
                .sorted(Comparator.comparing(BusinessPost::getCreatedAt).reversed())
                .limit(PAGE_SIZE)
                .toList();
        return toViews(posts, null);
    }

    public PostView get(UUID postId) {
        BusinessPost post = findPost(postId);
        Business business = findBusiness(post.getBusinessId());
        if (post.isRemoved() && !isOwner(business) && !CurrentUserProvider.isAdmin()) {
            throw new ResourceNotFoundException("This post has been removed because it broke UniLinkHub's marketplace rules.");
        }
        return toViews(List.of(post), Map.of(business.getId(), business)).get(0);
    }

    // ------------------------------------------------------------------ writing (owner)

    @Transactional
    public PostView create(UUID authorId, UUID businessId, String body, String imageUrl, UUID listingId) {
        Business business = findBusiness(businessId);
        requireOwner(business, authorId, "post on this page");
        if (business.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new ForbiddenException("Only verified businesses can post updates. Once an admin verifies \""
                    + business.getBusinessName() + "\", you'll be able to post here - it keeps the feed free of spam.");
        }
        String text = validateContent(body, imageUrl, null);
        validateListing(listingId, business);
        long today = postRepository.countByBusinessIdAndCreatedAtAfter(businessId, LocalDateTime.now().minusHours(24));
        if (today >= DAILY_POST_LIMIT) {
            throw new TooManyRequestsException("You've posted " + DAILY_POST_LIMIT + " times in the last 24 hours, which is the "
                    + "daily limit. It stops followers' feeds from being flooded - you can post again tomorrow.");
        }

        BusinessPost post = postRepository.save(BusinessPost.create(businessId, authorId, text, blankToNull(imageUrl), listingId));
        notifyFollowers(business, post);
        return toViews(List.of(post), Map.of(businessId, business)).get(0);
    }

    @Transactional
    public PostView edit(UUID requesterId, UUID postId, String body, String imageUrl, UUID listingId) {
        BusinessPost post = findPost(postId);
        Business business = findBusiness(post.getBusinessId());
        requireOwner(business, requesterId, "edit this post");
        if (post.isRemoved()) {
            throw new ForbiddenException("This post was removed by an admin, so it can't be edited.");
        }
        String text = validateContent(body, imageUrl, post.getImageUrl());
        validateListing(listingId, business);
        post.edit(text, blankToNull(imageUrl), listingId);
        return toViews(List.of(postRepository.save(post)), Map.of(business.getId(), business)).get(0);
    }

    @Transactional
    public void delete(UUID requesterId, UUID postId) {
        BusinessPost post = findPost(postId);
        requireOwner(findBusiness(post.getBusinessId()), requesterId, "delete this post");
        likeRepository.deleteByPostId(postId);
        commentRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    /** One pinned post per page: pinning a post unpins whichever was pinned before. */
    @Transactional
    public PostView setPinned(UUID requesterId, UUID postId, boolean pinned) {
        BusinessPost post = findPost(postId);
        Business business = findBusiness(post.getBusinessId());
        requireOwner(business, requesterId, "pin posts on this page");
        if (pinned && post.isRemoved()) {
            throw new ForbiddenException("This post was removed by an admin, so it can't be pinned.");
        }
        if (pinned) {
            postRepository.findByBusinessId(business.getId()).stream()
                    .filter(p -> p.isPinned() && !p.getId().equals(postId))
                    .forEach(p -> {
                        p.setPinned(false);
                        postRepository.save(p);
                    });
        }
        post.setPinned(pinned);
        return toViews(List.of(postRepository.save(post)), Map.of(business.getId(), business)).get(0);
    }

    // ------------------------------------------------------------------ reactions & comments (students)

    @Transactional
    public PostView like(UUID userId, UUID postId) {
        BusinessPost post = findVisiblePost(postId);
        if (!likeRepository.existsByPostIdAndUserId(postId, userId)) {
            likeRepository.save(PostLike.of(postId, userId));
        }
        return get(post.getId());
    }

    @Transactional
    public PostView unlike(UUID userId, UUID postId) {
        findVisiblePost(postId);
        likeRepository.deleteByPostIdAndUserId(postId, userId);
        return get(postId);
    }

    public List<CommentView> comments(UUID postId) {
        BusinessPost post = findVisiblePost(postId);
        Business business = findBusiness(post.getBusinessId());
        List<PostComment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        Map<UUID, User> authors = usersById(comments.stream().map(PostComment::getAuthorId).collect(Collectors.toSet()));
        return comments.stream().map(c -> toView(c, business, authors.get(c.getAuthorId()))).toList();
    }

    @Transactional
    public CommentView comment(UUID authorId, UUID postId, String body) {
        BusinessPost post = findVisiblePost(postId);
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) {
            throw new BadRequestException("Write a comment before posting it.");
        }
        if (text.length() > PostComment.MAX_LENGTH) {
            throw new BadRequestException("Comments can be up to " + PostComment.MAX_LENGTH + " characters - yours is " + text.length() + ".");
        }
        restrictedItemsPolicy.requireAllowed("comment", text);
        PostComment saved = commentRepository.save(PostComment.of(postId, authorId, text));

        Business business = findBusiness(post.getBusinessId());
        User author = userRepository.findById(authorId).orElse(null);
        if (!business.getOwnerId().equals(authorId)) {
            notificationService.notify(business.getOwnerId(), "POST",
                    (author == null ? "Someone" : author.getFullName()) + " commented on your post: \"" + preview(text) + "\"");
        }
        return toView(saved, business, author);
    }

    /** The comment's author, the page owner (their page, their call) or an admin may delete a comment. */
    @Transactional
    public void deleteComment(UUID requesterId, UUID commentId) {
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that comment. It may already have been deleted."));
        Business business = findBusiness(findPost(comment.getPostId()).getBusinessId());
        if (!canDeleteComment(comment, business, requesterId)) {
            throw new ForbiddenException("You can only delete your own comments, or comments on your own business's posts.");
        }
        commentRepository.delete(comment);
    }

    @Transactional
    public void flag(UUID userId, UUID postId) {
        BusinessPost post = findVisiblePost(postId);
        if (findBusiness(post.getBusinessId()).getOwnerId().equals(userId)) {
            throw new BadRequestException("You can't report your own post - edit or delete it instead.");
        }
        post.flag();
        postRepository.save(post);
    }

    // ------------------------------------------------------------------ admin

    /** Every post, reported ones first. */
    public List<PostView> adminList(boolean flaggedOnly) {
        List<BusinessPost> posts = postRepository.findAll().stream()
                .filter(p -> !flaggedOnly || (p.getFlagCount() > 0 && !p.isRemoved()))
                .sorted(Comparator.comparingInt(BusinessPost::getFlagCount).reversed()
                        .thenComparing(BusinessPost::getCreatedAt, Comparator.reverseOrder()))
                .limit(200)
                .toList();
        return toViews(posts, null);
    }

    @Transactional
    public PostView remove(UUID adminId, UUID postId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Please give a reason for removing this post. The business owner is shown it, so they "
                    + "understand which rule was broken.");
        }
        BusinessPost post = findPost(postId);
        if (post.isRemoved()) {
            throw new BadRequestException("This post has already been removed.");
        }
        post.remove(reason.trim(), LocalDateTime.now());
        postRepository.save(post);
        Business business = findBusiness(post.getBusinessId());
        notificationService.notify(business.getOwnerId(), "MODERATION",
                "An admin removed a post from \"" + business.getBusinessName() + "\" because it broke the marketplace rules: "
                        + reason.trim() + ". Repeated or serious breaches can lead to your account being suspended.");
        String adminName = userRepository.findById(adminId).map(User::getFullName).orElse("An admin");
        auditLogService.record(adminName, "POST", "Removed a post by \"" + business.getBusinessName() + "\" - reason: " + reason.trim());
        return get(postId);
    }

    // ------------------------------------------------------------------ helpers

    private String validateContent(String body, String imageUrl, String currentImageUrl) {
        String text = body == null ? "" : body.trim();
        if (text.isEmpty() && (imageUrl == null || imageUrl.isBlank())) {
            throw new BadRequestException("Write something or add a photo before posting.");
        }
        if (text.length() > BusinessPost.MAX_LENGTH) {
            throw new BadRequestException("Posts can be up to " + String.format("%,d", BusinessPost.MAX_LENGTH)
                    + " characters - yours is " + String.format("%,d", text.length()) + ". Try trimming it down.");
        }
        restrictedItemsPolicy.requireAllowed("post", text);
        ImageService.requireUploadedImageUrl(imageUrl, currentImageUrl);
        return text.isEmpty() ? null : text;
    }

    private void validateListing(UUID listingId, Business business) {
        if (listingId == null) {
            return;
        }
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new BadRequestException("We couldn't find the listing you attached. It may have been deleted."));
        if (!listing.getBusinessId().equals(business.getId())) {
            throw new BadRequestException("You can only attach listings from \"" + business.getBusinessName() + "\" to its posts.");
        }
        if (listing.isTakenDown()) {
            throw new BadRequestException("That listing was removed by an admin, so it can't be attached to a post.");
        }
    }

    private void notifyFollowers(Business business, BusinessPost post) {
        String what = post.getBody() != null ? "\"" + preview(post.getBody()) + "\"" : "a new photo";
        for (FollowedBusiness follower : followRepository.findByBusinessId(business.getId())) {
            if (!follower.getUserId().equals(business.getOwnerId())) {
                notificationService.notify(follower.getUserId(), "POST", business.getBusinessName() + " posted " + what);
            }
        }
    }

    private List<PostView> toViews(List<BusinessPost> posts, Map<UUID, Business> knownBusinesses) {
        if (posts.isEmpty()) {
            return List.of();
        }
        Set<UUID> postIds = posts.stream().map(BusinessPost::getId).collect(Collectors.toSet());
        List<PostLike> allLikes = likeRepository.findByPostIdIn(postIds);
        Map<UUID, Long> likes = allLikes.stream().collect(Collectors.groupingBy(PostLike::getPostId, Collectors.counting()));
        Optional<UUID> me = CurrentUserProvider.currentId();
        Set<UUID> likedByMe = me.map(id -> allLikes.stream()
                .filter(l -> l.getUserId().equals(id)).map(PostLike::getPostId).collect(Collectors.toSet())).orElse(Set.of());
        Map<UUID, Long> comments = commentRepository.findByPostIdIn(postIds).stream()
                .collect(Collectors.groupingBy(PostComment::getPostId, Collectors.counting()));
        Map<UUID, Business> businesses = knownBusinesses != null ? knownBusinesses
                : businessRepository.findAll().stream().collect(Collectors.toMap(Business::getId, Function.identity()));
        boolean admin = CurrentUserProvider.isAdmin();

        return posts.stream().map(p -> {
            Business b = businesses.get(p.getBusinessId());
            boolean owner = b != null && me.map(id -> id.equals(b.getOwnerId())).orElse(false);
            return new PostView(p.getId(), p.getBusinessId(),
                    b == null ? "Unknown business" : b.getBusinessName(),
                    b == null ? null : b.getImageUrl(),
                    b != null && b.getVerificationStatus() == VerificationStatus.VERIFIED,
                    p.getBody(), p.getImageUrl(), attached(p.getListingId()),
                    p.isPinned(), p.isEdited(), p.getCreatedAt(),
                    likes.getOrDefault(p.getId(), 0L), likedByMe.contains(p.getId()),
                    comments.getOrDefault(p.getId(), 0L),
                    owner, admin ? p.getFlagCount() : 0,
                    owner || admin ? p.getRemovedReason() : null);
        }).toList();
    }

    private PostView.AttachedListing attached(UUID listingId) {
        if (listingId == null) {
            return null;
        }
        return listingRepository.findById(listingId)
                .filter(l -> !l.isTakenDown())
                .map(l -> new PostView.AttachedListing(l.getId(), l.getName(), l.getPrice(), l.getImageUrl(), l.getCategory(), l.getStatus().name()))
                .orElse(null);
    }

    private CommentView toView(PostComment c, Business business, User author) {
        UUID me = CurrentUserProvider.currentId().orElse(null);
        return new CommentView(c.getId(), c.getPostId(), c.getAuthorId(),
                author == null ? "A student" : author.getFullName(), c.getBody(), c.getCreatedAt(),
                me != null && canDeleteComment(c, business, me));
    }

    private boolean canDeleteComment(PostComment comment, Business business, UUID requesterId) {
        return comment.getAuthorId().equals(requesterId) || business.getOwnerId().equals(requesterId) || CurrentUserProvider.isAdmin();
    }

    private Map<UUID, User> usersById(Set<UUID> ids) {
        return ids.stream().map(userRepository::findById).flatMap(Optional::stream)
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private boolean isOwner(Business business) {
        return CurrentUserProvider.currentId().map(id -> id.equals(business.getOwnerId())).orElse(false);
    }

    private void requireOwner(Business business, UUID requesterId, String action) {
        if (!business.getOwnerId().equals(requesterId)) {
            throw new ForbiddenException("Only the owner of \"" + business.getBusinessName() + "\" can " + action + ".");
        }
    }

    private BusinessPost findVisiblePost(UUID postId) {
        BusinessPost post = findPost(postId);
        if (post.isRemoved()) {
            throw new ResourceNotFoundException("This post has been removed because it broke UniLinkHub's marketplace rules.");
        }
        return post;
    }

    private BusinessPost findPost(UUID id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that post. It may have been deleted."));
    }

    private Business findBusiness(UUID id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that business. It may have been removed."));
    }

    private static String preview(String text) {
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= PREVIEW_LENGTH ? oneLine : oneLine.substring(0, PREVIEW_LENGTH - 1) + "…";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
