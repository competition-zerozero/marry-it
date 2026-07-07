package com.zerozero.marryit.auth.service;

import com.zerozero.marryit.auth.domain.OAuthProvider;

public record OAuthUserProfile(
        OAuthProvider provider,
        String providerUserId,
        String email,
        String name,
        String profileImageUrl
) {
}
