package nl.mallepetrus.jiptv.service;

import nl.mallepetrus.jiptv.dto.InvitationResponse;
import nl.mallepetrus.jiptv.dto.InviteUserRequest;
import nl.mallepetrus.jiptv.dto.UserRegistrationRequest;
import nl.mallepetrus.jiptv.entity.Invitation;
import nl.mallepetrus.jiptv.entity.User;
import nl.mallepetrus.jiptv.entity.UserRole;
import nl.mallepetrus.jiptv.repository.InvitationRepository;
import nl.mallepetrus.jiptv.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InvitationService {

    private static final Logger logger = LoggerFactory.getLogger(InvitationService.class);
    private static final int INVITATION_EXPIRY_DAYS = 7;
    private static final int INVITATION_CODE_LENGTH = 32;
    private static final String INVITATION_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Send invitation to a new user (Admin only)
     */
    @Transactional
    public InvitationResponse inviteUser(InviteUserRequest request, User invitedBy) {
        // Validate that inviter is admin
        if (invitedBy.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only administrators can invite users");
        }

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        // Check if there's already a pending invitation
        Optional<Invitation> existingInvitation = invitationRepository.findByEmail(request.getEmail());
        if (existingInvitation.isPresent() && existingInvitation.get().isValid()) {
            throw new IllegalArgumentException("There is already a pending invitation for this email");
        }

        // Generate unique invitation code
        String invitationCode = generateInvitationCode();
        while (invitationRepository.findByInvitationCode(invitationCode).isPresent()) {
            invitationCode = generateInvitationCode();
        }

        // Create invitation
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(INVITATION_EXPIRY_DAYS);
        Invitation invitation = new Invitation(request.getEmail(), invitationCode, invitedBy, expiresAt);
        invitation = invitationRepository.save(invitation);

        // Send invitation email
        try {
            emailService.sendInvitationEmail(
                request.getEmail(), 
                invitationCode, 
                invitedBy.getEmail(), 
                request.getMessage()
            );
            logger.info("Invitation sent successfully to {} by {}", request.getEmail(), invitedBy.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send invitation email to {}", request.getEmail(), e);
            // Don't fail the invitation creation if email fails
        }

        return mapToInvitationResponse(invitation);
    }

    /**
     * Register new user with invitation code
     */
    @Transactional
    public User registerUser(UserRegistrationRequest request) {
        // Validate password match
        if (!request.isPasswordMatch()) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Find and validate invitation
        Invitation invitation = invitationRepository.findByInvitationCode(request.getInvitationCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid invitation code"));

        if (!invitation.isValid()) {
            throw new IllegalArgumentException("Invitation code is expired or already used");
        }

        // Validate email matches invitation
        if (!invitation.getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email does not match the invitation");
        }

        // Check if user already exists (double check)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        // Create new user
        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(UserRole.USER);
        newUser.setEmailVerified(true); // Email is verified through invitation
        newUser.setActive(true);

        newUser = userRepository.save(newUser);

        // Mark invitation as used
        invitation.setUsed(true);
        invitation.setUsedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(newUser.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send welcome email to {}", newUser.getEmail(), e);
            // Don't fail registration if welcome email fails
        }

        logger.info("User registered successfully: {} via invitation from {}", 
                   newUser.getEmail(), invitation.getInvitedBy().getEmail());

        return newUser;
    }

    /**
     * Get invitation details by code (for registration form pre-fill)
     */
    public InvitationResponse getInvitationByCode(String invitationCode) {
        Invitation invitation = invitationRepository.findByInvitationCode(invitationCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invitation code"));

        return mapToInvitationResponse(invitation);
    }

    /**
     * Get all invitations (Admin only)
     */
    public List<InvitationResponse> getAllInvitations(User requestingUser) {
        if (requestingUser.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only administrators can view all invitations");
        }

        return invitationRepository.findAll().stream()
                .map(this::mapToInvitationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get pending invitations (Admin only)
     */
    public List<InvitationResponse> getPendingInvitations(User requestingUser) {
        if (requestingUser.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only administrators can view pending invitations");
        }

        return invitationRepository.findValidInvitations(LocalDateTime.now()).stream()
                .map(this::mapToInvitationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cancel/revoke invitation (Admin only)
     */
    @Transactional
    public void cancelInvitation(Long invitationId, User requestingUser) {
        if (requestingUser.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only administrators can cancel invitations");
        }

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        if (invitation.isUsed()) {
            throw new IllegalArgumentException("Cannot cancel an invitation that has already been used");
        }

        // Mark as used to effectively cancel it
        invitation.setUsed(true);
        invitation.setUsedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        logger.info("Invitation {} cancelled by admin {}", invitationId, requestingUser.getEmail());
    }

    /**
     * Clean up expired invitations (scheduled task)
     */
    @Transactional
    public int cleanupExpiredInvitations() {
        List<Invitation> expiredInvitations = invitationRepository.findExpiredInvitations(LocalDateTime.now());
        
        for (Invitation invitation : expiredInvitations) {
            if (!invitation.isUsed()) {
                invitation.setUsed(true); // Mark as used to clean up
                invitationRepository.save(invitation);
            }
        }

        logger.info("Cleaned up {} expired invitations", expiredInvitations.size());
        return expiredInvitations.size();
    }

    private String generateInvitationCode() {
        StringBuilder code = new StringBuilder(INVITATION_CODE_LENGTH);
        for (int i = 0; i < INVITATION_CODE_LENGTH; i++) {
            code.append(INVITATION_CODE_CHARS.charAt(secureRandom.nextInt(INVITATION_CODE_CHARS.length())));
        }
        return code.toString();
    }

    private InvitationResponse mapToInvitationResponse(Invitation invitation) {
        InvitationResponse response = new InvitationResponse();
        response.setId(invitation.getId());
        response.setEmail(invitation.getEmail());
        response.setInvitationCode(invitation.getInvitationCode());
        response.setInvitedByEmail(invitation.getInvitedBy().getEmail());
        response.setCreatedAt(invitation.getCreatedAt());
        response.setExpiresAt(invitation.getExpiresAt());
        response.setUsed(invitation.isUsed());
        response.setUsedAt(invitation.getUsedAt());
        response.setExpired(invitation.isExpired());
        response.setValid(invitation.isValid());
        return response;
    }
}