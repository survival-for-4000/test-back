package com.example._0.dto;

import com.example._0.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDto {
    private long id;
    private String nickname;
    private String profileImage;

    public static MemberDto of(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .nickname(member.getName())
                .profileImage(member.getProfile())
                .build();
    }
}