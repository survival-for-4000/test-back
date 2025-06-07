package com.example._0.controller;

import com.example._0.oauth.entity.PrincipalDetails;
import com.example._0.service.VideoCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
            @RequestBody Map<String, String> requestBody  // JSON body로 받기
    ) {
        String prompt = requestBody.get("prompt");
        String model_name = requestBody.get("model_name");

        System.out.println("=== Video Generation Request Received ===");
        System.out.println("User ID: " + principalDetails.user().getId());
        System.out.println("User Email: " + principalDetails.user().getEmail());
        System.out.println("Prompt: " + prompt);
        System.out.println("Model Name: " + model_name);
        System.out.println("Request Time: " + LocalDateTime.now());
        System.out.println("==========================================");

        try {
            Map result = videoCreateService.fetchVideoResult(
                    prompt,
                    String.valueOf(principalDetails.user().getId()),
                    model_name
            );

            System.out.println("Video generation service completed successfully");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.out.println("Error occurred during video generation: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}

