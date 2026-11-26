package com.ktroute.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.internet.MimeMessage;

@SpringBootTest
class MailConnectionTest {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Test
    void sendTestEmail() throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(fromEmail);
        helper.setSubject("RouteSync — Mail Test");
        helper.setText("<h2>Mail is working!</h2><p>Your RouteSync email config is set up correctly.</p>", true);

        mailSender.send(message);
        System.out.println("✅ Test email sent to: " + fromEmail);
    }
}
