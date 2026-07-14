package com.zerozero.marryit.auth.oauth;

import com.zerozero.marryit.agent.mcp.McpOAuthController;
import com.zerozero.marryit.agent.mcp.McpOAuthRequest;
import com.zerozero.marryit.auth.service.OAuthLoginResult;
import com.zerozero.marryit.auth.service.OAuthLoginService;
import com.zerozero.marryit.auth.service.OAuthUserProfile;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String SESSION_USER_ID = "LOGIN_USER_ID";
    public static final String SESSION_WORKSPACE_ID = "CURRENT_WORKSPACE_ID";

    private final OAuthLoginService oauthLoginService;
    private final GoogleOAuthUserProfileMapper googleOAuthUserProfileMapper;
    private final McpOAuthController mcpOAuthController;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(
            OAuthLoginService oauthLoginService,
            GoogleOAuthUserProfileMapper googleOAuthUserProfileMapper,
            org.springframework.beans.factory.ObjectProvider<McpOAuthController> mcpOAuthController,
            @Value("${app.frontend-url:/}") String frontendUrl
    ) {
        this.oauthLoginService = oauthLoginService;
        this.googleOAuthUserProfileMapper = googleOAuthUserProfileMapper;
        this.mcpOAuthController = mcpOAuthController.getIfAvailable();
        this.frontendUrl = frontendUrl;
        setDefaultTargetUrl(frontendUrl);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuthUserProfile profile = toProfile(authentication);
        OAuthLoginResult loginResult = oauthLoginService.login(profile);

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_USER_ID, loginResult.user().getId());
        session.setAttribute(SESSION_WORKSPACE_ID, loginResult.defaultWorkspace().getId());

        Object pendingMcpOAuthRequest = session.getAttribute(McpOAuthController.SESSION_PENDING_MCP_OAUTH_REQUEST);
        if (pendingMcpOAuthRequest instanceof McpOAuthRequest mcpOAuthRequest && mcpOAuthController != null) {
            session.removeAttribute(McpOAuthController.SESSION_PENDING_MCP_OAUTH_REQUEST);
            mcpOAuthController.completePendingAuthorization(response, loginResult.user().getId(), mcpOAuthRequest);
            return;
        }

        Object pendingInviteToken = session.getAttribute(InviteTokenCaptureFilter.SESSION_PENDING_INVITE_TOKEN);
        if (pendingInviteToken instanceof String inviteToken && !inviteToken.isBlank()) {
            session.removeAttribute(InviteTokenCaptureFilter.SESSION_PENDING_INVITE_TOKEN);
            String targetUrl = frontendUrl + "/?inviteToken=" + URLEncoder.encode(inviteToken, StandardCharsets.UTF_8);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private OAuthUserProfile toProfile(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Token)) {
            throw new IllegalArgumentException("Unsupported authentication type.");
        }

        if (!"google".equals(oauth2Token.getAuthorizedClientRegistrationId())) {
            throw new IllegalArgumentException("Unsupported OAuth provider.");
        }

        return googleOAuthUserProfileMapper.map(oauth2Token.getPrincipal());
    }
}
