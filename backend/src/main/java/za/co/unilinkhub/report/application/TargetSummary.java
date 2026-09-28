package za.co.unilinkhub.report.application;

import java.util.UUID;

/**
 * label/secondaryLabel are pre-formatted for display: a listing's name + its business, or a
 * user's name + student number - the admin UI shouldn't have to know the difference.
 */
public record TargetSummary(String type, UUID id, String label, String secondaryLabel) {
}
