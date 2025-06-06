package com.example._0.oauth.service;

import com.example._0.entity.Member;
import com.example._0.oauth.entity.OAuth2UserInfo;
import com.example._0.oauth.entity.PrincipalDetails;
import com.example._0.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfo.of(registrationId, attributes);
        Member member = getOrSave(oAuth2UserInfo);

        PrincipalDetails principalDetails = new PrincipalDetails(member, attributes);

        return principalDetails;
    }

    private Member getOrSave(OAuth2UserInfo oAuth2UserInfo) {
        return memberRepository.findByProviderId(oAuth2UserInfo.providerId())
                .orElseGet(() -> {
                    Member newMember = oAuth2UserInfo.toEntity();
                    return memberRepository.save(newMember);
                });
    }
}
