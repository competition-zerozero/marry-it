package com.zerozero.marryit.auth.oauth;

import com.zerozero.marryit.auth.domain.OAuthProvider;
import com.zerozero.marryit.auth.service.OAuthUserProfile;
import java.util.Map;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthUserProfileMapper {

    public OAuthUserProfile map(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        String providerUserId = requiredString(attributes, "sub");
        String email = requiredString(attributes, "email");
        String name = requiredString(attributes, "name");
        String profileImageUrl = optionalString(attributes, "picture");

        return new OAuthUserProfile(
                OAuthProvider.GOOGLE,
                providerUserId,
                email,
                name,
                profileImageUrl
        );
    }

    private String requiredString(Map<String, Object> attributes, String name) {
        String value = optionalString(attributes, name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing Google OAuth attribute: " + name);
        }
        return value;
    }

    private String optionalString(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        return value == null ? null : value.toString();
    }
}
