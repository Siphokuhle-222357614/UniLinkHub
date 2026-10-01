package za.co.unilinkhub.listing.repository;

/** How many live listings a category has - counted by the database, not by loading every listing. */
public record CategoryCount(String category, long count) {
}
