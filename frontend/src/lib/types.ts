export interface UserResponse {
  id: string;
  /** Null for admin accounts - they're staff accounts, not student accounts. */
  studentNumber: string | null;
  firstName: string;
  lastName: string;
  email: string;
  pendingEmail: string | null;
  phoneNumber: string | null;
  role: "STUDENT" | "ADMIN";
  accountStatus: "PENDING_VERIFICATION" | "ACTIVE" | "SUSPENDED" | "DEACTIVATED";
  seller: boolean;
  suspensionReason: string | null;
  disabledNotificationCategories: string[];
  /** When the student accepted the marketplace rules; null until they become a seller. */
  sellerRulesAcceptedAt: string | null;
  createdAt: string;
  /** The student's home campus key (e.g. "BELLVILLE"), or null. */
  campus: string | null;
}

export interface AuthResponse {
  token: string;
  user: UserResponse;
}

export interface BusinessDTO {
  id: string;
  ownerId: string;
  businessName: string;
  description: string;
  category: string;
  verificationStatus: "PENDING" | "VERIFIED" | "REJECTED";
  imageUrl: string | null;
  rejectionReason: string | null;
  createdAt: string;
  campus: string | null;
  campusLabel: string | null;
  pickupLocation: string | null;
}

export interface BusinessStatsDTO {
  totalListings: number;
  activeListings: number;
  totalViews: number;
  totalSaves: number;
  followerCount: number;
}

export interface ListingDTO {
  id: string;
  businessId: string;
  type: "PRODUCT" | "SERVICE";
  name: string;
  description: string;
  category: string;
  price: number;
  status: "ACTIVE" | "INACTIVE" | "SOLD_OUT";
  viewCount: number;
  stockQuantity: number | null;
  imageUrl: string | null;
  lowStockThreshold: number | null;
  durationMinutes: number | null;
  availabilitySchedule: string | null;
  createdAt: string;
  savedCount: number;
  /** Set when an admin took the listing down for breaking the marketplace rules. */
  takedownReason: string | null;
  takenDownAt: string | null;
  /** Gallery in display order; the first is the cover (= imageUrl). */
  imageUrls: string[];
  /** Seller details, filled in on browse/detail pages (null elsewhere). */
  businessName: string | null;
  campus: string | null;
  campusLabel: string | null;
  pickupLocation: string | null;
  seller: SellerTrust | null;
}

export interface SellerTrust {
  businessId: string;
  verified: boolean;
  /** Average review rating to one decimal, or null with no reviews yet. */
  rating: number | null;
  reviewCount: number;
  completedOrders: number;
  /** e.g. "within an hour"; null until there's enough chat history to say. */
  responseTimeLabel: string | null;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
}

export interface CampusOption {
  key: string;
  label: string;
}

export interface ReportDTO {
  id: string;
  reporterId: string;
  targetType: "LISTING" | "USER";
  targetId: string;
  reason: string;
  details: string | null;
  status: "OPEN" | "UNDER_REVIEW" | "RESOLVED" | "DISMISSED";
  reviewedByAdminId: string | null;
  adminNote: string | null;
  createdAt: string;
  resolvedAt: string | null;
}

export type ReportStatus = "OPEN" | "UNDER_REVIEW" | "RESOLVED" | "DISMISSED";
export type ReportReason = "MISREPRESENTATION" | "NON_DELIVERY" | "INAPPROPRIATE_CONDUCT" | "SPAM" | "PROHIBITED_ITEM" | "OTHER";

export interface ReporterSummary {
  id: string;
  studentNumber: string;
  fullName: string;
}

export interface TargetSummary {
  type: "LISTING" | "USER";
  id: string;
  label: string;
  secondaryLabel: string | null;
}

export interface ReportSummaryView {
  id: string;
  reason: ReportReason;
  details: string | null;
  status: ReportStatus;
  adminNote: string | null;
  createdAt: string;
  resolvedAt: string | null;
  reporter: ReporterSummary;
  target: TargetSummary;
  totalReportsOnTarget: number;
}

export interface ReportStatusCounts {
  open: number;
  underReview: number;
  resolved: number;
  dismissed: number;
}

export interface AdminBusinessView {
  id: string;
  businessName: string;
  description: string;
  category: string;
  verificationStatus: "PENDING" | "VERIFIED" | "REJECTED";
  createdAt: string;
  updatedAt: string;
  ownerStudentNumber: string;
  ownerFullName: string;
}

export interface ProviderProfileDTO {
  businessId: string;
  businessName: string;
  description: string;
  category: string;
  verificationStatus: "PENDING" | "VERIFIED" | "REJECTED";
  imageUrl: string | null;
  ownerId: string;
  ownerFullName: string;
  activeListingCount: number;
  totalViews: number;
  memberSince: string;
  campus: string | null;
  campusLabel: string | null;
  pickupLocation: string | null;
}

export interface AdminStatsDTO {
  totalStudents: number;
  pendingAccounts: number;
  businesses: { pending: number; verified: number; rejected: number };
  listings: { active: number; inactive: number; soldOut: number };
  reports: ReportStatusCounts;
}

export interface AdminUserDetailDTO {
  user: UserResponse;
  businesses: BusinessDTO[];
  reportsFiled: ReportSummaryView[];
  reportsReceived: ReportSummaryView[];
}

export interface BusinessContactDTO {
  email: string;
  phoneNumber: string | null;
}

export interface AnnouncementDTO {
  id: string;
  message: string;
  active: boolean;
  createdAt: string;
}

export interface AuditLogEntryDTO {
  id: string;
  category: "BUSINESS" | "ACCOUNT" | "REPORT" | "ANNOUNCEMENT" | "REVIEW" | "QUESTION";
  description: string;
  adminName: string;
  createdAt: string;
}

export type NotificationCategory =
  | "BUSINESS"
  | "BOOKING"
  | "STOCK"
  | "REVIEW"
  | "ANNOUNCEMENT"
  | "MESSAGE"
  | "ORDER"
  | "QUESTION"
  | "SAVED_SEARCH"
  | "MODERATION"
  | "POST";

export interface NotificationDTO {
  id: string;
  category: NotificationCategory;
  message: string;
  read: boolean;
  createdAt: string;
}

export type BookingStatus = "PENDING" | "ACCEPTED" | "DECLINED";

export interface BookingSummaryView {
  id: string;
  listingId: string;
  listingName: string;
  businessId: string;
  businessName: string;
  buyerId: string;
  buyerName: string;
  preferredAt: string;
  note: string | null;
  status: BookingStatus;
  declineReason: string | null;
  createdAt: string;
}

export interface BookingStatsDTO {
  total: number;
  pending: number;
  accepted: number;
  declined: number;
  mostBooked: { listingName: string; businessName: string; count: number }[];
}

export interface ReviewView {
  id: string;
  businessId: string;
  businessName: string;
  reviewerId: string;
  reviewerName: string;
  rating: number;
  comment: string | null;
  flagged: boolean;
  flagCount: number;
  createdAt: string;
}

export interface BusinessReviewsDTO {
  average: number;
  total: number;
  distribution: Record<string, number>;
  reviews: ReviewView[];
}

export interface ReviewStatsDTO {
  platformAverage: number;
  totalReviews: number;
  flaggedCount: number;
  reviewedBusinessCount: number;
  topRated: { businessName: string; average: number; count: number }[];
  lowestRated: { businessName: string; average: number; count: number }[];
}

// ---- Messaging ----
export interface ConversationSummaryView {
  id: string;
  businessId: string;
  businessName: string;
  listingId: string | null;
  listingName: string | null;
  counterpartId: string;
  counterpartName: string;
  iAmSeller: boolean;
  lastMessage: string;
  lastMessageAt: string;
  unreadCount: number;
}

export interface MessageDTO {
  id: string;
  conversationId: string;
  senderId: string;
  body: string;
  read: boolean;
  createdAt: string;
}

// ---- Orders ----
export type OrderStatus = "PLACED" | "CONFIRMED" | "READY" | "COMPLETED" | "CANCELLED";

export interface OrderItemDTO {
  listingId: string;
  listingName: string;
  unitPrice: number;
  quantity: number;
}

export interface OrderDTO {
  id: string;
  buyerId: string;
  buyerName: string;
  businessId: string;
  businessName: string;
  status: OrderStatus;
  fulfilmentMethod: string;
  note: string | null;
  promoCode: string | null;
  subtotal: number;
  discountAmount: number;
  total: number;
  cancelReason: string | null;
  items: OrderItemDTO[];
  createdAt: string;
  /** Only present on the buyer's own orders - shown to the seller at handover. */
  pickupCode: string | null;
  /** False for orders placed before pickup codes existed. */
  requiresPickupCode: boolean;
}

export interface OrderStatsDTO {
  totalOrders: number;
  grossValue: number;
  placed: number;
  confirmed: number;
  ready: number;
  completed: number;
  cancelled: number;
  recentOrders: OrderDTO[];
}

// ---- Listing Q&A ----
export interface QuestionView {
  id: string;
  listingId: string;
  listingName: string;
  askerId: string;
  askerName: string;
  questionText: string;
  answerText: string | null;
  answeredAt: string | null;
  flagged: boolean;
  flagCount: number;
  createdAt: string;
}

export interface QuestionStatsDTO {
  totalQuestions: number;
  answeredCount: number;
  pendingCount: number;
  flaggedCount: number;
}

// ---- Promo codes ----
export type DiscountType = "PERCENT" | "FIXED";

export interface PromoCodeDTO {
  id: string;
  businessId: string;
  code: string;
  discountType: DiscountType;
  discountValue: number;
  scopeListingId: string | null;
  scopeListingName: string | null;
  expiresAt: string | null;
  active: boolean;
  usable: boolean;
  usageCount: number;
  createdAt: string;
}

export interface PromoStatsDTO {
  activeCount: number;
  totalRedemptions: number;
  totalDiscountGiven: number;
  topUsed: { code: string; businessName: string; discountLabel: string; usageCount: number; active: boolean }[];
}

// ---- Saved searches ----
export interface SavedSearchDTO {
  id: string;
  label: string;
  keyword: string | null;
  category: string | null;
  maxPrice: number | null;
  listingType: string | null;
  alertsEnabled: boolean;
  newMatchesCount: number;
  createdAt: string;
}

export interface SellerAnalyticsDTO {
  days: number;
  revenue: number;
  previousRevenue: number;
  orders: number;
  previousOrders: number;
  averageOrderValue: number;
  uniqueBuyers: number;
  repeatBuyers: number;
  cancelledOrders: number;
  daily: { date: string; revenue: number; orders: number }[];
  topListings: { listingId: string; name: string; unitsSold: number; revenue: number }[];
}

export interface RestrictedCategory {
  key: string;
  label: string;
  description: string;
  examples: string[];
}

export interface AdminListingView {
  listing: ListingDTO;
  businessName: string;
  businessVerificationStatus: "PENDING" | "VERIFIED" | "REJECTED" | null;
  ownerId: string | null;
  ownerName: string;
  ownerEmail: string | null;
  openReports: number;
}

export interface UploadedImage {
  id: string;
  url: string;
  contentType: string;
  sizeBytes: number;
}

export interface PostView {
  id: string;
  businessId: string;
  businessName: string;
  businessImageUrl: string | null;
  businessVerified: boolean;
  body: string | null;
  imageUrl: string | null;
  listing: { id: string; name: string; price: number; imageUrl: string | null; category: string; status: string } | null;
  pinned: boolean;
  edited: boolean;
  createdAt: string;
  likeCount: number;
  likedByMe: boolean;
  commentCount: number;
  /** True for the business owner: edit, pin, delete, remove comments. */
  canManage: boolean;
  /** Only shown to admins. */
  flagCount: number;
  /** Only shown to the owner and admins, once an admin has removed the post. */
  removedReason: string | null;
}

export interface CommentView {
  id: string;
  postId: string;
  authorId: string;
  authorName: string;
  body: string;
  createdAt: string;
  canDelete: boolean;
}