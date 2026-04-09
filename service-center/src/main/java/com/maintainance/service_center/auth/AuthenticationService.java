package com.maintainance.service_center.auth;

import com.maintainance.service_center.email.EmailService;
import com.maintainance.service_center.email.EmailTemplateName;
import com.maintainance.service_center.role.RoleRepository;
import com.maintainance.service_center.security.JwtService;
import com.maintainance.service_center.user.ApprovalStatus;
import com.maintainance.service_center.user.Token;
import com.maintainance.service_center.user.TokenRepository;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserRepository;
import com.maintainance.service_center.user.UserType;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.misc.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    @Value("${application.mail.frontend.activation-url}")
    private String activationUrl;

    @Transactional
    public void register(RegistrationRequest request) throws MessagingException {
        log.info("Registering new user with email: {}", request.getEmail());

        var userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalArgumentException("Role USER was not initialized"));

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("An account with this email address already exists.");
        }

        UserType userType = request.getUserType() != null ? request.getUserType() : UserType.CUSTOMER;
        ApprovalStatus approvalStatus = userType == UserType.CENTER_OWNER
                ? ApprovalStatus.PENDING_APPROVAL
                : ApprovalStatus.APPROVED;

        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .userType(userType)
                .approvalStatus(approvalStatus)
                .roles(List.of(userRole))
                .build();

        userRepository.save(user);
        log.info("User saved with ID: {}", user.getId());

        sendValidationEmail(user);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("Authenticating user: {}", request.getEmail());

        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = ((User) auth.getPrincipal());

        if (user.getApprovalStatus() == ApprovalStatus.REJECTED) {
            log.warn("Rejected center owner attempted login: {}", user.getEmail());
            throw new IllegalStateException("Your registration has been rejected. Please contact support.");
        }

        var claims = new HashMap<String, Object>();
        claims.put("fullName", user.fullName());

        String jwtToken = jwtService.generateToken(claims, user);
        log.info("User authenticated successfully: {}", user.getEmail());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .approvalStatus(user.getApprovalStatus())
                .build();
    }
    
    private void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);
        log.info("Sending validation email to: {} with token: {}", user.getEmail(), newToken);

        emailService.sendEmail(
                user.getEmail(),
                user.fullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Account activation"
        );

        log.info("Validation email sent successfully to: {}", user.getEmail());
    }

    @Transactional
    public String generateAndSaveActivationToken(User user) {
        String generatedToken = generateActivationCode(6);
        log.debug("Generated activation token: {} for user: {}", generatedToken, user.getEmail());

        var token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();

        tokenRepository.save(token);
        log.info("Token saved to database: {} for user ID: {}", generatedToken, user.getId());

        return generatedToken;
    }

    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }

    public void activateAccount(String token) throws MessagingException {
        log.info("Attempting to activate account with token: {}", token);

        // ✅ Better error handling
        var savedToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.error("Token not found in database: {}", token);
                    return new RuntimeException("Invalid token. Please check your email for the correct activation code.");
                });

        log.info("Token found in database. Checking expiration...");

        // Check if already validated
        if (savedToken.getValidatedAt() != null) {
            log.warn("Token already used: {}", token);
            throw new RuntimeException("This activation token has already been used.");
        }

        // Check if expired
        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            log.warn("Token expired: {}. Sending new token...", token);
            // Generate and save new token in separate method with its own transaction
            resendValidationToken(savedToken.getUser());
            throw new RuntimeException("Activation token has expired. A new token has been sent to your email.");
        }

        activateUserAccount(savedToken);
        log.info("Account activated successfully");
    }

    @Transactional
    public void activateUserAccount(Token savedToken) {
        var user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        log.info("Activating user: {}", user.getEmail());

        user.setEnabled(true);
        userRepository.save(user);

        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resendValidationToken(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);
        log.info("New token generated and committed: {}", newToken);

        // Send email after transaction commits
        emailService.sendEmail(
                user.getEmail(),
                user.fullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Account activation"
        );

        log.info("Validation email sent with new token: {}", newToken);
    }
}
