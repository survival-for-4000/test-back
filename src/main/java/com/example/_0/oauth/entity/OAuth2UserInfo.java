package com.example._0.oauth.entity;


import com.example._0.entity.AuthProvider;
import com.example._0.entity.Member;
import com.example._0.entity.Role;
import lombok.Builder;

import java.util.Map;

@Builder
public record OAuth2UserInfo(
        String name,
        String email,
        String profile,
        String provider,
        String providerId
) {

    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> ofGoogle(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 registrationId입니다: " + registrationId);
        };
    }

    private static OAuth2UserInfo ofGoogle(Map<String, Object> attributes) {
        return OAuth2UserInfo.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .profile((String) attributes.get("picture"))
                .provider("GOOGLE")
                .providerId(attributes.get("sub").toString())
                .build();
    }


    public Member toEntity() {
        return Member.builder()
                .name(name)
                .email(email)
                .profile(profile)
                .provider(AuthProvider.valueOf(provider))
                .providerId(providerId)
                .role(Role.USER)
                .build();
    }
}
