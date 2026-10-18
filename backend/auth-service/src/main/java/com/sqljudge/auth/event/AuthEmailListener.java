package com.sqljudge.auth.event;

import com.sqljudge.auth.config.EmailProperties;
import java.util.UUID;
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
public class AuthEmailListener {

    private static final String VERIFICATION_SUBJECT = "Xác thực email SQL Judge";
    private static final String PASSWORD_RESET_SUBJECT = "Đặt lại mật khẩu SQL Judge";
    private static final String INVITATION_SUBJECT = "Kích hoạt tài khoản SQL Judge";
    private static final String ACCOUNT_EXISTS_SUBJECT = "Email này đã có tài khoản SQL Judge";

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    @Async
    @TransactionalEventListener
    public void sendVerificationEmail(VerificationEmailRequestedEvent event) {
        String link = emailProperties.frontendUrl() + "/verify-email?token=" + event.token();
        String body =
                """
                Chào %s,

                Mã xác thực email của bạn là: %s

                Hoặc bấm liên kết sau để xác thực:
                %s

                Mã và liên kết có hiệu lực 15 phút và chỉ dùng được một lần.
                Nếu bạn không đăng ký SQL Judge, hãy bỏ qua email này.
                """
                        .formatted(event.username(), event.code(), link);
        send(event.userId(), event.email(), VERIFICATION_SUBJECT, body, "verification_email");
    }

    @Async
    @TransactionalEventListener
    public void sendPasswordResetEmail(PasswordResetEmailRequestedEvent event) {
        String link = emailProperties.frontendUrl() + "/reset-password?token=" + event.token();
        String body =
                """
                Chào %s,

                Bấm liên kết sau để đặt mật khẩu mới cho tài khoản SQL Judge của bạn:
                %s

                Liên kết có hiệu lực 30 phút và chỉ dùng được một lần.
                Sau khi đổi mật khẩu, mọi thiết bị đang đăng nhập sẽ bị đăng xuất.
                Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
                """
                        .formatted(event.username(), link);
        send(event.userId(), event.email(), PASSWORD_RESET_SUBJECT, body, "password_reset_email");
    }

    @Async
    @TransactionalEventListener
    public void sendInvitationEmail(InvitationEmailRequestedEvent event) {
        String link = emailProperties.frontendUrl() + "/reset-password?token=" + event.token();
        String body =
                """
                Chào %s,

                Quản trị viên SQL Judge vừa tạo tài khoản cho bạn.
                Bấm liên kết sau để đặt mật khẩu và kích hoạt tài khoản:
                %s

                Liên kết có hiệu lực 7 ngày và chỉ dùng được một lần.
                Nếu bạn không mong đợi email này, hãy bỏ qua nó.
                """
                        .formatted(event.username(), link);
        send(event.userId(), event.email(), INVITATION_SUBJECT, body, "invitation_email");
    }

    @Async
    @TransactionalEventListener
    public void sendAccountExistsEmail(AccountExistsEmailRequestedEvent event) {
        String body =
                """
                Chào %s,

                Có người vừa dùng email này để đăng ký SQL Judge, nhưng email đã có tài khoản.

                Đăng nhập tại:
                %s

                Nếu quên mật khẩu, đặt lại tại:
                %s

                Nếu không phải bạn, hãy bỏ qua email này. Tài khoản của bạn không bị thay đổi.
                """
                        .formatted(
                                event.username(),
                                emailProperties.frontendUrl() + "/login",
                                emailProperties.frontendUrl() + "/forgot-password");
        send(event.userId(), event.email(), ACCOUNT_EXISTS_SUBJECT, body, "account_exists_email");
    }

    private void send(UUID userId, String to, String subject, String body, String logEvent) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailProperties.from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            log.info("{}_sent userId={}", logEvent, userId);
        } catch (MailException exception) {
            log.warn(
                    "{}_failed userId={} reason={}",
                    logEvent,
                    userId,
                    exception.getClass().getSimpleName());
        }
    }
}
