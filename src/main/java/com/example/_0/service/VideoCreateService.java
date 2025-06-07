package com.example._0.service;

import com.example._0.entity.Member;
import com.example._0.entity.Model;
import com.example._0.entity.Video;
import com.example._0.exception.UnauthorizedAccessException;
import com.example._0.repository.ModelRepository;
import com.example._0.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class VideoCreateService {

    private final RestTemplate restTemplate;
    private final ModelRepository modelRepository;
    private final VideoRepository videoRepository;

    @Value("${webClient.url}")
    private String baseUrl;


    public String startVideoJob(Member member, String prompt, Long modelId) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("해당 모델이 존재하지 않습니다."));

        if (!model.getMember().getId().equals(member.getId()) && !model.isShared()) {
            throw new UnauthorizedAccessException("접근 권한이 없습니다.");
        }

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/start-video")
                .queryParam("prompt", prompt)
                .queryParam("model_name", modelId)
                .toUriString();

        ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
        Map body = response.getBody();

        if (body == null || !body.containsKey("prompt_id")) {
            throw new RuntimeException("prompt_id를 받아오지 못했습니다");
        }

        return body.get("prompt_id").toString();
    }

    public String getVideoStatus(String promptId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/video-status/" + promptId)
                .toUriString();

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map body = response.getBody();

        if (body == null || !body.containsKey("status")) {
            throw new RuntimeException("작업 상태를 받아오지 못했습니다.");
        }

        return body.get("status").toString(); // e.g. "pending", "running", "done", etc.
    }

    public ResponseEntity<String> getVideoUrl(Member member, String promptId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/video-result/" + promptId)
                .toUriString();

        try {
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, String>>() {}
            );

            String publicUrl = (String) response.getBody().get("public_url");
            Video video = Video.builder()
                    .url(publicUrl)
                    .promptId(promptId)
                    .member(member)
                    .build();

            videoRepository.save(video);


            return ResponseEntity.ok(publicUrl);
        } catch (Exception e) {
            throw new RuntimeException("비디오 URL 조회 실패: " + e.getMessage(), e);
        }
    }





}

