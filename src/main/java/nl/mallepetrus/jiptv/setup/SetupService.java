package nl.mallepetrus.jiptv.setup;

import nl.mallepetrus.jiptv.entity.User;
import nl.mallepetrus.jiptv.entity.UserRole;
import nl.mallepetrus.jiptv.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetupService {

    private static final Logger logger = LoggerFactory.getLogger(SetupService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Check if the application needs initial setup
     */
    public boolean needsSetup() {
        long adminCount = userRepository.countByRole(UserRole.ADMIN);
        boolean needsSetup = adminCount == 0;
        
        logger.info("Setup check: {} admin users found, needs setup: {}", adminCount, needsSetup);
        return needsSetup;
    }

    /**
     * Create the initial admin user
     */
    @Transactional
    public User createInitialAdmin(String email, String password) {
        if (!needsSetup()) {
            throw new IllegalStateException("Setup has already been completed");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email already exists");
        }

        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        admin.setEmailVerified(true); // Admin is automatically verified

        User savedAdmin = userRepository.save(admin);
        logger.info("Initial admin user created with email: {}", email);
        
        return savedAdmin;
    }

    /**
     * Get setup status information
     */
    public SetupStatus getSetupStatus() {
        boolean needsSetup = needsSetup();
        long totalUsers = userRepository.count();
        long adminUsers = userRepository.countByRole(UserRole.ADMIN);
        
        return new SetupStatus(needsSetup, totalUsers, adminUsers);
    }

    public static class SetupStatus {
        private final boolean needsSetup;
        private final long totalUsers;
        private final long adminUsers;

        public SetupStatus(boolean needsSetup, long totalUsers, long adminUsers) {
            this.needsSetup = needsSetup;
            this.totalUsers = totalUsers;
            this.adminUsers = adminUsers;
        }

        public boolean isNeedsSetup() {
            return needsSetup;
        }

        public long getTotalUsers() {
            return totalUsers;
        }

        public long getAdminUsers() {
            return adminUsers;
        }
    }
}