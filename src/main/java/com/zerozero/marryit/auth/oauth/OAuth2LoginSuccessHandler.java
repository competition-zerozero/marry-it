package com.zerozero.marryit.auth.oauth;

import com.zerozero.marryit.auth.service.OAuthLoginResult;
import com.zerozero.marryit.auth.service.OAuthLoginService;
import com.zerozero.marryit.auth.service.OAuthUserProfile;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    public OAuth2LoginSuccessHandler(
            OAuthLoginService oauthLoginService,
            GoogleOAuthUserProfileMapper googleOAuthUserProfileMapper
    ) {
        this.oauthLoginService = oauthLoginService;
        this.googleOAuthUserProfileMapper = googleOAuthUserProfileMapper;
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuthUserProfile profile = toProfile(authentication);
        OAuthLoginResult loginResult = oauthLoginService.login(profile);

        request.getSession(true).setAttribute(SESSION_USER_ID, loginResult.user().getId());
        request.getSession(true).setAttribute(SESSION_WORKSPACE_ID, loginResult.defaultWorkspace().getId());

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
