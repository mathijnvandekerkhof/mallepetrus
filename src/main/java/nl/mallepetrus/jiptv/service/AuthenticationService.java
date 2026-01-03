package nl.mallepetrus.jiptv.service;

import nl.mallepetrus.jiptv.dto.JwtAuthenticationResponse;
import nl.mallepetrus.jiptv.dto.LoginRequest;
import nl.mallepetrus.jiptv.dto.RefreshTokenRequest;
import nl.mallepetrus.jiptv.entity.User;
import nl.mallepetrus.jiptv.repository.UserRepository;
import nl.mallepetrus.jiptv.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MfaService mfaService;

    @Autowired
    private ZeroTrustService zeroTrustService;

    @Value("${jiptv.jwt.expiration}")
    private int jwtExpirationInMs;

    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest) {
        try {
            // First, authenticate with username and password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Get user information
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Check if MFA is required
            if (mfaService.isMfaRequired(user)) {
                // If MFA is required but no TOTP code provided
                if (loginRequest.getTotpCode() == null || loginRequest.getTotpCode().trim().isEmpty()) {
                    throw new BadCredentialsException("MFA code required");
                }

                // Verify TOTP code
                if (!mfaService.verifyTotpCode(user.getMfaSecret(), loginRequest.getTotpCode())) {
                    throw new BadCredentialsException("Invalid MFA code");
                }
            }

            // Generate tokens
            String accessToken = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(loginRequest.getEmail());

            JwtAuthenticationResponse.UserInfo userInfo = new JwtAuthenticationResponse.UserInfo(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()
            );

            return new JwtAuthenticationResponse(
                    accessToken,
                    refreshToken,
                    (long) jwtExpirationInMs,
                    userInfo
            );

        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid credentials or MFA code");
        }
    }

    public JwtAuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Load user details for proper authentication
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                java.util.Collections.singletonList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                )
        );

        // Create new authentication for token generation
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String newAccessToken = tokenProvider.generateToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(username);

        JwtAuthenticationResponse.UserInfo userInfo = new JwtAuthenticationResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return new JwtAuthenticationResponse(
                newAccessToken,
                newRefreshToken,
                (long) jwtExpirationInMs,
                userInfo
        );
    }
}