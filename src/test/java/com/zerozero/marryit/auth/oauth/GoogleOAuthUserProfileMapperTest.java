package com.zerozero.marryit.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zerozero.marryit.auth.domain.OAuthProvider;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class GoogleOAuthUserProfileMapperTest {

    private final GoogleOAuthUserProfileMapper mapper = new GoogleOAuthUserProfileMapper();

    @Test
    void mapsGoogleAttributesToOAuthProfile() {
        OAuth2User oauth2User = googleUser(Map.of(
                "sub", "google-123",
                "email", "planner@example.com",
                "name", "서영",
                "picture", "https://example.com/profile.png"
        ));

        var profile = mapper.map(oauth2User);

        assertThat(profile.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(profile.providerUserId()).isEqualTo("google-123");
        assertThat(profile.email()).isEqualTo("planner@example.com");
        assertThat(profile.name()).isEqualTo("서영");
        assertThat(profile.profileImageUrl()).isEqualTo("https://example.com/profile.png");
    }

    @Test
    void rejectsProfileWithoutProviderUserId() {
        OAuth2User oauth2User = googleUser(Map.of(
                "email", "planner@example.com",
                "name", "서영"
        ));

        assertThatThrownBy(() -> mapper.map(oauth2User))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sub");
    }

    private OAuth2User googleUser(Map<String, Object> attributes) {
        return new DefaultOAuth2User(
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                attributes.containsKey("sub") ? "sub" : "email"
        );
    }
}
