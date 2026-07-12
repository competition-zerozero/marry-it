package com.zerozero.marryit.auth.oauth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InviteTokenCaptureFilter extends OncePerRequestFilter {

    public static final String SESSION_PENDING_INVITE_TOKEN = "PENDING_INVITE_TOKEN";

    private static final String GOOGLE_AUTHORIZATION_PATH = "/oauth2/authorization/google";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (GOOGLE_AUTHORIZATION_PATH.equals(request.getRequestURI())) {
            String inviteToken = request.getParameter("inviteToken");
            if (inviteToken != null && !inviteToken.isBlank()) {
                request.getSession(true).setAttribute(SESSION_PENDING_INVITE_TOKEN, inviteToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
