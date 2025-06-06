package com.example._0.controller;

import com.example._0.dto.MemberDto;
import com.example._0.entity.Member;
import com.example._0.oauth.entity.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberController {

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemberDto> getUserProfile(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {;
        Member member = principalDetails.user();
        MemberDto userProfile = MemberDto.of(member);

        return ResponseEntity.ok(userProfile);
    }

}