package com.maintainance.service_center.staff;

import com.maintainance.service_center.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-shot backfill: copies firstname/lastname/email from the live User row
 * onto memberships whose cache columns were never populated. After the
 * cache exists, historical attribution (FR-011) survives User deletion.
 *
 * Runs idempotently on every boot and is cheap when there is nothing to do.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class MembershipNameBackfillRunner implements ApplicationRunner {

    private final CenterMembershipRepository membershipRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<CenterMembership> stale = membershipRepository.findAll().stream()
                .filter(m -> m.getUserFirstname() == null
                          || m.getUserLastname() == null
                          || m.getUserEmail() == null)
                .toList();
        if (stale.isEmpty()) {
            return;
        }
        int filled = 0;
        for (CenterMembership m : stale) {
            User u = m.getUser();
            if (u == null) {
                continue;
            }
            if (m.getUserFirstname() == null) m.setUserFirstname(u.getFirstname());
            if (m.getUserLastname() == null) m.setUserLastname(u.getLastname());
            if (m.getUserEmail() == null) m.setUserEmail(u.getEmail());
            filled++;
        }
        membershipRepository.saveAll(stale);
        log.info("Membership name backfill: cached firstname/lastname/email on {} rows", filled);
    }
}
