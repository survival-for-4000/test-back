package com.example._0.controller;


import com.example._0.dto.ModelDto;
import com.example._0.entity.Member;
import com.example._0.oauth.entity.PrincipalDetails;
import com.example._0.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ModelController {
    private final ModelService modelService;

    @GetMapping("/my-models")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ModelDto>> getMyModels(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Member member = principalDetails.user();
        List<ModelDto> modelDtoList = modelService.getMyModels(member);

        return ResponseEntity.ok(modelDtoList);
    }

    @GetMapping("/shared-models")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ModelDto>> getSharedModels(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        List<ModelDto> modelDtoList = modelService.getSharedModels();

        return ResponseEntity.ok(modelDtoList);
    }
}
