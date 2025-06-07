package com.example._0.controller;

import com.example._0.dto.VideoDto;
import com.example._0.oauth.entity.PrincipalDetails;
import com.example._0.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/video")
public class VideoController {

    private final VideoService videoService;

    @GetMapping("/list")
    public ResponseEntity<List<VideoDto>> getMyVideos(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        List<VideoDto> videos = videoService.getVideosByMember(principalDetails.user());
        return ResponseEntity.ok(videos);
    }
}
