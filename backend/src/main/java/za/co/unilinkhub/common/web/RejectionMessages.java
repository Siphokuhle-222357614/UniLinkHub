package za.co.unilinkhub.common.web;

/**
 * Plain-English explanations for rejections that are raised outside any one module - by the
 * security filters, the framework, or the role guard - so the same wording is used everywhere.
 * Module-specific rejections write their own message where they're thrown.
 */
public final class RejectionMessages {

    public static final String NOT_LOGGED_IN =
            "Please log in to do this. You need a UniLinkHub account for this part of the site.";
    public static final String SESSION_ENDED =
            "Your session has ended. Please log in again to carry on.";
    public static final String ADMIN_ONLY =
            "This part of UniLinkHub is only for administrators. Your account doesn't have admin access.";
    public static final String WRONG_LOGIN =
            "That email and password don't match a UniLinkHub account. Check for typos and try again.";
    public static final String NOT_FOUND =
            "We couldn't find what you were looking for. It may have been moved or removed.";
    public static final String UNREADABLE_REQUEST =
            "We couldn't read the information that was sent. Please refresh the page and try again.";
    public static final String BAD_LINK =
            "That link doesn't look right - part of it isn't valid. Please check it and try again.";
    public static final String IMAGE_TOO_BIG =
            "That image is too big. Please choose an image smaller than 5 MB.";
    public static final String IMAGE_TOO_BIG_TO_STORE =
            "That photo is too large for us to save right now. Please try a smaller photo, or take a screenshot of it and upload that.";
    public static final String CHANGED_AT_SAME_TIME =
            "Someone else changed this at the same moment. Please refresh the page and try again.";
    public static final String UNEXPECTED =
            "Something went wrong on our side. Please try again in a moment - if it keeps happening, let an admin know.";

    /** Stable codes the frontend can react to. */
    public static final String CODE_EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    public static final String CODE_ACCOUNT_SUSPENDED = "ACCOUNT_SUSPENDED";
    public static final String CODE_ADMIN_ACCOUNT = "ADMIN_ACCOUNT";
    public static final String CODE_RESTRICTED_ITEM = "RESTRICTED_ITEM";

    private RejectionMessages() {
    }
}
