package com.example._0.controller;

import com.example._0.oauth.entity.PrincipalDetails;
import com.example._0.service.VideoCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VideoCreateController {

    private final VideoCreateService videoCreateService;

    @GetMapping("/video-result")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map> getVideoResult(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam Long id,
            @RequestParam String prompt
    ) {

        Map result = videoCreateService.fetchVideoResult(
                principalDetails.user(),
                prompt,
                id
        );
        return ResponseEntity.ok(result);
    }
}

