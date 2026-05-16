package com.ilgiyebo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromAddress;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendVerificationEmail(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("[일기예보] 대학 이메일 인증 코드");

            Context context = new Context();
            context.setVariable("code", code);
            String html = templateEngine.process("verification-email", context);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("인증 코드 이메일 발송 완료: to={}", to);
        } catch (MessagingException e) {
            log.error("인증 코드 이메일 발송 실패: to={}", to, e);
            throw new RuntimeException("이메일 발송에 실패했습니다", e);
        }
    }
}
