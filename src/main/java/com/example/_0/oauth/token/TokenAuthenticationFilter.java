package com.example._0.oauth.token;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String accessToken = resolveToken(request);

        try {
            // 토큰이 존재할 때만 검증 수행
            if (accessToken != null && tokenProvider.validateToken(accessToken)) {
                setAuthentication(accessToken);
            }
            // 토큰이 없으면 그냥 통과 (permitAll 경로에서는 문제없음)
        } catch (ExpiredJwtException | JwtException e) {
            throw e;
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(String accessToken) {
        Authentication authentication = tokenProvider.getAuthentication(accessToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    protected String resolveToken(HttpServletRequest request) {
        HttpServletRequest originalRequest = (HttpServletRequest) request.getAttribute("originalRequest");
        if (originalRequest == null) {
            originalRequest = request;
        }

        Cookie[] cookies = originalRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("_hoauth".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}