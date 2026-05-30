package com.maintainance.service_center.payment;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterPermission;
import com.maintainance.service_center.staff.CenterSecurityService;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Spec 023 — center bank payout account + payout requests, drawn from Available balance. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CenterPayoutService {

    private static final BigDecimal MINIMUM_PAYOUT = new BigDecimal("5.000");
    private static final Set<PaymentStatus> SETTLED =
            EnumSet.of(PaymentStatus.RELEASED, PaymentStatus.PAID);

    private final PayoutAccountRepository accountRepository;
    private final PayoutRepository payoutRepository;
    private final PaymentRepository paymentRepository;
    private final MaintenanceCenterRepository centerRepository;
    private final CenterMembershipRepository membershipRepository;
    private final CenterSecurityService centerSecurity;

    @Transactional(readOnly = true)
    public PayoutAccountResponse getAccount(User caller) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_PAYOUTS);
        PayoutAccount a = accountRepository.findByCenterId(center.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No payout account"));
        return toAccountResponse(a);
    }

    @Transactional
    public PayoutAccountResponse upsertAccount(User caller, UpsertPayoutAccountRequest req) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_PAYOUTS);
        if (!req.iban().toUpperCase().startsWith("KW")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A Kuwaiti IBAN (KW…) is required");
        }
        PayoutAccount a = accountRepository.findByCenterId(center.getId())
                .orElseGet(() -> {
                    PayoutAccount na = new PayoutAccount();
                    na.setCenter(center);
                    return na;
                });
        a.setIban(req.iban());
        a.setHolderName(req.holderName());
        // Stub: auto-verify. A real integration verifies asynchronously (status starts UNVERIFIED).
        a.setStatus(PayoutAccountStatus.VERIFIED);
        return toAccountResponse(accountRepository.save(a));
    }

    @Transactional(readOnly = true)
    public List<PayoutResponse> listPayouts(User caller) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_PAYOUTS);
        return payoutRepository.findByCenterIdOrderByRequestedAtDesc(center.getId()).stream()
                .map(this::toPayoutResponse).toList();
    }

    @Transactional
    public PayoutResponse requestPayout(User caller, RequestPayoutRequest req) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_PAYOUTS);

        PayoutAccount account = accountRepository.findByCenterId(center.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Register a payout account first"));
        if (account.getStatus() != PayoutAccountStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payout account is not verified");
        }
        if (req.amount().compareTo(MINIMUM_PAYOUT) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Below the minimum payout of " + MINIMUM_PAYOUT + " KD");
        }
        BigDecimal available = computeAvailable(center.getId());
        if (req.amount().compareTo(available) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount exceeds the available balance");
        }

        Payout payout = new Payout();
        payout.setCenter(center);
        payout.setAccount(account);
        payout.setAmount(req.amount());
        payout.setStatus(PayoutStatus.REQUESTED);
        payout.setRequestedBy(caller);
        Payout saved = payoutRepository.save(payout);
        saved.setReference("PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payoutRepository.save(saved);
        log.info("Payout {} requested by user {} for center {} amount {}",
                saved.getId(), caller.getId(), center.getId(), req.amount());
        return toPayoutResponse(saved);
    }

    /** Available = settled net (minus refunds) − payouts already requested/processing/paid. */
    private BigDecimal computeAvailable(Long centerId) {
        BigDecimal settledNet = paymentRepository.findByCenterId(centerId).stream()
                .filter(p -> SETTLED.contains(p.getStatus()))
                .map(p -> nz(p.getNetAmount()).subtract(nz(p.getRefundedAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payoutsTaken = payoutRepository.findByCenterIdOrderByRequestedAtDesc(centerId).stream()
                .filter(po -> po.getStatus() != PayoutStatus.FAILED)
                .map(Payout::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return settledNet.subtract(payoutsTaken);
    }

    private MaintenanceCenter resolveMyCenter(User caller) {
        return centerRepository.findFirstByOwnerId(caller.getId())
                .or(() -> membershipRepository
                        .findByUserIdAndStatus(caller.getId(), MembershipStatus.ACTIVE)
                        .stream().findFirst().map(CenterMembership::getCenter))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No center associated with this account"));
    }

    private PayoutAccountResponse toAccountResponse(PayoutAccount a) {
        return new PayoutAccountResponse(a.getId(), a.getIban(), a.getHolderName(), a.getBankName(), a.getStatus().name());
    }

    private PayoutResponse toPayoutResponse(Payout p) {
        return new PayoutResponse(p.getId(), p.getAmount(), p.getAccount().getId(), p.getStatus().name(),
                p.getReference(), p.getRequestedAt(), p.getCompletedAt(), p.getFailureReason());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
