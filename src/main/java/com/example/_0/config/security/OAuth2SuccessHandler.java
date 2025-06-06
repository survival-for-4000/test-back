package com.example._0.config.security;

import com.example._0.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example._0.oauth.entity.PrincipalDetails;
import com.example._0.oauth.token.TokenProvider;
import com.example._0.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

import static com.example._0.util.CookieUtils.addCookie;

@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final TokenProvider tokenProvider;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("${app.client.url}")
    private String defaultRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            Authentication finalAuth = authentication;
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
                PrincipalDetails principalDetails = (PrincipalDetails) oauth2Token.getPrincipal();

                // Custom UsernamePasswordAuthenticationToken 생성
                UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                        principalDetails,
                        null,
                        principalDetails.getAuthorities()
                ) {
                    @Override
                    public String getName() {
                        return ((PrincipalDetails) getPrincipal()).getName();
                    }
                };

                SecurityContextHolder.getContext().setAuthentication(newAuth);
                finalAuth=newAuth;
            }
            String accessToken = tokenProvider.generateAccessToken(finalAuth);

            String refreshToken = tokenProvider.generateRefreshToken(finalAuth);

            Optional<String> redirectUriCookie = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                    .map(cookie -> cookie.getValue());

            String redirectUri;
            if (redirectUriCookie.isPresent()) {
                redirectUri = redirectUriCookie.get();
            } else {
                redirectUri = defaultRedirectUrl;
            }

            addCookie(response, "_hoauth", accessToken, 3600);
            addCookie(response, "_hrauth", refreshToken, 604800);

            httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, redirectUri);

        } catch (Exception e) {
            log.error("OAuth2 성공 핸들러 예외 발생", e);
            throw e;
        }
    }
}