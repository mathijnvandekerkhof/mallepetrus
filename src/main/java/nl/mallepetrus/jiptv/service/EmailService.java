package nl.mallepetrus.jiptv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${jiptv.mail.from}")
    private String fromEmail;

    @Value("${jiptv.domains.user:localhost:8080}")
    private String userDomain;

    /**
     * Send invitation email to new user
     */
    public void sendInvitationEmail(String toEmail, String invitationCode, String invitedByEmail, String personalMessage) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("JIPTV - You're invited to join!");

            String htmlContent = buildInvitationEmailContent(toEmail, invitationCode, invitedByEmail, personalMessage);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Invitation email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send invitation email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send invitation email", e);
        }
    }

    /**
     * Send welcome email after successful registration
     */
    public void sendWelcomeEmail(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to JIPTV!");

            String htmlContent = buildWelcomeEmailContent(toEmail);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Welcome email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send welcome email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    /**
     * Send simple text email (fallback method)
     */
    public void sendSimpleEmail(String toEmail, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            logger.info("Simple email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send simple email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildInvitationEmailContent(String toEmail, String invitationCode, String invitedByEmail, String personalMessage) {
        String registrationUrl = String.format("http://%s/register?code=%s", userDomain, invitationCode);
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'></head><body>");
        html.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        
        // Header
        html.append("<div style='background-color: #2c3e50; color: white; padding: 20px; text-align: center;'>");
        html.append("<h1>JIPTV Invitation</h1>");
        html.append("</div>");
        
        // Content
        html.append("<div style='padding: 20px; background-color: #f8f9fa;'>");
        html.append("<h2>You're invited to join JIPTV!</h2>");
        html.append("<p>Hello,</p>");
        html.append("<p>You have been invited by <strong>").append(invitedByEmail).append("</strong> to join JIPTV.</p>");
        
        if (personalMessage != null && !personalMessage.trim().isEmpty()) {
            html.append("<div style='background-color: #e9ecef; padding: 15px; border-left: 4px solid #007bff; margin: 20px 0;'>");
            html.append("<p><em>Personal message:</em></p>");
            html.append("<p>").append(personalMessage).append("</p>");
            html.append("</div>");
        }
        
        html.append("<p>To complete your registration, please click the button below:</p>");
        
        // CTA Button
        html.append("<div style='text-align: center; margin: 30px 0;'>");
        html.append("<a href='").append(registrationUrl).append("' ");
        html.append("style='background-color: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>");
        html.append("Complete Registration");
        html.append("</a>");
        html.append("</div>");
        
        html.append("<p>Or copy and paste this link in your browser:</p>");
        html.append("<p style='word-break: break-all; background-color: #e9ecef; padding: 10px; border-radius: 3px;'>");
        html.append(registrationUrl);
        html.append("</p>");
        
        html.append("<p><strong>Your invitation code:</strong> <code>").append(invitationCode).append("</code></p>");
        html.append("<p><em>This invitation will expire in 7 days.</em></p>");
        
        html.append("</div>");
        
        // Footer
        html.append("<div style='background-color: #6c757d; color: white; padding: 15px; text-align: center; font-size: 12px;'>");
        html.append("<p>JIPTV - Secure IPTV Streaming Platform</p>");
        html.append("<p>If you didn't expect this invitation, you can safely ignore this email.</p>");
        html.append("</div>");
        
        html.append("</div></body></html>");
        
        return html.toString();
    }

    private String buildWelcomeEmailContent(String toEmail) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'></head><body>");
        html.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        
        // Header
        html.append("<div style='background-color: #28a745; color: white; padding: 20px; text-align: center;'>");
        html.append("<h1>Welcome to JIPTV!</h1>");
        html.append("</div>");
        
        // Content
        html.append("<div style='padding: 20px; background-color: #f8f9fa;'>");
        html.append("<h2>Registration Successful!</h2>");
        html.append("<p>Hello,</p>");
        html.append("<p>Welcome to JIPTV! Your account has been successfully created.</p>");
        
        html.append("<h3>Next Steps:</h3>");
        html.append("<ol>");
        html.append("<li><strong>Set up Multi-Factor Authentication (MFA)</strong> - Secure your account with TOTP</li>");
        html.append("<li><strong>Download the WebOS TV App</strong> - Install the JIPTV app on your smart TV</li>");
        html.append("<li><strong>Pair your TV device</strong> - Use QR code scanning to link your TV</li>");
        html.append("<li><strong>Add your IPTV provider</strong> - Configure your streaming credentials</li>");
        html.append("</ol>");
        
        html.append("<p>If you need any assistance, please contact your administrator.</p>");
        html.append("<p>Enjoy streaming with JIPTV!</p>");
        
        html.append("</div>");
        
        // Footer
        html.append("<div style='background-color: #6c757d; color: white; padding: 15px; text-align: center; font-size: 12px;'>");
        html.append("<p>JIPTV - Secure IPTV Streaming Platform</p>");
        html.append("</div>");
        
        html.append("</div></body></html>");
        
        return html.toString();
    }
}