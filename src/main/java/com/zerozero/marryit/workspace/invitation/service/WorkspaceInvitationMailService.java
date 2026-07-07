package com.zerozero.marryit.workspace.invitation.service;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceInvitationMailService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceInvitationMailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String fromAddress;
    private final String appBaseUrl;

    public WorkspaceInvitationMailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${workspace.invitation.mail.enabled:false}") boolean enabled,
            @Value("${workspace.invitation.mail.from:}") String fromAddress,
            @Value("${workspace.invitation.app-base-url:http://localhost:8080}") String appBaseUrl
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
        this.appBaseUrl = appBaseUrl;
    }

    public void sendInvitation(WorkspaceInvitationResponse invitation) {
        Optional<JavaMailSender> mailSender = Optional.ofNullable(mailSenderProvider.getIfAvailable());
        if (!enabled || mailSender.isEmpty() || fromAddress.isBlank()) {
            log.info("Skip sending invitation mail for {} because mail is not configured.", invitation.invitedEmail());
            return;
        }

        String inviteLink = appBaseUrl + invitation.inviteUrl();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(invitation.invitedEmail());
        message.setSubject("[marry-it] 워크스페이스 초대");
        message.setText("""
                %s 님을 %s 워크스페이스에 초대했습니다.

                역할: %s
                초대 링크: %s
                만료일: %s

                링크를 열고 Google 계정으로 로그인한 뒤 초대를 수락하세요.
                """.formatted(
                invitation.invitedEmail(),
                invitation.workspaceName(),
                invitation.role(),
                inviteLink,
                invitation.expiresAt()
        ));
        mailSender.get().send(message);
    }
}
