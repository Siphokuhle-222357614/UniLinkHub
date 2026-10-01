import type { ReportReason } from "./types";

/** One list of report reasons for the report forms, the student dashboard and the admin console. */
export const REPORT_REASONS: { value: ReportReason; label: string }[] = [
  { value: "PROHIBITED_ITEM", label: "Restricted or illegal item (alcohol, drugs, weapons...)" },
  { value: "MISREPRESENTATION", label: "Misrepresentation" },
  { value: "NON_DELIVERY", label: "Non-delivery" },
  { value: "INAPPROPRIATE_CONDUCT", label: "Inappropriate conduct" },
  { value: "SPAM", label: "Spam" },
  { value: "OTHER", label: "Other" },
];

export const REASON_LABELS: Record<string, string> = {
  PROHIBITED_ITEM: "Restricted or illegal item",
  MISREPRESENTATION: "Misrepresentation",
  NON_DELIVERY: "Non-delivery",
  INAPPROPRIATE_CONDUCT: "Inappropriate conduct",
  SPAM: "Spam",
  OTHER: "Other",
};
