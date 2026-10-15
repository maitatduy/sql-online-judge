package com.sqljudge.auth.event;

import com.sqljudge.auth.config.EmailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationEmailListener {

    private static final String SUBJECT = "Xác thực email SQL Judge";

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    @Async
    @TransactionalEventListener
    public void sendVerificationEmail(VerificationEmailRequestedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailProperties.from());
        message.setTo(event.email());
        message.setSubject(SUBJECT);
        message.setText(body(event));
        try {
            mailSender.send(message);
            log.info("verification_email_sent userId={}", event.userId());
        } catch (MailException exception) {
            log.warn(
                    "verification_email_failed userId={} reason={}",
                    event.userId(),
                    exception.getClass().getSimpleName());
        }
    }

    private String body(VerificationEmailRequestedEvent event) {
        String link = emailProperties.frontendUrl() + "/verify-email?token=" + event.token();
        return """
        Chào %s,

        Mã xác thực email của bạn là: %s

        Hoặc bấm liên kết sau để xác thực:
        %s

        Mã và liên kết có hiệu lực 15 phút và chỉ dùng được một lần.
        Nếu bạn không đăng ký SQL Judge, hãy bỏ qua email này.
        """
                .formatted(event.username(), event.code(), link);
    }
}
