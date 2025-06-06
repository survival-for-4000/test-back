package com.example._0.oauth.token.controller;

import com.example._0.oauth.token.TokenProvider;
import com.example._0.oauth.token.dto.AccessTokenRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RequiredArgsConstructor
@RestController
public class TokenController {

    private final TokenProvider tokenProvider;

    @PostMapping("/auth/token/verify")
    public ResponseEntity<ResponseEntity<String>> getToken(@RequestBody AccessTokenRequest request) {
        String accessToken = tokenProvider.generateAccessTokenFromRefreshToken(request.get_hrauth());

        HttpCookie cookie = ResponseCookie.from("_hoauth", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ResponseEntity.ok().body(accessToken));
    }
}


