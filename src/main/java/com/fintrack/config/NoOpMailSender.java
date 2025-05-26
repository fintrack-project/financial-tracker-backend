package com.fintrack.config;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.InputStream;
import java.util.Properties;

@Configuration
@ConditionalOnProperty(name = "spring.mail.host", havingValue = "localhost", matchIfMissing = true)
public class NoOpMailSender {
    private static final Logger logger = LoggerFactory.getLogger(NoOpMailSender.class);

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        logger.info("Using NoOpMailSender - Mail is not configured");
        return new JavaMailSenderImpl() {
            @Override
            public void send(MimeMessage mimeMessage) throws MailException {
                logger.debug("NoOpMailSender: Suppressed sending of email");
            }

            @Override
            public void send(MimeMessage... mimeMessages) throws MailException {
                logger.debug("NoOpMailSender: Suppressed sending of {} emails", mimeMessages.length);
            }

            @Override
            public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
                logger.debug("NoOpMailSender: Suppressed sending of prepared email");
            }

            @Override
            public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
                logger.debug("NoOpMailSender: Suppressed sending of {} prepared emails", mimeMessagePreparators.length);
            }

            @Override
            public void send(SimpleMailMessage simpleMessage) throws MailException {
                logger.debug("NoOpMailSender: Suppressed sending of simple email");
            }

            @Override
            public void send(SimpleMailMessage... simpleMessages) throws MailException {
                logger.debug("NoOpMailSender: Suppressed sending of {} simple emails", simpleMessages.length);
            }

            @Override
            public MimeMessage createMimeMessage() {
                return new MimeMessage((Session) null);
            }

            @Override
            public MimeMessage createMimeMessage(InputStream contentStream) throws MailParseException {
                return new MimeMessage((Session) null);
            }
        };
    }
} 