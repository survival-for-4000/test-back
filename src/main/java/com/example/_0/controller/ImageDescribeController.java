package com.example._0.controller;

import com.example._0.oauth.entity.PrincipalDetails;
import com.example._0.service.ImageDescribeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ImageDescribeController {

    private final ImageDescribeService imageDescribeService;

    @PostMapping("/describe-image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> describeImages(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("name") String name) throws IOException {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Long id = imageDescribeService.createModel(principalDetails.user(), name);

        String result;
        try {
            result = imageDescribeService.processImages(id, files);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok(result);
    }
}

