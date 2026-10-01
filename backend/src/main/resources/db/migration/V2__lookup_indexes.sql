-- Indexes for the lookups the app runs on every page. Hibernate never created these, so tables
-- were scanned in full once they grew. (Columns already covered by a unique constraint are skipped.)

CREATE INDEX idx_listings_business ON listings (business_id);
CREATE INDEX idx_listings_status_category ON listings (status, category);

CREATE INDEX idx_businesses_owner ON businesses (owner_id);

CREATE INDEX idx_orders_buyer ON orders (buyer_id, created_at);
CREATE INDEX idx_orders_business_status ON orders (business_id, status);

CREATE INDEX idx_conversations_buyer ON conversations (buyer_id);
CREATE INDEX idx_conversations_seller ON conversations (seller_id);
CREATE INDEX idx_messages_conversation ON messages (conversation_id, created_at);

CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);

CREATE INDEX idx_bookings_business ON bookings (business_id);
CREATE INDEX idx_bookings_buyer ON bookings (buyer_id);

CREATE INDEX idx_questions_listing ON questions (listing_id);
CREATE INDEX idx_reports_status ON reports (status);
CREATE INDEX idx_reports_target ON reports (target_id);

CREATE INDEX idx_followed_businesses_business ON followed_businesses (business_id);
CREATE INDEX idx_saved_listings_listing ON saved_listings (listing_id);
CREATE INDEX idx_saved_searches_user ON saved_searches (user_id);

CREATE INDEX idx_post_comments_post ON post_comments (post_id);
