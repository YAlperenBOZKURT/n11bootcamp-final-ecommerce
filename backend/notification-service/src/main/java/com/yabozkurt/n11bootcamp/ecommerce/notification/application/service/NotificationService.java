package com.yabozkurt.n11bootcamp.ecommerce.notification.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.OrderCancelledEvent;
import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.OrderConfirmedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.OrderFailedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.PasswordResetRequestedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.UserRegisteredEvent;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String WELCOME_TEMPLATE_PATH = "templates/welcome-email.html";
    private static final String PASSWORD_RESET_TEMPLATE_PATH = "templates/password-reset-email.html";
    private static final String ORDER_TEMPLATE_PATH = "templates/order-email.html";

    @Nullable
    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String welcomeTemplate;
    private final String passwordResetTemplate;
    private final String orderTemplate;

    public NotificationService(@Nullable JavaMailSender mailSender,
                               @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.welcomeTemplate = loadTemplate(WELCOME_TEMPLATE_PATH);
        this.passwordResetTemplate = loadTemplate(PASSWORD_RESET_TEMPLATE_PATH);
        this.orderTemplate = loadTemplate(ORDER_TEMPLATE_PATH);
    }

    public void sendOrderConfirmed(OrderConfirmedEvent event) {
        log.info("[NOTIFICATION] Order confirmed - orderId={}, userId={}, email={}, amount={}",
                event.getOrderId(), event.getUserId(), event.getUserEmail(), event.getTotalAmount());

        String subject = "Siparişiniz Onaylandı - #" + event.getOrderId();
        String text = "Harika haber! Siparişiniz onaylandı.\n\n"
                + "Sipariş No: #" + event.getOrderId() + "\n"
                + "Tutar: " + event.getTotalAmount() + " TL\n\n"
                + "Siparişiniz hazırlanırken sizi bilgilendirmeye devam edeceğiz.";

        String html = renderOrderTemplate(
                "Siparişiniz Onaylandı",
                "Harika haber! Siparişiniz başarıyla onaylandı.",
                "Sipariş No: #" + event.getOrderId() + "<br/>Tutar: " + event.getTotalAmount() + " TL",
                "Siparişiniz hazırlanırken sizi bilgilendirmeye devam edeceğiz."
        );

        sendEmail(event.getUserEmail(), subject, text, html);
    }

    public void sendOrderFailed(OrderFailedEvent event) {
        log.warn("[NOTIFICATION] Order failed - orderId={}, userId={}, reason={}",
                event.getOrderId(), event.getUserId(), event.getReason());

        String subject = "Siparişiniz Tamamlanamadı - #" + event.getOrderId();
        String text = "Üzgünüz, siparişiniz işlenemedi.\n\n"
                + "Sipariş No: #" + event.getOrderId() + "\n"
                + "Sebep: " + event.getReason() + "\n\n"
                + "Lütfen ödeme bilgilerinizi kontrol ederek tekrar deneyin.";

        String html = renderOrderTemplate(
                "Siparişiniz Tamamlanamadı",
                "Üzgünüz, siparişiniz işlenemedi.",
                "Sipariş No: #" + event.getOrderId() + "<br/>Sebep: " + escapeHtml(event.getReason()),
                "Lütfen ödeme bilgilerinizi kontrol ederek tekrar deneyin."
        );

        sendEmail(event.getUserEmail(), subject, text, html);
    }

    public void sendOrderCancelled(OrderCancelledEvent event) {
        log.info("[NOTIFICATION] Order cancelled - orderId={}, userId={}, reason={}",
                event.getOrderId(), event.getUserId(), event.getReason());

        String subject = "Siparişiniz İptal Edildi - #" + event.getOrderId();
        String text = "Siparişiniz iptal edildi.\n\n"
                + "Sipariş No: #" + event.getOrderId() + "\n"
                + "Sebep: " + event.getReason() + "\n\n"
                + "İsterseniz ürünleri yeniden sepetinize ekleyebilirsiniz.";

        String html = renderOrderTemplate(
                "Siparişiniz İptal Edildi",
                "Siparişiniz iptal edildi.",
                "Sipariş No: #" + event.getOrderId() + "<br/>Sebep: " + escapeHtml(event.getReason()),
                "İsterseniz ürünleri yeniden sepetinize ekleyebilirsiniz."
        );

        sendEmail(event.getUserEmail(), subject, text, html);
    }

    public void sendWelcome(UserRegisteredEvent event) {
        log.info("[NOTIFICATION] User registered - userId={}, email={}", event.getUserId(), event.getEmail());

        String subject = "Bozkurt'a Hoş Geldiniz!";
        String text = "Merhaba " + event.getFirstName() + ",\n\n"
                + "Aramıza hoş geldiniz! Hesabınız başarıyla oluşturuldu.\n"
                + "Artık ürünleri keşfedebilir, sepet oluşturabilir ve güvenle alışveriş yapabilirsiniz.\n\n"
                + "Keyifli alışverişler dileriz.";

        String html = renderWelcomeTemplate(
                "Bozkurt'a Hoş Geldiniz!",
                "Merhaba " + escapeHtml(event.getFirstName()) + ", aramıza hoş geldiniz.",
                "Hesabınız başarıyla oluşturuldu.",
                "Artık ürünleri keşfedebilir, sepet oluşturabilir ve güvenle alışveriş yapabilirsiniz."
        );

        sendEmail(event.getEmail(), subject, text, html);
    }

    public void sendPasswordReset(PasswordResetRequestedEvent event) {
        log.info("[NOTIFICATION] Password reset requested - userId={}, email={}", event.getUserId(), event.getEmail());

        String subject = "Şifre Sıfırlama Talebi";
        String text = "Merhaba " + event.getFirstName() + ",\n\n"
                + "Şifrenizi sıfırlamak için kullanacağınız kod:\n"
                + event.getResetToken() + "\n\n"
                + "Bu kod " + event.getExpiresInSeconds() + " saniye içinde geçerliliğini yitirecektir.\n"
                + "Eğer bu talebi siz oluşturmadıysanız bu e-postayı güvenle yok sayabilirsiniz.";

        String html = renderPasswordResetTemplate(
                "Şifre Sıfırlama Talebi",
                "Merhaba " + escapeHtml(event.getFirstName()) + ", şifrenizi sıfırlamak için aşağıdaki kodu kullanın.",
                escapeHtml(event.getResetToken()),
                "Bu kod " + event.getExpiresInSeconds() + " saniye içinde geçerliliğini yitirecektir.",
                "Eğer bu talebi siz oluşturmadıysanız bu e-postayı güvenle yok sayabilirsiniz."
        );

        sendEmail(event.getEmail(), subject, text, html);
    }

    private void sendEmail(String to, String subject, String textBody, String htmlBody) {
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("MAIL_USERNAME is not configured. Skipping email send. to={}, subject={}", to, subject);
            return;
        }
        if (mailSender == null) {
            log.warn("JavaMailSender is not available. Skipping email send. to={}, subject={}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);
            mailSender.send(message);
            log.info("Email sent to={} subject={}", to, subject);
        } catch (Exception ex) {
            log.error("Email send failed to={} subject={}: {}", to, subject, ex.getMessage());
        }
    }

    private String renderOrderTemplate(String title, String subtitle, String body, String footer) {
        return orderTemplate
                .replace("{{title}}", escapeHtml(title))
                .replace("{{heading}}", escapeHtml(title))
                .replace("{{subtitle}}", escapeHtml(subtitle))
                .replace("{{body}}", body)
                .replace("{{footer}}", escapeHtml(footer));
    }

    private String renderWelcomeTemplate(String title, String subtitle, String body, String footer) {
        return welcomeTemplate
                .replace("{{title}}", escapeHtml(title))
                .replace("{{heading}}", escapeHtml(title))
                .replace("{{subtitle}}", escapeHtml(subtitle))
                .replace("{{body}}", escapeHtml(body))
                .replace("{{footer}}", escapeHtml(footer));
    }

    private String renderPasswordResetTemplate(String title, String subtitle, String token, String expiry, String footer) {
        return passwordResetTemplate
                .replace("{{title}}", escapeHtml(title))
                .replace("{{heading}}", escapeHtml(title))
                .replace("{{subtitle}}", escapeHtml(subtitle))
                .replace("{{token}}", token)
                .replace("{{expiry}}", escapeHtml(expiry))
                .replace("{{footer}}", escapeHtml(footer));
    }

    private String loadTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Email template could not be loaded: " + path, ex);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
