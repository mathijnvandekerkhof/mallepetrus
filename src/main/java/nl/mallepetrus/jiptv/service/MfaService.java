package nl.mallepetrus.jiptv.service;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import nl.mallepetrus.jiptv.dto.MfaSetupResponse;
import nl.mallepetrus.jiptv.entity.User;
import nl.mallepetrus.jiptv.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

@Service
public class MfaService {

    @Autowired
    private UserRepository userRepository;

    @Value("${jiptv.mfa.issuer:JIPTV}")
    private String issuer;

    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeGenerator codeGenerator;
    private final CodeVerifier codeVerifier;

    public MfaService() {
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();
        this.codeGenerator = new DefaultCodeGenerator();
        
        TimeProvider timeProvider = new SystemTimeProvider();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    @Transactional
    public MfaSetupResponse generateMfaSetup(User user) {
        // Generate a new secret for the user
        String secret = secretGenerator.generate();
        
        // Save the secret to the user (but don't enable MFA yet)
        user.setMfaSecret(secret);
        userRepository.save(user);

        // Generate QR code data
        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer(issuer)
                .digits(6)
                .period(30)
                .build();

        try {
            // Generate QR code as base64 image
            byte[] qrCodeImage = qrGenerator.generate(data);
            String qrCodeBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCodeImage);
            
            return new MfaSetupResponse(
                    secret,
                    qrCodeBase64,
                    secret, // Manual entry key is the same as secret
                    user.isMfaEnabled()
            );
        } catch (QrGenerationException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    @Transactional
    public boolean enableMfa(User user, String totpCode) {
        if (user.getMfaSecret() == null) {
            throw new IllegalStateException("MFA secret not generated. Call generateMfaSetup first.");
        }

        // Verify the TOTP code
        if (verifyTotpCode(user.getMfaSecret(), totpCode)) {
            user.setMfaEnabled(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean disableMfa(User user, String totpCode) {
        if (!user.isMfaEnabled()) {
            return true; // Already disabled
        }

        // Verify the TOTP code before disabling
        if (verifyTotpCode(user.getMfaSecret(), totpCode)) {
            user.setMfaEnabled(false);
            user.setMfaSecret(null); // Remove the secret
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public boolean verifyTotpCode(String secret, String code) {
        if (secret == null || code == null) {
            return false;
        }
        
        try {
            return codeVerifier.isValidCode(secret, code);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isMfaRequired(User user) {
        return user.isMfaEnabled() && user.getMfaSecret() != null;
    }

    public MfaSetupResponse getMfaStatus(User user) {
        return new MfaSetupResponse(
                null, // Don't expose the secret
                null, // Don't generate QR code for status check
                null, // Don't expose manual entry key
                user.isMfaEnabled()
        );
    }
}