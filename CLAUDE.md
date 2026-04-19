# Maintenance Center Platform — Claude Code Context

## 🎯 Project Overview

A service marketplace connecting users with verified maintenance centers across Kuwait
and the broader Middle East. Users can discover, review, and book repair services for
cars, electronics, and home appliances. Expansion into restaurants, hotels, and other
service verticals is planned.

**Target Market:** Kuwait (primary), GCC / Middle East (expansion)
**Languages:** Arabic (primary), English
**Status:** ✅ Backend fully implemented and tested. ✅ Center-owner app complete.
🔄 Customer app Phase 2 ~90% complete.

---

## 🏗️ Architecture

### Overall Strategy
- **Phase 1:** Modular Spring Boot monolith (current) — fast to build, easy to debug
- **Phase 2:** Extract to microservices as traffic and team grow
- Domain boundaries are kept clean now to make future extraction straightforward

### Repository Structure (3 Separate Repos)
```
service-center/               # Spring Boot API — the backend repo (ACTIVE)
maintenance-customer-app/     # React Native — customer-facing (~30 screens)
maintenance-center-app/       # React Native — center owner (~20 screens)
```

The backend module is named `service-center`, Maven artifact ID is `service-center`,
group `com.maintainance`. The root IntelliJ project is named `life-experience-app`.

### Actual Backend Package Structure
```
service-center/src/main/java/com/maintainance/service_center/
├── address/        # Address (embeddable) + AddressRequest DTO
├── admin/          # AdminController, AdminService — center owner approval endpoints
├── auth/           # AuthController, AuthService, RegistrationRequest, AuthRequest/Response
├── booking/        # Booking entity + enums + BookingController, BookingService, DTOs
├── category/       # ServiceCategory entity + Repository + Controller + Response
├── center/         # MaintenanceCenter entity + full CRUD service + DTOs + Controller
├── chat/           # Conversation, Message entities + ChatController, ChatService, WebSocketConfig
├── common/         # PageResponse<T>
├── complaint/      # Complaint entity + enums + Controller + Service + DTOs
├── config/         # BeansConfig, FileStorageService, AdminSeeder, LocalTimeDeserializer,
│                   # StorageProperties, WebMvcConfig
├── email/          # EmailService, EmailTemplateName
├── favorite/       # UserFavorite entity + Controller + Service + DTOs
├── handler/        # GlobalExceptionHandling, ExceptionResponse, BusinessErrorCodes
├── notification/   # Notification entity + enums + Controller + Service + DTOs
├── review/         # Review entity + ReviewController, ReviewService, ReviewResponse
├── role/           # Role entity, RoleRepository
├── search/         # SearchHistory entity, SearchSource enum + SearchController, SearchService
├── security/       # JwtService, JwtFilter, SecurityConfig, UserDetailsServiceImpl
└── user/           # User, Token, repositories, UserController, UserService, DTOs,
                    # UserType, ApprovalStatus, Language

# Planned packages (Phase 3.5+)
├── pricing/        # 🆕 Phase 3.5: CenterServicePricing + Controller + Service
├── workprogress/   # 🆕 Phase 4.0: BookingWorkProgress, WorkStage enum
├── loyalty/        # 🆕 Phase 4.5: LoyaltyAccount, LoyaltyTransaction, Rewards
├── vehicle/        # 🆕 Phase 4.5: UserVehicle, MaintenanceReminder
└── referral/       # 🆕 Phase 4.5: ReferralCode, Referral tracking
```

---

## 🛠️ Tech Stack

### Backend (from actual pom.xml)
| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot **3.5.6** |
| Language | Java **17** (compiled), JDK 21 in IntelliJ |
| Database | PostgreSQL **15** |
| ORM | Spring Data JPA / Hibernate |
| Auth | JWT via **jjwt 0.11.5** (api + impl + jackson) |
| Email | JavaMailSender + Thymeleaf |
| Validation | spring-boot-starter-validation |
| Security | spring-boot-starter-security |
| API Docs | springdoc-openapi-starter-webmvc-ui **2.1.0** |
| Lombok | 1.18.40 (annotation processor in compiler.xml) |
| Build | Maven wrapper (mvnw, Maven 3.9.11) |
| Containers | Docker Compose — postgres + maildev |
| Push Notifications | Firebase Admin SDK **9.2.0** |

### Mobile Apps (both repos active)
| Layer | Technology |
|-------|-----------|
| Framework | React Native 0.81.5 + Expo SDK 54 |
| Language | TypeScript |
| Navigation | Expo Router (file-based) |
| State | Redux Toolkit + RTK Query |
| Real-time | WebSocket / STOMP |
| Push Notifs | expo-notifications + FCM |
| i18n | react-i18next (Arabic RTL + English) |

---

## 🗄️ Database — Actual State

### Docker Compose Services
- **postgres** — container `le-postgres`, image `postgres:15`, port `5432`
  - DB name: **`experience`** (not `maintenance_db`)
  - Username: `abdelqodous`, Password: `P@ssw0rd`
- **mail-dev** — container `le-mail-dev`, image `maildev/maildev`
  - Web UI: port `1080`, SMTP: port `1025`

### Tables That Already Exist in DB
| Table | Notes |
|-------|-------|
| `_user` | Underscore prefix avoids `user` reserved word conflict |
| `_user_roles` | Join table: users ↔ roles |
| `role` | Roles table |
| `token` | Email verification OTP tokens |

Sequences: `_user_seq`, `role_seq`, `token_seq`

### Actual `_user` Columns
`id` (int PK), `account_locked` (bool), `created_date` (timestamp),
`date_of_birth` (date), `email` (varchar unique), `enabled` (bool),
`firstname` (varchar), `last_modified_date` (timestamp), `lastname` (varchar),
`password` (varchar)

### Actual `token` Columns
`id` (int PK), `created_at` (timestamp), `expires_at` (timestamp),
`token` (varchar — raw 6-digit OTP string), `validated_at` (timestamp),
`user_id` (int FK → `_user.id`)

### All Tables Now in DB
All domain entities are persisted. Active tables: `_user`, `_user_roles`, `role`,
`token`, `maintenance_centers`, `service_categories`, `center_categories`,
`booking`, `review`, `conversations`, `messages`, `complaint`, `notification`,
`user_favorite`, `search_history`

### Phase 3.5+ New Tables (upcoming migrations)

#### 1. FCM Token Storage (add to `_user`)
```sql
ALTER TABLE _user ADD COLUMN fcm_token VARCHAR(500);
ALTER TABLE _user ADD COLUMN fcm_token_updated_at TIMESTAMP;
```

#### 2. Verified Reviews (add to `review`)
```sql
ALTER TABLE review ADD COLUMN is_verified BOOLEAN DEFAULT false;
ALTER TABLE review ADD COLUMN verification_type VARCHAR(50);
ALTER TABLE review ADD COLUMN booking_id INTEGER REFERENCES booking(id);
CREATE INDEX idx_review_verified ON review(center_id, is_verified);
```

#### 3. Center Service Pricing
```sql
CREATE TABLE center_service_pricing (
    id SERIAL PRIMARY KEY,
    center_id INTEGER NOT NULL REFERENCES maintenance_centers(id),
    service_type VARCHAR(50) NOT NULL,
    service_name_ar VARCHAR(200) NOT NULL,
    service_name_en VARCHAR(200) NOT NULL,
    min_price DECIMAL(10,3),
    max_price DECIMAL(10,3),
    typical_duration_minutes INTEGER,
    description_ar TEXT,
    description_en TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP,
    UNIQUE(center_id, service_type, service_name_en)
);
CREATE INDEX idx_center_pricing_center ON center_service_pricing(center_id);
```

#### 4. Work Progress Tracking (Phase 4.0)
```sql
ALTER TABLE booking ADD COLUMN work_stage VARCHAR(50) DEFAULT 'RECEIVED';
ALTER TABLE booking ADD COLUMN work_stage_updated_at TIMESTAMP;
ALTER TABLE booking ADD COLUMN estimated_completion TIMESTAMP;

CREATE TABLE booking_work_progress (
    id SERIAL PRIMARY KEY,
    booking_id INTEGER NOT NULL REFERENCES booking(id),
    stage VARCHAR(50) NOT NULL,
    notes TEXT,
    notes_ar TEXT,
    internal_notes TEXT,
    photo_url VARCHAR(500),
    video_url VARCHAR(500),
    estimated_minutes_remaining INTEGER,
    created_by INTEGER REFERENCES _user(id),
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_booking_progress_booking ON booking_work_progress(booking_id);
```

#### 5. Booking Media (Phase 4.0)
```sql
CREATE TABLE booking_media (
    id SERIAL PRIMARY KEY,
    booking_id INTEGER NOT NULL REFERENCES booking(id),
    media_type VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    caption TEXT,
    caption_ar TEXT,
    is_visible_to_customer BOOLEAN DEFAULT true,
    uploaded_by INTEGER REFERENCES _user(id),
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_booking_media_booking ON booking_media(booking_id);
```

#### 6. Booking Quotes (Phase 4.0)
```sql
CREATE TABLE booking_quotes (
    id SERIAL PRIMARY KEY,
    booking_id INTEGER NOT NULL REFERENCES booking(id),
    version INTEGER NOT NULL DEFAULT 1,
    line_items JSONB NOT NULL,
    subtotal DECIMAL(10,3) NOT NULL,
    discount_amount DECIMAL(10,3) DEFAULT 0,
    discount_reason TEXT,
    tax_amount DECIMAL(10,3) DEFAULT 0,
    total_amount DECIMAL(10,3) NOT NULL,
    estimated_duration_minutes INTEGER,
    notes TEXT,
    notes_ar TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT',
    sent_at TIMESTAMP,
    responded_at TIMESTAMP,
    response_notes TEXT,
    created_by INTEGER REFERENCES _user(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);
ALTER TABLE booking ADD COLUMN approved_quote_id INTEGER REFERENCES booking_quotes(id);
ALTER TABLE booking ADD COLUMN quoted_amount DECIMAL(10,3);
ALTER TABLE booking ADD COLUMN final_amount DECIMAL(10,3);
```

#### 7. Trust Score & Badges (Phase 4.0)
```sql
ALTER TABLE maintenance_centers ADD COLUMN trust_score INTEGER DEFAULT 0;
ALTER TABLE maintenance_centers ADD COLUMN trust_score_updated_at TIMESTAMP;
ALTER TABLE maintenance_centers ADD COLUMN trust_level VARCHAR(20);

CREATE TABLE center_badges (
    id SERIAL PRIMARY KEY,
    center_id INTEGER NOT NULL REFERENCES maintenance_centers(id),
    badge_type VARCHAR(50) NOT NULL,
    earned_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP,
    metadata JSONB,
    UNIQUE(center_id, badge_type)
);
CREATE INDEX idx_center_badges_center ON center_badges(center_id);
```

#### 8. Loyalty System (Phase 4.5)
```sql
CREATE TABLE loyalty_accounts (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE REFERENCES _user(id),
    total_points INTEGER DEFAULT 0,
    available_points INTEGER DEFAULT 0,
    lifetime_points INTEGER DEFAULT 0,
    tier VARCHAR(20) DEFAULT 'BRONZE',
    tier_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE loyalty_transactions (
    id SERIAL PRIMARY KEY,
    account_id INTEGER NOT NULL REFERENCES loyalty_accounts(id),
    transaction_type VARCHAR(50) NOT NULL,
    points INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    reference_type VARCHAR(50),
    reference_id INTEGER,
    description TEXT,
    description_ar TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE loyalty_rewards (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name_ar VARCHAR(200) NOT NULL,
    name_en VARCHAR(200) NOT NULL,
    description_ar TEXT,
    description_en TEXT,
    points_required INTEGER NOT NULL,
    reward_type VARCHAR(50) NOT NULL,
    reward_value DECIMAL(10,3),
    min_booking_amount DECIMAL(10,3),
    max_discount_amount DECIMAL(10,3),
    valid_days INTEGER DEFAULT 30,
    tier_required VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_rewards (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES _user(id),
    reward_id INTEGER NOT NULL REFERENCES loyalty_rewards(id),
    redemption_code VARCHAR(20) UNIQUE NOT NULL,
    points_spent INTEGER NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    used_on_booking_id INTEGER REFERENCES booking(id),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    used_at TIMESTAMP
);
```

#### 9. Vehicle & Maintenance Reminders (Phase 4.5)
```sql
CREATE TABLE user_vehicles (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES _user(id),
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    year INTEGER NOT NULL,
    license_plate VARCHAR(20),
    vin VARCHAR(50),
    color VARCHAR(50),
    current_mileage INTEGER,
    mileage_updated_at TIMESTAMP,
    nickname VARCHAR(100),
    photo_url VARCHAR(500),
    is_primary BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);
ALTER TABLE booking ADD COLUMN vehicle_id INTEGER REFERENCES user_vehicles(id);

CREATE TABLE maintenance_schedule_templates (
    id SERIAL PRIMARY KEY,
    service_code VARCHAR(50) UNIQUE NOT NULL,
    name_ar VARCHAR(200) NOT NULL,
    name_en VARCHAR(200) NOT NULL,
    description_ar TEXT,
    description_en TEXT,
    service_type VARCHAR(50) NOT NULL,
    default_interval_months INTEGER,
    default_interval_km INTEGER,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    is_active BOOLEAN DEFAULT true
);

CREATE TABLE vehicle_maintenance_reminders (
    id SERIAL PRIMARY KEY,
    vehicle_id INTEGER NOT NULL REFERENCES user_vehicles(id),
    template_id INTEGER REFERENCES maintenance_schedule_templates(id),
    custom_name_ar VARCHAR(200),
    custom_name_en VARCHAR(200),
    due_date DATE,
    due_mileage INTEGER,
    last_service_date DATE,
    last_service_mileage INTEGER,
    last_booking_id INTEGER REFERENCES booking(id),
    is_completed BOOLEAN DEFAULT false,
    is_snoozed BOOLEAN DEFAULT false,
    snooze_until DATE,
    notification_sent_7_days BOOLEAN DEFAULT false,
    notification_sent_3_days BOOLEAN DEFAULT false,
    notification_sent_today BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);
```

#### 10. Referral System (Phase 4.5)
```sql
CREATE TABLE referral_codes (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE REFERENCES _user(id),
    code VARCHAR(20) UNIQUE NOT NULL,
    total_referrals INTEGER DEFAULT 0,
    successful_referrals INTEGER DEFAULT 0,
    total_earnings_points INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE referrals (
    id SERIAL PRIMARY KEY,
    referrer_id INTEGER NOT NULL REFERENCES _user(id),
    referred_id INTEGER NOT NULL UNIQUE REFERENCES _user(id),
    referral_code VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    referrer_reward_points INTEGER,
    referred_reward_points INTEGER,
    referred_discount_percent INTEGER,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE _user ADD COLUMN referred_by_code VARCHAR(20);
ALTER TABLE _user ADD COLUMN referred_by_user_id INTEGER REFERENCES _user(id);
```

### Key JPA Patterns Used in This Codebase
- **No shared `BaseEntity`** — each entity has its own `@CreatedDate` / `@LastModifiedDate`
  with `@EntityListeners(AuditingEntityListener.class)` directly
- `@EnableJpaAuditing` is on `ServiceCenterApplication`
- `Role.id` → `@GeneratedValue` (no strategy specified — uses TABLE/sequence)
- `Token.id` → `@GeneratedValue` (same)
- `User.id` → `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- `Address` is `@Embeddable`, embedded in `User` and `MaintenanceCenter`
- Bilingual pattern: `nameAr` / `nameEn`, `descriptionAr` / `descriptionEn`
  — always use this pattern for any user-facing string field

---

## 🔐 Authentication — Actual Implementation

### Working Endpoints
```
POST  /api/v1/auth/register           → 202 Accepted (sends OTP email)
POST  /api/v1/auth/authenticate       → 200 { "token": "<jwt>" }
GET   /api/v1/auth/activate-account   → ?token=XXXXXX
POST  /api/v1/auth/register-owner     → center owner self-registration
```

### How the Flow Works
1. `register` → saves `User` (`enabled=false`) → generates 6-digit numeric OTP
   → saves to `token` table → sends via MailDev email
2. `activate-account` → finds token → checks expiry → if expired, regenerates + resends
   → sets `user.enabled=true` + `token.validatedAt`
3. `authenticate` → Spring Security `AuthenticationManager` → JWT with `fullName` claim
   + authorities list

### JWT Config (application-dev.yml)
- Property: `application.security.jwt.secret-key` (256-bit base64)
- Property: `application.security.jwt.expiration` = `8640000` ms (**2.4 hours**)
- Activation URL: `application.mail.frontend.activation-url` = `http://localhost:4200/activate-account`

---

---

## 📡 API Design

- Server context path: `/api/v1/` (set in `application.yml`)
- Auth controller: `@RequestMapping("auth")` → resolves to `/api/v1/auth/**`
- Swagger UI: `http://localhost:8080/api/v1/swagger-ui/index.html`
- Authenticated endpoints require: `Authorization: Bearer <jwt>`
- Pagination: `page`, `size`, `sort` query params (Spring Pageable)

### Error Response (GlobalExceptionHandling)
```json
{
  "businessErrorCode": 304,
  "businessErrorDescription": "Username and / or password is incorrect",
  "error": "string",
  "validationErrors": ["field error message"],
  "errors": { "field": "message" }
}
```
Fields are omitted when null/empty (`@JsonInclude(NON_EMPTY)`).

### BusinessErrorCodes (existing)
| Code | Status | Description |
|------|--------|-------------|
| 300 | 400 | Incorrect current password |
| 301 | 400 | New password mismatch |
| 302 | 403 | Account locked |
| 303 | 403 | Account disabled |
| 304 | 403 | Bad credentials |

### All API Endpoints

```
# Auth
POST   /api/v1/auth/register
POST   /api/v1/auth/authenticate
GET    /api/v1/auth/activate-account?token=XXXXXX
POST   /api/v1/auth/register-owner

# Users
GET    /api/v1/users/me
PUT    /api/v1/users/me
PUT    /api/v1/users/me/password
PUT    /api/v1/users/me/fcm-token          # Phase 3.5

# Centers
GET    /api/v1/centers
GET    /api/v1/centers/{id}
GET    /api/v1/centers/{id}/pricing        # Phase 3.5
GET    /api/v1/centers/my/profile
PUT    /api/v1/centers/my
POST   /api/v1/centers/my/images
DELETE /api/v1/centers/my/images/{imageId}
GET    /api/v1/centers/my/pricing          # Phase 3.5
POST   /api/v1/centers/my/pricing          # Phase 3.5
PUT    /api/v1/centers/my/pricing/{id}     # Phase 3.5
DELETE /api/v1/centers/my/pricing/{id}     # Phase 3.5

# Bookings
GET    /api/v1/bookings
GET    /api/v1/bookings/{id}
POST   /api/v1/bookings
PUT    /api/v1/bookings/{id}/status
PUT    /api/v1/bookings/{id}/cancel
PUT    /api/v1/bookings/{id}/work-stage    # Phase 4.0
POST   /api/v1/bookings/{id}/work-progress # Phase 4.0
GET    /api/v1/bookings/{id}/work-progress # Phase 4.0
GET    /api/v1/bookings/{id}/live-status   # Phase 4.0
POST   /api/v1/bookings/{id}/media         # Phase 4.0
GET    /api/v1/bookings/{id}/media         # Phase 4.0
GET    /api/v1/bookings/{id}/media/gallery # Phase 4.0
POST   /api/v1/bookings/{id}/quotes        # Phase 4.0
GET    /api/v1/bookings/{id}/quotes        # Phase 4.0
POST   /api/v1/bookings/{id}/quotes/{quoteId}/send    # Phase 4.0
POST   /api/v1/bookings/{id}/quotes/{quoteId}/approve # Phase 4.0
POST   /api/v1/bookings/{id}/quotes/{quoteId}/reject  # Phase 4.0

# Reviews
GET    /api/v1/reviews/center?centerId={id}
GET    /api/v1/reviews/my
POST   /api/v1/reviews
PUT    /api/v1/reviews/{id}/reply

# Chat
GET    /api/v1/conversations
GET    /api/v1/conversations/{id}/messages
POST   /api/v1/conversations
WebSocket: /ws/chat

# Notifications
GET    /api/v1/notifications
PUT    /api/v1/notifications/{id}/read
PUT    /api/v1/notifications/read-all

# Loyalty (Phase 4.5)
GET    /api/v1/loyalty/account
GET    /api/v1/loyalty/transactions
GET    /api/v1/loyalty/rewards
POST   /api/v1/loyalty/rewards/{id}/redeem
GET    /api/v1/loyalty/my-rewards
POST   /api/v1/loyalty/validate-code

# Vehicles (Phase 4.5)
GET    /api/v1/vehicles
POST   /api/v1/vehicles
PUT    /api/v1/vehicles/{id}
DELETE /api/v1/vehicles/{id}
PUT    /api/v1/vehicles/{id}/mileage
GET    /api/v1/vehicles/{id}/dashboard
GET    /api/v1/vehicles/{id}/reminders
POST   /api/v1/vehicles/{id}/reminders
PUT    /api/v1/reminders/{id}/complete
PUT    /api/v1/reminders/{id}/snooze

# Referrals (Phase 4.5)
GET    /api/v1/referral/code
GET    /api/v1/referral/stats
GET    /api/v1/referral/share-content
GET    /api/v1/referral/validate/{code}

# Favorites, Complaints, Search, Admin, Categories (existing)
```

---

## 🌍 Localization & Regional Context

- Bilingual entity fields: always `nameAr` + `nameEn`, `descriptionAr` + `descriptionEn`
- Currency: Kuwaiti Dinar (KD) — use `DECIMAL(10,3)` for fils precision
- Phone: Kuwait format `+965 XXXX XXXX`
- Distance: kilometers
- Time zone: Asia/Kuwait (UTC+3)
- Mobile UI: all strings via i18n keys — never hardcode display text

---

## ⚙️ Development Environment

### Running the Backend
```bash
# Start Docker services (postgres + maildev)
docker-compose up -d

# Run Spring Boot (from repo root)
cd service-center
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Endpoints
# API:         http://localhost:8080/api/v1/
# Swagger UI:  http://localhost:8080/api/v1/swagger-ui/index.html
# MailDev:     http://localhost:1080
```

### Running Tests
```bash
cd service-center
./mvnw test
```

---

## 📋 Coding Standards

### Java / Spring Boot
- **`@RequiredArgsConstructor`** for constructor injection — no `@Autowired` on fields
- **`@Slf4j`** for logging (already used in `AuthenticationService`)
- DTOs for all request/response — never expose JPA entities in controllers
- `@Valid` on all `@RequestBody` parameters
- Service layer owns all business logic — controllers are thin
- Never call `Optional.get()` without checking — use `orElseThrow()`
- `@Transactional` on service methods that write
- `@Transactional(propagation = Propagation.REQUIRES_NEW)` for independent sub-operations
- Passwords always `BCryptPasswordEncoder` — never plain

### Entity Conventions
- Lombok: `@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor` on entities
- Auditing: `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate` / `@LastModifiedDate`
- Bilingual string fields: `nameAr` / `nameEn` pair — always both

### New Feature Pattern (7 steps)
1. Database migration (SQL `ALTER TABLE` / `CREATE TABLE`)
2. Entity class
3. Repository interface
4. Service class
5. Request/Response DTOs
6. Controller
7. Tests

### Existing Enums (do not redefine)
`BookingStatus`, `ServiceType`, `PaymentMethod`, `PaymentStatus`, `CancelledBy`,
`MessageType`, `SenderType`, `ComplaintType`, `ComplaintStatus`, `ComplaintPriority`,
`NotificationType`, `NotificationPriority`, `UserType`, `Language`, `SearchSource`

### New Enums (Phase 3.5+ — define when implementing)

```java
public enum WorkStage {
    RECEIVED, DIAGNOSING, QUOTE_READY, QUOTE_APPROVED, QUOTE_REJECTED,
    PARTS_ORDERED, PARTS_RECEIVED, WORK_IN_PROGRESS,
    QUALITY_CHECK, READY_FOR_PICKUP, PICKED_UP
}

public enum MediaCategory {
    VEHICLE_ARRIVAL, ISSUE_FOUND, PARTS_USED, WORK_IN_PROGRESS,
    BEFORE_REPAIR, AFTER_REPAIR, QUALITY_CHECK, CUSTOMER_PICKUP
}

public enum QuoteStatus {
    DRAFT, SENT, APPROVED, REJECTED, REVISED
}

public enum BadgeType {
    VERIFIED_LICENSE, VERIFIED_LOCATION, TOP_RATED, FAST_RESPONDER,
    COMPLETION_CHAMPION, ON_TIME_EXPERT, VETERAN_100, VETERAN_500, VETERAN_1000,
    WARRANTY_PARTNER, PRICE_TRANSPARENT, QUICK_SERVICE, PHOTO_PROOF_ALWAYS, CUSTOMER_FAVORITE
}

public enum LoyaltyTier {
    BRONZE(1.0), SILVER(1.25), GOLD(1.5), PLATINUM(2.0);
    // Thresholds: Silver=1000, Gold=5000, Platinum=15000 lifetime points
    private final double multiplier;
}

public enum LoyaltyTransactionType {
    EARN_BOOKING, EARN_REVIEW, EARN_REFERRAL, EARN_FIRST_BOOKING, EARN_BONUS,
    REDEEM, EXPIRE, ADJUST
}

// Add to existing NotificationType enum:
// WORK_STAGE_UPDATE, QUOTE_READY, QUOTE_APPROVED, QUOTE_REJECTED,
// MAINTENANCE_REMINDER_7_DAYS, MAINTENANCE_REMINDER_3_DAYS,
// MAINTENANCE_REMINDER_TODAY, MAINTENANCE_REMINDER_OVERDUE,
// POINTS_EARNED, POINTS_EXPIRING, TIER_UPGRADE, REWARD_EXPIRING,
// REFERRAL_SIGNUP, REFERRAL_COMPLETED
```

---

## 🚀 Development Phases

### Phase 1 — Backend ✅ Complete
- [x] Project structure, Docker Compose, Maven setup
- [x] User, Role, Token entities + DB tables
- [x] Auth: register, OTP email, activate, login (JWT)
- [x] Email service (MailDev in dev)
- [x] Global exception handling + BusinessErrorCodes
- [x] Swagger / OpenAPI
- [x] All domain entities + services + controllers (Center, Booking, Review, Chat, etc.)
- [x] Fixed all 7 original bugs
- [x] File upload (center images/logo)
- [x] WebSocket / STOMP real-time chat
- [x] Notification service
- [x] Admin approval gate for center owners

### Phase 2 — Center Owner App ✅ Complete
- [x] Expo Router navigation, auth guard, session persistence
- [x] 401 auto-logout middleware, Arabic RTL + English i18n
- [x] Dashboard, Bookings list + detail + status update
- [x] Profile editor (bilingual address, categories, images)
- [x] Reviews list + owner reply
- [x] Chat list + thread + WebSocket/STOMP real-time
- [x] Notifications list + mark read
- [x] Push notifications (FCM token registration)
- [x] Multi-branch support + CENTER_OWNER self-registration + admin approval gate

### Phase 3 — Customer App 🔄 ~90% Complete
- [x] Foundation: auth flow, session, i18n, onboarding, error boundary
- [x] Centers: search/filter list, center detail
- [x] Bookings: list, new (multi-step form), detail + cancel, confirmation, success
- [x] Favorites, Notifications, Profile view/edit/logout
- [x] My reviews list + write review
- [x] Complaints list + new + detail
- [x] Chat: conversations list + thread (WebSocket/STOMP)
- [x] Help screen (FAQ)
- [ ] Push notifications (FCM)
- [ ] Privacy, Terms, Notification prefs stub screens
- [ ] Production hardening (HTTPS/WSS, EAS project ID, Sentry)

### Phase 3.5 — Trust MVP
- [ ] FCM push notification service (`FCMService.java`)
- [ ] `fcm_token` column on `_user` + `PUT /users/me/fcm-token` endpoint
- [ ] Verified reviews (`is_verified` field + `booking_id` FK on `review`)
- [ ] Center service pricing table + CRUD endpoints
- [ ] Booking status info DTO

### Phase 4.0 — Deep Trust
- [ ] `WorkStage` enum + booking columns (`work_stage`, `estimated_completion`)
- [ ] `BookingWorkProgress` entity + endpoints
- [ ] `BookingMedia` entity + endpoints
- [ ] `BookingQuotes` entity + endpoints
- [ ] Trust score calculation job
- [ ] Center badges system (`center_badges` table + `BadgeType` enum)

### Phase 4.5 — Retention
- [ ] Loyalty system (`loyalty_accounts`, `loyalty_transactions`, `loyalty_rewards`, `user_rewards`)
- [ ] Vehicle management (`user_vehicles`)
- [ ] Maintenance reminders + scheduled notification job
- [ ] Referral system (`referral_codes`, `referrals`)

### Phase 5.0 — Growth
- [ ] AI price estimator
- [ ] Emergency roadside
- [ ] Subscription plans
- [ ] KNET payment integration (Kuwait)
- [ ] Analytics dashboard for center owners
- [ ] Offline support (mobile)

---

## 🧠 Claude Code Preferences

### ⚠️ MUST follow — non-negotiable rules

**1. Always ask before executing any command.**
Never run `mvn`, `npm`, `docker`, `git`, or any shell command without stating
what you are about to run and waiting for explicit confirmation ("yes" / "go ahead").

**2. Always ask before modifying any existing file.**
For every existing file that would change, show:
- The full file path
- A clear summary of what will change and why
Then wait for confirmation before writing.

**3. Always run tests after making changes — and report results.**
```bash
cd service-center && ./mvnw test
```
If tests fail, fix them before considering the task complete. Do not skip this.

**4. Always write production-ready code — no pseudocode or placeholders.**
No `// TODO`, stub returns, or `throw new UnsupportedOperationException()`
unless explicitly requested.

### General Workflow

- **New feature pattern:** Repository → Service → DTOs → Controller
- **Bilingual fields:** Always add both `Ar` and `En` variants
- **Ambiguity:** Ask one focused clarifying question — do not assume
- **Secrets:** Use `application.yml` properties — never hardcode
- **Lombok:** `@RequiredArgsConstructor` for injection, `@Slf4j` for logging

### Repo Awareness

3 separate repos — always confirm which repo before acting:
- `service-center` — Spring Boot backend (✅ complete)
- `maintenance-center-app` — React Native center owner app (✅ complete)
- `maintenance-customer-app` — React Native customer app (🔄 ~90% complete)

---

## ⚙️ Configuration (Phase 3.5+)

### Firebase (add to `application.yml`)
```yaml
firebase:
  credentials-file: classpath:firebase-service-account.json
  # Or use environment variable:
  # credentials-json: ${FIREBASE_CREDENTIALS_JSON}
```

### pom.xml addition for Firebase
```xml
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.2.0</version>
</dependency>
```

### Loyalty Constants (add to `application.yml`)
```yaml
loyalty:
  points-per-kd: 10
  review-points: 50
  review-with-photo-points: 100
  referral-points: 200
  first-booking-bonus: 100
  expiry-months: 12
  tier-thresholds:
    silver: 1000
    gold: 5000
    platinum: 15000
```

### Referral Constants (add to `application.yml`)
```yaml
referral:
  referrer-bonus-points: 200
  referred-bonus-points: 100
  referred-discount-percent: 20
  referred-discount-max-kd: 10
  valid-days: 30
```

---

## 📁 Files to Create (Phase 3.5)

```
src/main/java/com/maintainance/service_center/
├── notification/
│   ├── FCMService.java              # Firebase Cloud Messaging
│   └── PushNotificationRequest.java
├── pricing/
│   ├── CenterServicePricing.java    # Entity
│   ├── CenterServicePricingRepository.java
│   ├── CenterServicePricingService.java
│   ├── CenterServicePricingController.java
│   ├── CenterServicePricingRequest.java
│   └── CenterServicePricingResponse.java
└── resources/
    └── firebase-service-account.json  # Firebase credentials (gitignored)
```
