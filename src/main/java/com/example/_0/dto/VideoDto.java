package com.example._0.dto;

import com.example._0.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VideoDto {
    private String id;
    private String promptId;
    private String videoUrl;
    private String createdAt;

    public static VideoDto from(Video video) {
        return new VideoDto(
                video.getId().toString(),
                video.getPromptId(),
                video.getUrl(),
                video.getCreatedAt().toString()
        );
    }
}
