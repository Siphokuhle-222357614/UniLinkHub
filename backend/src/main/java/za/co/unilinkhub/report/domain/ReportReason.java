package za.co.unilinkhub.report.domain;

public enum ReportReason {
    MISREPRESENTATION,
    NON_DELIVERY,
    INAPPROPRIATE_CONDUCT,
    SPAM,
    /** Alcohol, drugs, weapons or anything else on the marketplace rules' restricted list. */
    PROHIBITED_ITEM,
    OTHER
}
