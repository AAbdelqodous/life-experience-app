# Maintenance Center Platform — Claude Code Context

## 🎯 Project Overview

A service marketplace connecting users with verified maintenance centers across Kuwait
and the broader Middle East. Users can discover, review, and book repair services for
cars, electronics, and home appliances. Expansion into restaurants, hotels, and other
service verticals is planned.

**Target Market:** Kuwait (primary), GCC / Middle East (expansion)
**Languages:** Arabic (primary), English
**Status:** Backend domain model and auth largely complete. Services, controllers,
and DTOs for most domains still need to be built. Mobile apps not yet started.

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
├── address/        # Address (embeddable)
├── auth/           # AuthController, AuthService, RegistrationRequest, AuthRequest/Response
├── booking/        # Booking entity + enums (BookingStatus, ServiceType, PaymentMethod, etc.)
├── category/       # ServiceCategory entity
├── center/         # MaintenanceCenter entity
├── chat/           # Conversation, Message entities + enums
├── complaint/      # Complaint entity + enums
├── config/         # BeansConfig (AuthProvider, PasswordEncoder, AuthManager)
├── email/          # EmailService, EmailTemplateName
├── favorite/       # UserFavorite entity
├── handler/        # GlobalExceptionHandling, ExceptionResponse, BusinessErrorCodes
├── notification/   # Notification entity + enums
├── review/         # Review entity, ReviewRepository
├── role/           # Role entity, RoleRepository
├── search/         # SearchHistory entity, SearchSource enum
├── security/       # JwtService, JwtFilter, SecurityConfig, UserDetailsServiceImpl
└── user/           # User, Token, TokenRepository, UserRepository, UserType, Language
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

### Mobile (planned — not started)
| Layer | Technology |
|-------|-----------|
| Framework | React Native 0.73+ |
| Language | TypeScript |
| Navigation | React Navigation 6 |
| State | Redux Toolkit + RTK Query |
| Maps | react-native-maps (Google Maps SDK) |
| Real-time | Socket.io-client |
| Push Notifs | Firebase Cloud Messaging |
| i18n | react-i18next |

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

### Entities Defined But Not Yet in DB
All other JPA entities exist as classes. Hibernate `ddl-auto: update` will create
their tables on next startup: `booking`, `service_categories`, `maintenance_centers`,
`conversations`, `messages`, `complaint`, `notifications`, `review`,
`user_favorite`, `search_history`

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

## 🐛 Known Bugs — Fix These Before Building Further

These are real bugs found in the existing code. Do not work around them — fix them properly.

**1. `JwtService.axtractAllClaims()`** — calls `.parseClaimsJwt(token)` (for unsigned
tokens). Must be `.parseClaimsJws(token)` (for signed tokens). Breaks all JWT validation.

**2. `JwtFilter.userDetailsService` not injected** — field is declared but missing from
`@RequiredArgsConstructor` injection (it's not `final`). Causes `NullPointerException`
on every authenticated request. Add `final` keyword or inject via constructor.

**3. `MaintenanceCenter` `@JoinColumn` missing `name =`** — `@JoinColumn("center_id")`
is invalid syntax, must be `@JoinColumn(name = "center_id")`. Will fail at startup.

**4. `Complaint` entity uses `@Column` instead of `@JoinColumn`** — on `complainant`
and `center` `@ManyToOne` fields. Will cause Hibernate mapping errors.

**5. `UserFavorite` and `SearchHistory` same issue** — `@Column` on `@ManyToOne` fields
instead of `@JoinColumn`.

**6. `Message` has wrong `@Table(name = "conversations")`** — must be `"messages"`.
Will conflict with the `Conversation` entity table.

**7. `ReviewRepository.calculateAverageRating()` JPQL has space** — `: centerId`
must be `:centerId` (no space). Will throw `QueryException` at startup.

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

---

## 🌍 Localization & Regional Context

- Bilingual entity fields: always `nameAr` + `nameEn`, `descriptionAr` + `descriptionEn`
- Currency: Kuwaiti Dinar (KD)
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

### Existing Enums (do not redefine)
`BookingStatus`, `ServiceType`, `PaymentMethod`, `PaymentStatus`, `CancelledBy`,
`MessageType`, `SenderType`, `ComplaintType`, `ComplaintStatus`, `ComplaintPriority`,
`NotificationType`, `NotificationPriority`, `UserType`, `Language`, `SearchSource`

---

## 🚀 Development Phases

### Phase 1 — Backend (current focus)
- [x] Project structure, Docker Compose, Maven setup
- [x] User, Role, Token entities + DB tables
- [x] Auth: register, OTP email, activate, login (JWT)
- [x] Email service (MailDev in dev)
- [x] Global exception handling + BusinessErrorCodes
- [x] Swagger / OpenAPI
- [x] All domain entities defined (Booking, Center, Category, Review, Chat, etc.)
- [ ] **Fix 7 known bugs above — do this first**
- [ ] MaintenanceCenter: Repository, Service, DTOs, Controller
- [ ] ServiceCategory: Repository, Service, DTOs, Controller
- [ ] Booking: Repository, Service, DTOs, Controller
- [ ] Review: Service, DTOs, Controller (Repository already exists)
- [ ] User profile: Service, DTOs, Controller
- [ ] Search & filter endpoint

### Phase 2 — Enhanced Backend
- [ ] WebSocket / STOMP real-time chat
- [ ] Firebase push notifications
- [ ] Image upload
- [ ] Complaint management

### Phase 3 — Mobile Apps
- [ ] React Native customer app (30 screens)
- [ ] React Native center owner app (20 screens)

### Phase 4 — Advanced
- [ ] KNET payment integration (Kuwait)
- [ ] Analytics dashboard for center owners
- [ ] Offline support

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
- `service-center` — Spring Boot backend (currently active)
- `maintenance-customer-app` — React Native customer app (not started)
- `maintenance-center-app` — React Native center owner app (not started)
