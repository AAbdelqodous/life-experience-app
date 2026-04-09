package com.maintainance.service_center.admin;

import com.maintainance.service_center.user.ApprovalStatus;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserRepository;
import com.maintainance.service_center.user.UserResponse;
import com.maintainance.service_center.user.UserService;
import com.maintainance.service_center.user.UserType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final UserService userService;

    public Page<UserResponse> getPendingOwners(Pageable pageable) {
        return userRepository
                .findByUserTypeAndApprovalStatus(UserType.CENTER_OWNER, ApprovalStatus.PENDING_APPROVAL, pageable)
                .map(userService::toResponse);
    }

    @Transactional
    public UserResponse approveOwner(Integer userId) {
        User user = findOwner(userId);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        userRepository.save(user);
        log.info("Admin approved center owner ID: {}", userId);
        return userService.toResponse(user);
    }

    @Transactional
    public UserResponse rejectOwner(Integer userId) {
        User user = findOwner(userId);
        user.setApprovalStatus(ApprovalStatus.REJECTED);
        userRepository.save(user);
        log.info("Admin rejected center owner ID: {}", userId);
        return userService.toResponse(user);
    }

    private User findOwner(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        if (user.getUserType() != UserType.CENTER_OWNER) {
            throw new IllegalArgumentException("User " + userId + " is not a center owner");
        }
        return user;
    }
}
