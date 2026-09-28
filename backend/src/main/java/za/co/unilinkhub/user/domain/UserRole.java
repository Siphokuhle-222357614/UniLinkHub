package za.co.unilinkhub.user.domain;

/**
 * Authorization level only. Selling is a capability (see {@link User#isSeller()}), not a role
 * a student has to pick at signup — matching the single-account model in the project docs.
 */
public enum UserRole {
    STUDENT,
    ADMIN
}
