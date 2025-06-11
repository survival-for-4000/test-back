package com.example._0.dto;

import com.example._0.entity.Status;
import com.example._0.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VideoDto {
    private String id;
    private String prompt;
    private ModelDto model;
    private String taskId;
    private String videoUrl;
    private Status status;
    private String createdAt;

    public static VideoDto from(Video video) {
        return new VideoDto(
                video.getId().toString(),
                video.getPrompt(),
                video.getModel() != null ? ModelDto.of(video.getModel()) : null, // null 체크 추가
                video.getTaskId(),
                video.getUrl(),
                video.getStatus(),
                video.getCreatedAt().toString()
        );
    }
}
