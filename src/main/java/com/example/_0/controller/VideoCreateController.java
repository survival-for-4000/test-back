package com.example._0.controller;

import com.example._0.oauth.entity.PrincipalDetails;
import com.example._0.service.VideoCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VideoCreateController {

    private final VideoCreateService videoCreateService;

    @PostMapping("/video-result")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map> getVideoResult(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam Long id,
            @RequestParam String prompt
    ) {

        // === 요청 정보 출력 ===
        System.out.println("=== Video Generation Request Received ===");
        System.out.println("Request Time: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("User ID: " + principalDetails.user().getId());
        System.out.println("User Email: " + principalDetails.user().getEmail());
        System.out.println("Model ID: " + id);
        System.out.println("Prompt: " + prompt);
        System.out.println("==========================================");

        try {
            Map result = videoCreateService.fetchVideoResult(
                    principalDetails.user(),
                    prompt,
                    id
            );

            System.out.println("✅ Video generation request successful");
            System.out.println("Response: " + result);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.out.println("❌ Error occurred during video generation: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}

