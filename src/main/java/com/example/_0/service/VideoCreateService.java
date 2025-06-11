package com.example._0.service;

import com.example._0.entity.Member;
import com.example._0.entity.Model;
import com.example._0.entity.Status;
import com.example._0.entity.Video;
import com.example._0.exception.UnauthorizedAccessException;
import com.example._0.repository.ModelRepository;
import com.example._0.repository.VideoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VideoCreateService {

    private final RestTemplate restTemplate;
    private final ModelRepository modelRepository;
    private final VideoRepository videoRepository;

    @Value("${webClient.url}")
    private String baseUrl;

    private String videoBaseUrl;

    @PostConstruct
    public void init() {
        this.videoBaseUrl = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/8000")
                .toUriString();  // ← 여기가 중요!
    }

    public String startVideoJob(Member member, String prompt, Long modelId) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    throw new IllegalArgumentException("해당 모델이 존재하지 않습니다.");
                });

        if (!model.getMember().getId().equals(member.getId()) && !model.isShared()) {
            throw new UnauthorizedAccessException("접근 권한이 없습니다.");
        }

        String url = UriComponentsBuilder.fromHttpUrl(videoBaseUrl + "/start-video")
                .queryParam("prompt", prompt)
                .queryParam("model_name", modelId)
                .toUriString();

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            Map body = response.getBody();

            if (body == null || !body.containsKey("prompt_id")) {
                throw new RuntimeException("prompt_id를 받아오지 못했습니다");
            }

            String promptId = body.get("prompt_id").toString();

            Video video = Video.builder()
                    .url(null)
                    .prompt(prompt)
                    .model(model)
                    .taskId(promptId)
                    .member(member)
                    .status(Status.TRAINING)
                    .build();

            videoRepository.save(video);

            checkVideoStatusAsync(member, promptId);

            return promptId;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("영상 생성 작업 시작 실패: " + e.getMessage(), e);
        }
    }

    @Async
    public void checkVideoStatusAsync(Member member, String promptId) {
        String url = UriComponentsBuilder.fromHttpUrl(videoBaseUrl + "/video-status/" + promptId).toUriString();

        try {
            int retry = 0;
            int maxRetries = 50;
            while (retry < maxRetries) {
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                String status = response.getBody().get("status").toString();

                if ("done".equalsIgnoreCase(status)) {
                    getVideoUrl(member, promptId);
                    return;
                } else if ("failed".equalsIgnoreCase(status)) {
                    Video video = videoRepository.findByTaskId(promptId).orElseThrow();
                    video.setStatus(Status.FAILED);
                    videoRepository.save(video);
                    return;
                }

                retry++;
                Thread.sleep(3000); // 1초 대기
            }

        } catch (Exception e) {
            log.error("비동기 상태 체크 실패", e);
        }
    }


    public String getVideoStatus(Member member, String taskId) {

        System.out.println("🔍 영상 상태 조회 시작 - 사용자: " + member.getId() + ", TaskId: " + taskId);

        Optional<Video> videoOpt = videoRepository.findByTaskId(taskId);
        if (videoOpt.isEmpty()) {
            throw new RuntimeException("해당 작업을 찾을 수 없습니다.");
        }

        Video video = videoOpt.get();
        if (!video.getMember().getId().equals(member.getId())) {
            throw new UnauthorizedAccessException("접근 권한이 없습니다.");
        }

        if (video.getUrl() != null && !video.getUrl().trim().isEmpty()) {
            return "done";
        }

        String url = UriComponentsBuilder.fromHttpUrl(videoBaseUrl + "/video-status/" + taskId)
                .toUriString();

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map body = response.getBody();

            if (body == null || !body.containsKey("status")) {
                throw new RuntimeException("작업 상태를 받아오지 못했습니다.");
            }

            String status = body.get("status").toString();
            if (status == "done"){
                getVideoUrl(member, taskId);
            }

            return status;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("작업 상태 조회 실패: " + e.getMessage(), e);
        }
    }

    public ResponseEntity<String> getVideoUrl(Member member, String taskId) {

        String url = UriComponentsBuilder
                .fromHttpUrl(videoBaseUrl   + "/video-result/" + taskId)
                .toUriString();

        try {
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, String>>() {}
            );

            String publicUrl = response.getBody().get("public_url");
            if (publicUrl == null || publicUrl.trim().isEmpty()) {
                Video video = videoRepository.findByTaskId(taskId).orElseThrow();
                video.setStatus(Status.FAILED);
                videoRepository.save(video);
                throw new RuntimeException("비디오 URL을 받아오지 못했습니다.");
            }

            Video video = videoRepository.findByTaskId(taskId)
                    .orElseThrow(() -> {
                        throw new RuntimeException("Video not found");
                    });

            if (!video.getMember().getId().equals(member.getId())) {
                throw new UnauthorizedAccessException("접근 권한이 없습니다.");
            }

            video.setUrl(publicUrl);
            video.setStatus(Status.SUCCEEDED);
            videoRepository.save(video);

            return ResponseEntity.ok(publicUrl);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("비디오 URL 조회 실패: " + e.getMessage(), e);
        }
    }
}