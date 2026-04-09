# Service Center Backend — Claude Code Context

## 🎯 Project Overview

Spring Boot REST API powering a service marketplace connecting customers with verified
maintenance centers across Kuwait and the Middle East. Centers handle car, electronics,
and home appliance repairs. Two mobile clients consume this API: a customer app and a
center-owner app.

**Target Market:** Kuwait (primary), GCC / Middle East (expansion)
**Languages:** Arabic (primary), English (bilingual fields on all entities)
**Status:** ✅ Backend fully implemented and tested. All domains complete.

---

## 🏗️ Architecture

### Strategy
- **Phase 1 (current):** Modular Spring Boot monolith — fast to build, easy to debug
- **Phase 2:** Extract to microservices as traffic and team grow
- Domain boundaries are clean now to make extraction straightforward

### Repository Layout (3 separate repos)
```
life-experience-app/service-center/   # This repo — Spring Boot API
maintenance-center-app/               # React Native center-owner app (~20 screens)
maintenance-customer-app/             # React Native customer app (~30 screens)
```

Maven artifact: `service-center`, group: `com.maintainance`
Root IntelliJ project: `life-experience-app`

### Package Structure
```
src/main/java/com/maintainance/service_center/
├── address/        # Address (embeddable) + AddressRequest DTO
├── auth/           # AuthController, AuthService, RegistrationRequest, AuthRequest/Response
├── booking/        # Booking entity + enums + BookingController, BookingService, BookingResponse
├── category/       # ServiceCategory entity + Repository + Controller + Response
├── center/         # MaintenanceCenter entity + full CRUD service + DTOs + Controller
├── chat/           # Conversation, Message entities + ChatController, ChatService
├── complaint/      # Complaint entity + enums + Controller + Service
├── config/         # BeansConfig, FileStorageService
├── email/          # EmailService, EmailTemplateName
├── favorite/       # UserFavorite entity + FavoriteController + FavoriteService
├── handler/        # GlobalExceptionHandling, ExceptionResponse, BusinessErrorCodes
├── notification/   # Notification entity + enums + NotificationController, NotificationService
├── review/         # Review entity + ReviewController, ReviewService, ReviewResponse
├── role/           # Role entity, RoleRepository
├── search/         # SearchHistory entity, SearchSource enum + SearchController
├── security/       # JwtService, JwtFilter, SecurityConfig, UserDetailsServiceImpl
├── admin/          # AdminController, AdminService — center owner approval endpoints
└── user/           # User, Token, TokenRepository, UserRepository, UserType, Language, ApprovalStatus
```

---

## 🛠️ Tech Stack

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
| Lombok | 1.18.40 |
| Build | Maven wrapper (mvnw) |
| Containers | Docker Compose — postgres + maildev |

---

## 🗄️ Database

### Docker Compose Services
- **postgres** — container `le-postgres`, image `postgres:15`, port `5432`
  - DB name: **`experience`**
  - Username: `abdelqodous`, Password: `P@ssw0rd` (dev only — use env vars in prod)
- **mail-dev** — container `le-mail-dev`, image `maildev/maildev`
  - Web UI: port `1080`, SMTP: port `1025`

### Key JPA Patterns
- **No shared `BaseEntity`** — each entity has its own `@CreatedDate` / `@LastModifiedDate`
  with `@EntityListeners(AuditingEntityListener.class)` directly
- `@EnableJpaAuditing` on `ServiceCenterApplication`
- `Address` is `@Embeddable`, embedded in `User` and `MaintenanceCenter`
- Bilingual pattern: `nameAr`/`nameEn`, `descriptionAr`/`descriptionEn` — always both
- Address uses bilingual fields: `cityAr`/`cityEn`, `districtAr`/`districtEn`,
  `streetAr`/`streetEn`, `governorateAr`/`governorateEn`

### Tables
| Table | Notes |
|-------|-------|
| `_user` | Underscore prefix avoids reserved word conflict |
| `_user_roles` | Join table: users ↔ roles |
| `role` | Roles |
| `token` | Email verification OTP tokens |
| `maintenance_centers` | Center profiles |
| `service_categories` | Seeded with 6 rows |
| `center_categories` | centers ↔ service_categories join |
| `booking` | Customer bookings |
| `review` | Center reviews |
| `conversations`, `messages` | Chat threads |
| `complaint` | Customer complaints |
| `notification` | System notifications |
| `user_favorite` | Saved centers |
| `search_history` | Customer search logs |

---

## 🔐 Authentication

```
POST  /api/v1/auth/register           → 202 Accepted (sends OTP email)
                                         Body: { firstname, lastname, email, password, userType? }
                                         userType: "CUSTOMER" (default) | "CENTER_OWNER"
                                         CENTER_OWNER registrations start with approvalStatus=PENDING_APPROVAL

POST  /api/v1/auth/authenticate       → 200 { "token": "<jwt>", "approvalStatus": "APPROVED|PENDING_APPROVAL" }
                                         REJECTED users get 500 (IllegalStateException)

GET   /api/v1/auth/activate-account   → ?token=XXXXXX
```

### JWT Config (application-dev.yml)
- Secret key: `application.security.jwt.secret-key` (env var in prod: `JWT_SECRET_KEY`)
- Expiry: `8640000` ms = 2.4 hours
- Claims: `fullName`, authorities list

---

## 📡 API Design

- Context path: `/api/v1/`
- Swagger UI: `http://localhost:8080/api/v1/swagger-ui/index.html`
- Authenticated endpoints: `Authorization: Bearer <jwt>`
- `LocalTime` serializes as `"HH:mm:ss"` (ISO, not timestamp)
- Pagination: `{ content, totalElements, totalPages, number, size }`

### Endpoints Summary

**Auth** — `/auth/**`
**Centers** — `GET /centers`, `GET /centers/{id}`, `GET /centers/my/profile`, `PUT /centers/my`, `POST /centers/my/images`
**Categories** — `GET /categories`
**Bookings** — `GET /bookings`, `GET /bookings/{id}`, `POST /bookings`, `PUT /bookings/{id}/status`, `PUT /bookings/{id}/cancel`, `GET /bookings/stats`
**Reviews** — `GET /reviews/center`, `GET /reviews/center/{centerId}`, `POST /reviews`, `POST /reviews/{id}/reply`
**Chat** — `GET /conversations/center`, `GET /conversations/my`, `GET /conversations/{id}/messages`, `POST /conversations/{id}/messages`, `POST /conversations/start`
**Notifications** — `GET /notifications`, `PUT /notifications/{id}/read`, `PUT /notifications/read-all`
**Favorites** — `GET /favorites`, `POST /favorites/{centerId}`, `DELETE /favorites/{centerId}`
**Search** — `GET /search`, `POST /search/history`, `GET /search/history`
**Users** — `GET /users/me`, `PUT /users/me`, `PUT /users/me/push-token`
**Complaints** — `GET /complaints`, `POST /complaints`, `GET /complaints/{id}`
**Admin** — `GET /admin/users/pending`, `PUT /admin/users/{id}/approve`, `PUT /admin/users/{id}/reject`
  All admin endpoints require `ROLE_ADMIN` (`@Secured("ROLE_ADMIN")`)

### Key Response Field Names (do not rename)
| Field | Notes |
|-------|-------|
| `bookingStatus` | NOT `status` |
| `bookingDate`, `bookingTime` | NOT `scheduledDate`/`scheduledTime` |
| `isRead` | NOT `read` |
| `notificationType` | NOT `type` |
| `totalReviews` | NOT `reviewCount` |
| `isActive` | NOT `isOpen` |
| `ownerReply` | maps from `Review.centerResponse` |

### Error Response
```json
{ "businessErrorCode": 304, "businessErrorDescription": "...", "error": "...", "validationErrors": [...] }
```

### BusinessErrorCodes
| Code | HTTP | Description |
|------|------|-------------|
| 300 | 400 | Incorrect current password |
| 301 | 400 | New password mismatch |
| 302 | 403 | Account locked |
| 303 | 403 | Account disabled |
| 304 | 403 | Bad credentials |

---

## ⚠️ Production Blockers (NOT production-ready yet)

These must be resolved before deploying:

1. **Secrets in config** — `application-dev.yml` contains DB password, JWT secret, mail
   credentials in plain text. Must be moved to env vars: `${DB_PASSWORD}`, `${JWT_SECRET_KEY}`, `${MAIL_PASSWORD}`
2. **No `application-prod.yml`** — Create with `ddl-auto: validate`, real SMTP, env-var references
3. **Wildcard CORS** — `SecurityConfig.java` uses `setAllowedOriginPatterns(List.of("*"))`.
   Must be restricted to actual frontend domains in production
4. **Hardcoded activation URL** — `http://localhost:4200/activate-account` must use env var
5. **No rate limiting** — Auth endpoints have no brute-force protection
6. **No HTTPS enforcement** — Add HSTS and security headers
7. **`ddl-auto: update`** — Change to `validate` in production to prevent accidental schema changes
8. **`printStackTrace()`** in `GlobalExceptionHandling.java` — Replace with SLF4J logger

---

## ⚙️ Development Environment

```bash
# Start Docker (postgres + maildev)
docker-compose up -d

# Run backend (dev profile)
cd ~/IdeaProjects/life-experience-app/service-center
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# API:        http://localhost:8080/api/v1/
# Swagger UI: http://localhost:8080/api/v1/swagger-ui/index.html
# MailDev:    http://localhost:1080

# Run tests
./mvnw test
```

---

## 📋 Coding Standards

### Java / Spring Boot
- **`@RequiredArgsConstructor`** for constructor injection — no `@Autowired` on fields
- **`@Slf4j`** for logging — never `System.out.println` or `printStackTrace()`
- DTOs for all request/response — never expose JPA entities in controllers
- `@Valid` on all `@RequestBody` parameters
- Service layer owns all business logic — controllers are thin
- Never call `Optional.get()` without checking — use `orElseThrow()`
- `@Transactional` on service methods that write
- Passwords always `BCryptPasswordEncoder` — never plain
- `Objects.equals()` for nullable comparisons — never `a.equals(b)` where `a` can be null

### Existing Enums (do not redefine)
`BookingStatus`, `ServiceType`, `PaymentMethod`, `PaymentStatus`, `CancelledBy`,
`MessageType`, `SenderType`, `ComplaintType`, `ComplaintStatus`, `ComplaintPriority`,
`NotificationType`, `NotificationPriority`, `UserType`, `ApprovalStatus`, `Language`, `SearchSource`

### New Feature Pattern
Repository → Service → DTOs → Controller

---

## 🚀 Development Phases

### Phase 1 — Core Backend ✅ Complete
- [x] Project setup, Docker Compose, Maven
- [x] User/Role/Token + auth (register, OTP, activate, login)
- [x] JWT security, global exception handling
- [x] All domain entities and services (Center, Booking, Review, Chat, Notification, Complaint, Favorite, Search)
- [x] File upload (center images/logo)
- [x] WebSocket / STOMP real-time chat
- [x] Notification service

### Phase 2 — Production Hardening ⏳ Pending
- [ ] Move all secrets to environment variables
- [ ] Create `application-prod.yml`
- [ ] Restrict CORS to known origins
- [ ] Add rate limiting on auth endpoints
- [ ] Add HTTPS / HSTS enforcement
- [x] Push token endpoint: `PUT /users/me/push-token` ✅ implemented
- [x] Center owner registration with admin approval gate (`ApprovalStatus`) ✅ implemented
- [ ] Add refresh token endpoint: `POST /auth/refresh`
- [ ] Replace `ddl-auto: update` with `validate` + Flyway migrations

### Phase 3 — Advanced
- [ ] KNET payment integration
- [ ] Analytics endpoints for center owner dashboard
- [ ] Multi-branch management
- [ ] Offline sync support

---

## 🧠 Claude Code Rules

**1. Always ask before running any command.**
State the command and wait for explicit confirmation before executing.

**2. Always ask before modifying any existing file.**
Show full file path + summary of changes. Wait for confirmation.

**3. Run tests after changes and report results.**
```bash
./mvnw test
```

**4. Production-ready code only — no pseudocode or placeholders.**

**5. Always confirm which repo before acting.**
- `service-center` — this repo (Spring Boot backend)
- `maintenance-center-app` — React Native center-owner app
- `maintenance-customer-app` — React Native customer app
