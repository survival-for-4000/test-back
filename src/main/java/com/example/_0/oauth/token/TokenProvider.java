package com.example._0.oauth.token;


import com.example._0.entity.Member;
import com.example._0.exception.InvalidJwtSignatureException;
import com.example._0.exception.InvalidTokenException;
import com.example._0.oauth.entity.PrincipalDetails;
import com.example._0.oauth.token.service.TokenService;
import com.example._0.repository.MemberRepository;
import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class TokenProvider {

    @Value("${jwt.key}")
    private String key;
    private SecretKey secretKey;
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60L;
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60L * 24 * 7;
    private static final String KEY_ROLE = "role";
    private final TokenService tokenService;
    private final MemberRepository memberRepository;

    @PostConstruct
    private void setSecretKey() {
        secretKey = Keys.hmacShaKeyFor(key.getBytes());
    }

    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication, ACCESS_TOKEN_EXPIRE_TIME);
    }

    public String generateRefreshToken(Authentication authentication) {
        String refreshToken = generateToken(authentication, REFRESH_TOKEN_EXPIRE_TIME);
        tokenService.saveRefreshToken(authentication.getName(), refreshToken);
        return refreshToken;
    }

    private String generateToken(Authentication authentication, long expireTime) {
        Date now = new Date();
        Date expiredDate = new Date(now.getTime() + expireTime);

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining());

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        String providerId= principalDetails.user().getProviderId();

        return Jwts.builder()
                .setSubject(providerId)
                .claim(KEY_ROLE, authorities)
                .setIssuedAt(now)
                .setExpiration(expiredDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateAccessTokenFromRefreshToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new InvalidTokenException("토큰 형식이 올바르지 않습니다.");
        }

        Claims claims = parseClaims(refreshToken);
        String providerId = claims.getSubject();
        String role = claims.get("role", String.class);

        String storedToken = tokenService.getRefreshToken(providerId);

        if (!refreshToken.equals(storedToken)) {
            throw new InvalidTokenException("토큰 형식이 올바르지 않습니다.");
        }

        return Jwts.builder()
                .setSubject(providerId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRE_TIME))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        String providerId = claims.getSubject();
        Member member = memberRepository.findByProviderId(providerId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        PrincipalDetails principalDetails = new PrincipalDetails(
                member,
                new HashMap<>()
        );

        return new UsernamePasswordAuthenticationToken(principalDetails, "", principalDetails.getAuthorities());
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            boolean isValid = claims.getExpiration().after(new Date());
            return isValid;
        } catch (Exception e) {
            throw new InvalidTokenException("토큰 형식이 올바르지 않습니다.");
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("토큰 형식이 올바르지 않습니다.");
        } catch (SecurityException e) {
            throw new InvalidJwtSignatureException("JWT 서명이 유효하지 않습니다.");
        }
    }
}
