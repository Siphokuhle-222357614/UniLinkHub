package za.co.unilinkhub.qa.application;

public record QuestionStatsDTO(
        long totalQuestions,
        long answeredCount,
        long pendingCount,
        long flaggedCount
) {
}
