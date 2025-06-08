package com.example._0.controller;

import com.example._0.oauth.entity.PrincipalDetails;
import com.example._0.service.ModelTrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/train")
public class ModelTrainController {

    private final ModelTrainService modelTrainService;

    @GetMapping("/start/{modelId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> trainMyModel(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long modelId
    ) {

        boolean success = modelTrainService.requestFastApiTrain(String.valueOf(modelId));

        if (success) {
            return ResponseEntity.ok("Deepspeed training started");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to start Deepspeed");
        }
    }
}
