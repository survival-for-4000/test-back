package com.example._0.service;

import com.example._0.dto.VideoDto;
import com.example._0.entity.Member;
import com.example._0.entity.Video;
import com.example._0.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;

    public List<VideoDto> getVideosByMember(Member member) {
        List<Video> videos = videoRepository.findByMember(member);
        return videos.stream()
                .map(VideoDto::from)
                .toList();
    }

    public List<VideoDto> getVideosByMemberAndModels(Member member, Long modelId) {
        List<Video> videos = videoRepository.findByMemberAndModelId(member, modelId);
        return videos.stream()
                .map(VideoDto::from)
                .toList();
    }

}
