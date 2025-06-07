package com.example._0.controller;

import com.example._0.oauth.entity.PrincipalDetails;
import com.example._0.service.VideoCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VideoCreateController {

    private final VideoCreateService videoCreateService;

    @PostMapping("/video/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> startVideo(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam Long id,
            @RequestParam String prompt
    ) {
        String promptId = videoCreateService.startVideoJob(
                principalDetails.user(), prompt, id
        );
        return ResponseEntity.ok(Map.of("promptId", promptId));
    }

    @GetMapping("/video/status/{promptId}")
    public ResponseEntity<Map<String, String>> checkVideoStatus(@PathVariable String promptId) {
        String status = videoCreateService.getVideoStatus(promptId);
        return ResponseEntity.ok(Map.of("status", status));
    }

    @GetMapping("/video/result/{promptId}")
    public ResponseEntity<byte[]> downloadVideo(@PathVariable String promptId) {
        return videoCreateService.downloadVideoFile(promptId);
    }


}

