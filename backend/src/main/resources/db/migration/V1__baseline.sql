-- UniLinkHub baseline schema.
-- Generated from the JPA entities (Hibernate, MySQL dialect) and works on MySQL 8 and MariaDB 10.4+.
-- Never edit a migration that has already run anywhere: add a new V<n>__description.sql instead.
-- Databases created before migrations existed are baselined at this version (spring.flyway.baseline-on-migrate),
-- so this file only ever runs on an empty database.

CREATE TABLE announcements (
    id binary(16) not null,
    active bit not null,
    created_at datetime(6) not null,
    message varchar(500) not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE audit_log_entries (
    id binary(16) not null,
    created_at datetime(6) not null,
    category varchar(30) not null,
    admin_name varchar(200) not null,
    description varchar(500) not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE bookings (
    id binary(16) not null,
    created_at datetime(6) not null,
    preferred_at datetime(6) not null,
    business_id binary(16) not null,
    buyer_id binary(16) not null,
    listing_id binary(16) not null,
    decline_reason varchar(1000),
    note varchar(1000),
    status enum ('ACCEPTED','DECLINED','PENDING') not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE business_posts (
    id binary(16) not null,
    edited bit not null,
    flag_count integer not null,
    pinned bit not null,
    created_at datetime(6) not null,
    removed_at datetime(6),
    author_id binary(16) not null,
    business_id binary(16) not null,
    listing_id binary(16),
    removed_reason varchar(1000),
    body varchar(2000),
    image_url varchar(255),
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE businesses (
    id binary(16) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    owner_id binary(16) not null,
    category varchar(100) not null,
    pickup_location varchar(120),
    business_name varchar(150) not null,
    description varchar(1000) not null,
    image_url varchar(1000),
    rejection_reason varchar(1000),
    campus enum ('BELLVILLE','DISTRICT_SIX','GRANGER_BAY','MOWBRAY','WELLINGTON'),
    verification_status enum ('PENDING','REJECTED','VERIFIED') not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE conversations (
    id binary(16) not null,
    created_at datetime(6) not null,
    business_id binary(16) not null,
    buyer_id binary(16) not null,
    listing_id binary(16),
    seller_id binary(16) not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE followed_businesses (
    id binary(16) not null,
    created_at datetime(6) not null,
    business_id binary(16) not null,
    user_id binary(16) not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE images (
    id binary(16) not null,
    size_bytes integer not null,
    created_at datetime(6) not null,
    uploader_id binary(16) not null,
    content_type varchar(30) not null,
    data mediumblob not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE listing_photos (
    position integer not null,
    listing_id binary(16) not null,
    url varchar(255) not null,
    primary key (position, listing_id)
) ENGINE = InnoDB;

CREATE TABLE listings (
    id binary(16) not null,
    duration_minutes integer,
    low_stock_threshold integer,
    price decimal(10,2) not null,
    stock_quantity integer,
    created_at datetime(6) not null,
    taken_down_at datetime(6),
    view_count bigint not null,
    business_id binary(16) not null,
    listing_type varchar(31) not null,
    category varchar(100) not null,
    name varchar(150) not null,
    takedown_reason varchar(1000),
    description varchar(2000) not null,
    availability_schedule varchar(255),
    image_url varchar(255),
    status enum ('ACTIVE','INACTIVE','SOLD_OUT') not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE messages (
    id binary(16) not null,
    is_read bit not null,
    created_at datetime(6) not null,
    conversation_id binary(16) not null,
    sender_id binary(16) not null,
    body varchar(2000) not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE notifications (
    id binary(16) not null,
    is_read bit not null,
    created_at datetime(6) not null,
    user_id binary(16) not null,
    category varchar(30) not null,
    message varchar(500) not null,
    primary key (id)
) ENGINE = InnoDB;

-- order_items is a JPA element collection, so Hibernate gives it no key of its own. Managed MySQL
-- (Aiven, and others running with sql_require_primary_key=ON) refuses tables without a primary
-- key, so it gets a surrogate one that Hibernate simply never mentions.
CREATE TABLE order_items (
    id bigint not null auto_increment,
    quantity integer not null,
    unit_price decimal(10,2) not null,
    listing_id binary(16) not null,
    order_id binary(16) not null,
    listing_name varchar(150) not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE orders (
    id binary(16) not null,
    discount_amount decimal(10,2) not null,
    pickup_code varchar(4),
    pickup_code_failures integer not null,
    subtotal decimal(10,2) not null,
    total decimal(10,2) not null,
    created_at datetime(6) not null,
    pickup_code_locked_until datetime(6),
    updated_at datetime(6) not null,
    business_id binary(16) not null,
    buyer_id binary(16) not null,
    fulfilment_method varchar(30) not null,
    promo_code varchar(40),
    cancel_reason varchar(1000),
    note varchar(1000),
    status enum ('CANCELLED','COMPLETED','CONFIRMED','PLACED','READY') not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE post_comments (
    id binary(16) not null,
    created_at datetime(6) not null,
    author_id binary(16) not null,
    post_id binary(16) not null,
    body varchar(1000) not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE post_likes (
    id binary(16) not null,
    created_at datetime(6) not null,
    post_id binary(16) not null,
    user_id binary(16) not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE promo_codes (
    id binary(16) not null,
    active bit not null,
    discount_value decimal(10,2) not null,
    total_discount_given decimal(10,2) not null,
    usage_count integer not null,
    created_at datetime(6) not null,
    expires_at datetime(6),
    business_id binary(16) not null,
    scope_listing_id binary(16),
    code varchar(40) not null,
    discount_type enum ('FIXED','PERCENT') not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE questions (
    id binary(16) not null,
    flag_count integer not null,
    flagged bit not null,
    answered_at datetime(6),
    created_at datetime(6) not null,
    asker_id binary(16) not null,
    listing_id binary(16) not null,
    answer_text varchar(1000),
    question_text varchar(1000) not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE reports (
    id binary(16) not null,
    created_at datetime(6) not null,
    resolved_at datetime(6),
    reporter_id binary(16) not null,
    reviewed_by_admin_id binary(16),
    target_id binary(16) not null,
    admin_note varchar(2000),
    details varchar(2000),
    reason enum ('INAPPROPRIATE_CONDUCT','MISREPRESENTATION','NON_DELIVERY','OTHER','PROHIBITED_ITEM','SPAM') not null,
    status enum ('DISMISSED','OPEN','RESOLVED','UNDER_REVIEW') not null,
    target_type enum ('LISTING','USER') not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE reviews (
    id binary(16) not null,
    flag_count integer not null,
    flagged bit not null,
    rating integer not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    business_id binary(16) not null,
    reviewer_id binary(16) not null,
    comment varchar(1000),
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE saved_listings (
    id binary(16) not null,
    created_at datetime(6) not null,
    listing_id binary(16) not null,
    user_id binary(16) not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE saved_searches (
    id binary(16) not null,
    alerts_enabled bit not null,
    max_price decimal(10,2),
    created_at datetime(6) not null,
    last_viewed_at datetime(6) not null,
    user_id binary(16) not null,
    listing_type varchar(20),
    category varchar(100),
    keyword varchar(150),
    label varchar(150) not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE stock_alerts (
    id binary(16) not null,
    created_at datetime(6) not null,
    listing_id binary(16) not null,
    user_id binary(16) not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE TABLE users (
    id binary(16) not null,
    is_seller bit not null,
    created_at datetime(6) not null,
    password_reset_token_expires_at datetime(6),
    seller_rules_accepted_at datetime(6),
    verification_token_expires_at datetime(6),
    phone_number varchar(32),
    student_number varchar(32),
    email_change_token varchar(64),
    password_reset_token varchar(64),
    verification_token varchar(64),
    first_name varchar(100) not null,
    last_name varchar(100) not null,
    disabled_notification_categories varchar(200),
    email varchar(254) not null,
    pending_email varchar(254),
    suspension_reason varchar(1000),
    password_hash varchar(255) not null,
    account_status enum ('ACTIVE','DEACTIVATED','PENDING_VERIFICATION','SUSPENDED') not null,
    campus enum ('BELLVILLE','DISTRICT_SIX','GRANGER_BAY','MOWBRAY','WELLINGTON'),
    role enum ('ADMIN','STUDENT') not null,
    primary key (id)
) ENGINE = InnoDB;

CREATE INDEX idx_business_posts_business on business_posts (business_id);

ALTER TABLE conversations
    ADD CONSTRAINT uk_conversations_business_buyer UNIQUE (business_id, buyer_id);
ALTER TABLE followed_businesses
    ADD CONSTRAINT uk_followed_businesses_user_business UNIQUE (user_id, business_id);
ALTER TABLE post_likes
    ADD CONSTRAINT uk_post_likes UNIQUE (post_id, user_id);
ALTER TABLE promo_codes
    ADD CONSTRAINT uk_promo_codes_business_code UNIQUE (business_id, code);
ALTER TABLE reviews
    ADD CONSTRAINT uk_reviews_business_reviewer UNIQUE (business_id, reviewer_id);
ALTER TABLE saved_listings
    ADD CONSTRAINT uk_saved_listings_user_listing UNIQUE (user_id, listing_id);
ALTER TABLE stock_alerts
    ADD CONSTRAINT uk_stock_alerts_listing_user UNIQUE (listing_id, user_id);
ALTER TABLE users
    ADD CONSTRAINT uk_users_email UNIQUE (email);
ALTER TABLE users
    ADD CONSTRAINT uk_users_student_number UNIQUE (student_number);
ALTER TABLE listing_photos
    ADD CONSTRAINT fk_listing_photos_listing FOREIGN KEY (listing_id) REFERENCES listings (id);

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id);
