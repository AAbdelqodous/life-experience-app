package com.maintainance.service_center.quoterequest;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import com.maintainance.service_center.chat.ChatService;
import com.maintainance.service_center.chat.Conversation;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.notification.NotificationService;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterPermission;
import com.maintainance.service_center.staff.CenterSecurityService;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Spec 009 (customer) + 024 (center) — the shared quote-request marketplace.
 * Customer broadcasts, compares, and accepts; centers see a sealed inbox and respond.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteRequestService {

    /** Request window before auto-expiry (spec default 48h). */
    private static final long REQUEST_WINDOW_HOURS = 48;

    private final QuoteRequestRepository requestRepository;
    private final QuoteResponseRepository responseRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final MaintenanceCenterRepository centerRepository;
    private final CenterMembershipRepository membershipRepository;
    private final CenterSecurityService centerSecurity;
    private final BookingRepository bookingRepository;
    private final ChatService chatService;
    private final NotificationService notificationService;

    // ── Customer operations ──────────────────────────────────────────────────

    @Transactional
    public QuoteRequestResponse createRequest(User customer, CreateQuoteRequestRequest req) {
        ServiceCategory category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + req.categoryId()));

        QuoteRequest entity = new QuoteRequest();
        entity.setCustomer(customer);
        entity.setCategory(category);
        entity.setServiceId(req.serviceId());
        entity.setDescription(req.description());
        entity.setAttachmentUrls(req.attachmentUrls() != null ? req.attachmentUrls() : new ArrayList<>());
        entity.setVehicleOrApplianceNote(req.vehicleOrApplianceNote());
        entity.setAreaGovernorate(req.areaGovernorate());
        entity.setFulfillmentHint(req.fulfillmentHint());
        entity.setStatus(QuoteRequestStatus.OPEN);
        entity.setExpiresAt(LocalDateTime.now().plusHours(REQUEST_WINDOW_HOURS));
        var matched = centerRepository.findByCategoryId(category.getId(), PageRequest.of(0, 100));
        entity.setReachCount((int) matched.getTotalElements());

        QuoteRequest saved = requestRepository.save(entity);
        log.info("Quote request {} created by customer {} → reached {} centers",
                saved.getId(), customer.getId(), saved.getReachCount());

        // Notify each matched center's owner of the new lead (spec 024).
        matched.getContent().forEach(c ->
                notificationService.notifyNewQuoteRequest(
                        c.getOwner(), saved.getId(), category.getNameEn(), category.getNameAr()));

        return toCustomerView(saved, List.of());
    }

    @Transactional
    public List<QuoteRequestSummaryResponse> listMyRequests(User customer) {
        return requestRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(this::expireIfNeeded)
                .map(r -> new QuoteRequestSummaryResponse(
                        r.getId(),
                        r.getCategory().getNameAr(),
                        r.getCategory().getNameEn(),
                        r.getStatus(),
                        r.getReachCount(),
                        countActiveResponses(r.getId()),
                        r.getExpiresAt(),
                        r.getCreatedAt()))
                .toList();
    }

    /**
     * Single request view, branched by caller: the owning customer gets the full
     * (all-responses) view; anyone else is treated as a center and gets the sealed view.
     */
    @Transactional
    public Object getForCaller(User caller, Long requestId) {
        QuoteRequest request = load(requestId);
        expireIfNeeded(request);
        if (request.getCustomer().getId().equals(caller.getId())) {
            return toCustomerView(request, responseRepository.findByRequestId(requestId));
        }
        return toCenterView(caller, request);
    }

    @Transactional
    public AcceptResultResponse acceptQuote(User customer, Long requestId, Long quoteId) {
        QuoteRequest request = load(requestId);
        requireOwner(request, customer);
        expireIfNeeded(request);
        if (request.getStatus() != QuoteRequestStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is no longer open");
        }
        QuoteResponse chosen = responseRepository.findById(quoteId)
                .filter(r -> r.getRequest().getId().equals(requestId))
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + quoteId));
        if (chosen.getStatus() == QuoteResponseStatus.WITHDRAWN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quote has been withdrawn");
        }

        Booking booking = createBookingFromQuote(request, chosen);

        List<QuoteResponse> all = responseRepository.findByRequestId(requestId);
        for (QuoteResponse r : all) {
            r.setStatus(r.getId().equals(quoteId)
                    ? QuoteResponseStatus.SELECTED : QuoteResponseStatus.NOT_SELECTED);
        }
        responseRepository.saveAll(all);

        request.setStatus(QuoteRequestStatus.ACCEPTED);
        request.setAcceptedBookingId(booking.getId());
        requestRepository.save(request);

        // Notify the winning center, and the others that they were not selected (spec 024 US2).
        notificationService.notifyQuoteWon(chosen.getCenter().getOwner(), booking.getId());
        for (QuoteResponse r : all) {
            if (!r.getId().equals(quoteId)) {
                notificationService.notifyQuoteNotSelected(r.getCenter().getOwner(), requestId);
            }
        }

        log.info("Quote request {} accepted (quote {}) → booking {} at center {}",
                requestId, quoteId, booking.getId(), chosen.getCenter().getId());
        return new AcceptResultResponse(requestId, QuoteRequestStatus.ACCEPTED, booking.getId());
    }

    @Transactional
    public CancelResultResponse cancelRequest(User customer, Long requestId) {
        QuoteRequest request = load(requestId);
        requireOwner(request, customer);
        if (request.getStatus() != QuoteRequestStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only open requests can be cancelled");
        }
        request.setStatus(QuoteRequestStatus.CANCELLED);
        requestRepository.save(request);
        return new CancelResultResponse(requestId, QuoteRequestStatus.CANCELLED);
    }

    // ── Center operations ────────────────────────────────────────────────────

    @Transactional
    public List<InboxItemResponse> getInbox(User caller) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.RESPOND_TO_QUOTES);

        List<Long> categoryIds = center.getCategories().stream().map(ServiceCategory::getId).toList();
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return requestRepository.findOpenForCategories(categoryIds).stream()
                .map(r -> {
                    QuoteResponse mine = responseRepository
                            .findByRequestIdAndCenterId(r.getId(), center.getId()).orElse(null);
                    return new InboxItemResponse(
                            r.getId(),
                            r.getCategory().getNameAr(),
                            r.getCategory().getNameEn(),
                            preview(r.getDescription()),
                            r.getAreaGovernorate(),
                            null,
                            new ArrayList<>(r.getAttachmentUrls()),
                            r.getCreatedAt(),
                            r.getExpiresAt(),
                            r.getStatus(),
                            mine != null ? mine.getStatus().name() : "NONE");
                })
                .toList();
    }

    @Transactional
    public CenterQuoteResponseDto submitQuote(User caller, Long requestId, SubmitQuoteRequestDto dto) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.RESPOND_TO_QUOTES);

        QuoteRequest request = load(requestId);
        expireIfNeeded(request);
        if (request.getStatus() != QuoteRequestStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is no longer open");
        }
        if (dto.priceMax().compareTo(dto.priceMin()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "priceMax must be >= priceMin");
        }

        QuoteResponse response = responseRepository
                .findByRequestIdAndCenterId(requestId, center.getId())
                .orElseGet(() -> {
                    QuoteResponse fresh = new QuoteResponse();
                    fresh.setRequest(request);
                    fresh.setCenter(center);
                    fresh.setSubmittedAt(LocalDateTime.now());
                    return fresh;
                });
        boolean isUpdate = response.getId() != null;
        response.setPriceMin(dto.priceMin());
        response.setPriceMax(dto.priceMax());
        response.setEstimatedDurationMinutes(dto.estimatedDurationMinutes());
        response.setInclusions(dto.inclusions());
        response.setMessage(dto.message());
        response.setStatus(isUpdate ? QuoteResponseStatus.UPDATED : QuoteResponseStatus.SUBMITTED);
        if (isUpdate) {
            response.setUpdatedAt(LocalDateTime.now());
        }
        QuoteResponse saved = responseRepository.save(response);

        // Notify the customer that a new quote arrived (only on first submit, not every edit).
        if (!isUpdate) {
            notificationService.notifyQuoteReceived(request.getCustomer(), requestId);
        }
        return toCenterResponseDto(saved);
    }

    @Transactional
    public WithdrawResultResponse withdrawQuote(User caller, Long requestId) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.RESPOND_TO_QUOTES);

        QuoteResponse response = responseRepository
                .findByRequestIdAndCenterId(requestId, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("No quote to withdraw"));
        if (response.getStatus() == QuoteResponseStatus.SELECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot withdraw a selected quote");
        }
        response.setStatus(QuoteResponseStatus.WITHDRAWN);
        responseRepository.save(response);
        return new WithdrawResultResponse(response.getId(), QuoteResponseStatus.WITHDRAWN);
    }

    // ── Shared: request-scoped chat ──────────────────────────────────────────

    /**
     * Open (or reuse) the conversation between the request's customer and a center.
     * The owning customer supplies the responding {@code centerId}; a center caller omits it
     * (their own center is resolved, and chat targets the request's customer). Both sides
     * converge on the same (center, customer) conversation.
     */
    @Transactional
    public StartChatResponse startChat(User caller, Long requestId, Long centerIdFromBody) {
        QuoteRequest request = load(requestId);
        Conversation conversation;
        if (request.getCustomer().getId().equals(caller.getId())) {
            if (centerIdFromBody == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "centerId is required");
            }
            conversation = chatService.getOrCreateConversation(centerIdFromBody, caller);
        } else {
            MaintenanceCenter center = resolveMyCenter(caller);
            centerSecurity.requirePermission(center.getId(), caller, CenterPermission.RESPOND_TO_QUOTES);
            conversation = chatService.getOrCreateConversation(center.getId(), request.getCustomer());
        }
        return new StartChatResponse(conversation.getId());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private QuoteRequest load(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quote request not found: " + id));
    }

    private void requireOwner(QuoteRequest request, User customer) {
        if (!request.getCustomer().getId().equals(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your request");
        }
    }

    private QuoteRequest expireIfNeeded(QuoteRequest r) {
        if (r.getStatus() == QuoteRequestStatus.OPEN
                && r.getExpiresAt() != null
                && r.getExpiresAt().isBefore(LocalDateTime.now())) {
            r.setStatus(QuoteRequestStatus.EXPIRED);
            requestRepository.save(r);
        }
        return r;
    }

    private MaintenanceCenter resolveMyCenter(User caller) {
        return centerRepository.findFirstByOwnerId(caller.getId())
                .or(() -> membershipRepository
                        .findByUserIdAndStatus(caller.getId(), MembershipStatus.ACTIVE)
                        .stream().findFirst().map(CenterMembership::getCenter))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No center associated with this account"));
    }

    private int countActiveResponses(Long requestId) {
        return (int) responseRepository.findByRequestId(requestId).stream()
                .filter(r -> r.getStatus() == QuoteResponseStatus.SUBMITTED
                        || r.getStatus() == QuoteResponseStatus.UPDATED)
                .count();
    }

    private Booking createBookingFromQuote(QuoteRequest request, QuoteResponse chosen) {
        Booking booking = new Booking();
        booking.setBookingNumber("QR" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setCustomer(request.getCustomer());
        booking.setCenter(chosen.getCenter());
        booking.setCategory(request.getCategory());
        booking.setServiceDescription(request.getDescription());
        // Placeholder schedule — the center/customer schedule the exact slot after acceptance.
        booking.setBookingDate(LocalDate.now());
        booking.setBookingTime(LocalTime.of(9, 0));
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setOriginRequestId(request.getId());
        return bookingRepository.save(booking);
    }

    private static String preview(String text) {
        if (text == null) return "";
        return text.length() > 80 ? text.substring(0, 80) + "…" : text;
    }

    private QuoteRequestResponse toCustomerView(QuoteRequest r, List<QuoteResponse> responses) {
        List<CustomerQuoteResponseDto> dtos = responses.stream().map(resp -> {
            MaintenanceCenter c = resp.getCenter();
            return new CustomerQuoteResponseDto(
                    resp.getId(),
                    c.getId(),
                    c.getNameAr(),
                    c.getNameEn(),
                    c.getLogoUrl(),
                    c.getAverageRating(),
                    null,
                    null,
                    resp.getPriceMin(),
                    resp.getPriceMax(),
                    resp.getEstimatedDurationMinutes(),
                    resp.getInclusions(),
                    resp.getMessage(),
                    resp.getStatus(),
                    resp.getSubmittedAt() != null ? resp.getSubmittedAt() : resp.getCreatedAt());
        }).toList();

        return new QuoteRequestResponse(
                r.getId(),
                r.getCategory().getId(),
                r.getCategory().getNameAr(),
                r.getCategory().getNameEn(),
                r.getServiceId(),
                r.getDescription(),
                new ArrayList<>(r.getAttachmentUrls()),
                r.getAreaGovernorate(),
                r.getFulfillmentHint(),
                r.getStatus(),
                r.getReachCount(),
                r.getExpiresAt(),
                r.getAcceptedBookingId(),
                r.getCreatedAt(),
                dtos);
    }

    private QuoteRequestDetailResponse toCenterView(User caller, QuoteRequest r) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.RESPOND_TO_QUOTES);
        QuoteResponse mine = responseRepository
                .findByRequestIdAndCenterId(r.getId(), center.getId()).orElse(null);
        return new QuoteRequestDetailResponse(
                r.getId(),
                r.getCategory().getId(),
                r.getCategory().getNameAr(),
                r.getCategory().getNameEn(),
                r.getServiceId(),
                r.getDescription(),
                new ArrayList<>(r.getAttachmentUrls()),
                r.getVehicleOrApplianceNote(),
                r.getAreaGovernorate(),
                null,
                r.getFulfillmentHint(),
                r.getStatus(),
                r.getExpiresAt(),
                mine != null ? toCenterResponseDto(mine) : null);
    }

    private static CenterQuoteResponseDto toCenterResponseDto(QuoteResponse r) {
        return new CenterQuoteResponseDto(
                r.getId(),
                r.getPriceMin(),
                r.getPriceMax(),
                r.getEstimatedDurationMinutes(),
                r.getInclusions(),
                r.getMessage(),
                r.getStatus(),
                r.getSubmittedAt(),
                r.getUpdatedAt());
    }
}
