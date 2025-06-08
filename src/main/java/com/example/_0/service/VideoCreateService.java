package com.example._0.service;

import com.example._0.entity.Member;
import com.example._0.entity.Model;
import com.example._0.entity.Video;
import com.example._0.exception.UnauthorizedAccessException;
import com.example._0.repository.ModelRepository;
import com.example._0.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VideoCreateService {

    private final RestTemplate restTemplate;
    private final ModelRepository modelRepository;
    private final VideoRepository videoRepository;

    @Value("${webClient.url}")
    private String baseUrl;

    public String startVideoJob(Member member, String prompt, Long modelId) {
        System.out.println("🎬 영상 생성 작업 시작 - 사용자: " + member.getId() + ", 모델ID: " + modelId + ", 프롬프트: '" + prompt + "'");

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    System.out.println("❌ 모델을 찾을 수 없음 - 모델ID: " + modelId);
                    return new IllegalArgumentException("해당 모델이 존재하지 않습니다.");
                });

        System.out.println("✅ 모델 조회 성공 - 모델명: " + model.getName() + ", 소유자: " + model.getMember().getId() + ", 공유여부: " + model.isShared());

        if (!model.getMember().getId().equals(member.getId()) && !model.isShared()) {
            System.out.println("🚫 권한 없음 - 요청자: " + member.getId() + ", 모델 소유자: " + model.getMember().getId() + ", 공유여부: " + model.isShared());
            throw new UnauthorizedAccessException("접근 권한이 없습니다.");
        }

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/start-video")
                .queryParam("prompt", prompt)
                .queryParam("model_name", modelId)
                .toUriString();

        System.out.println("📡 외부 API 호출 시작 - URL: " + url);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            Map body = response.getBody();

            System.out.println("📨 외부 API 응답 - 상태코드: " + response.getStatusCode() + ", 응답 바디: " + body);

            if (body == null || !body.containsKey("prompt_id")) {
                System.out.println("❌ 잘못된 응답 - prompt_id가 없음: " + body);
                throw new RuntimeException("prompt_id를 받아오지 못했습니다");
            }

            String promptId = body.get("prompt_id").toString();
            System.out.println("✅ prompt_id 획득 성공: " + promptId);

            Video video = Video.builder()
                    .url(null)
                    .prompt(prompt)
                    .model(model)
                    .taskId(promptId)
                    .member(member)
                    .build();

            Video savedVideo = videoRepository.save(video);
            System.out.println("💾 비디오 엔티티 저장 완료 - ID: " + savedVideo.getId() + ", TaskId: " + savedVideo.getTaskId());

            return promptId;

        } catch (Exception e) {
            System.out.println("💥 외부 API 호출 실패 - URL: " + url + ", 에러: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("영상 생성 작업 시작 실패: " + e.getMessage(), e);
        }
    }

    public String getVideoStatus(Member member, String taskId) {
        System.out.println("🔍 영상 상태 조회 시작 - 사용자: " + member.getId() + ", TaskId: " + taskId);

        // ✅ 1단계: DB에서 해당 taskId가 요청한 사용자의 것인지 확인
        Optional<Video> videoOpt = videoRepository.findByTaskId(taskId);
        if (videoOpt.isEmpty()) {
            System.out.println("⚠️ DB에서 작업을 찾을 수 없음 - TaskId: " + taskId);
            throw new RuntimeException("해당 작업을 찾을 수 없습니다.");
        }

        Video video = videoOpt.get();
        System.out.println("✅ DB에서 작업 조회 성공 - VideoId: " + video.getId() + ", 소유자: " + video.getMember().getId() + ", 모델: " + video.getModel().getName());

        // ✅ 2단계: 사용자 권한 확인
        if (!video.getMember().getId().equals(member.getId())) {
            System.out.println("🚫 권한 없음 - 요청자: " + member.getId() + ", 작업 소유자: " + video.getMember().getId());
            throw new UnauthorizedAccessException("접근 권한이 없습니다.");
        }

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/video-status/" + taskId)
                .toUriString();

        System.out.println("📡 외부 상태 조회 API 호출 - URL: " + url);

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map body = response.getBody();

            System.out.println("📨 상태 조회 응답 - 상태코드: " + response.getStatusCode() + ", 응답 바디: " + body);

            if (body == null || !body.containsKey("status")) {
                System.out.println("❌ 잘못된 상태 응답 - status가 없음: " + body);
                throw new RuntimeException("작업 상태를 받아오지 못했습니다.");
            }

            String status = body.get("status").toString();
            System.out.println("✅ 상태 조회 성공 - TaskId: " + taskId + ", Status: " + status);

            return status;

        } catch (Exception e) {
            System.out.println("💥 외부 상태 조회 API 실패 - URL: " + url + ", 에러: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("작업 상태 조회 실패: " + e.getMessage(), e);
        }
    }

    public ResponseEntity<String> getVideoUrl(Member member, String taskId) {
        System.out.println("📹 영상 URL 조회 시작 - 사용자: " + member.getId() + ", TaskId: " + taskId);

        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/video-result/" + taskId)
                .toUriString();

        System.out.println("📡 외부 결과 조회 API 호출 - URL: " + url);

        try {
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, String>>() {}
            );

            System.out.println("📨 결과 조회 응답 - 상태코드: " + response.getStatusCode() + ", 응답 바디: " + response.getBody());

            String publicUrl = response.getBody().get("public_url");
            if (publicUrl == null || publicUrl.trim().isEmpty()) {
                System.out.println("❌ public_url이 비어있음 - 응답: " + response.getBody());
                throw new RuntimeException("비디오 URL을 받아오지 못했습니다.");
            }

            System.out.println("✅ public_url 획득 성공: " + publicUrl);

            Video video = videoRepository.findByTaskId(taskId)
                    .orElseThrow(() -> {
                        System.out.println("❌ DB에서 비디오를 찾을 수 없음 - TaskId: " + taskId);
                        return new RuntimeException("Video not found");
                    });

            // 권한 확인
            if (!video.getMember().getId().equals(member.getId())) {
                System.out.println("🚫 URL 조회 권한 없음 - 요청자: " + member.getId() + ", 작업 소유자: " + video.getMember().getId());
                throw new UnauthorizedAccessException("접근 권한이 없습니다.");
            }

            video.setUrl(publicUrl);
            Video savedVideo = videoRepository.save(video);
            System.out.println("💾 비디오 URL 업데이트 완료 - VideoId: " + savedVideo.getId() + ", URL: " + publicUrl);

            return ResponseEntity.ok(publicUrl);

        } catch (Exception e) {
            System.out.println("💥 비디오 URL 조회 실패 - URL: " + url + ", 에러: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("비디오 URL 조회 실패: " + e.getMessage(), e);
        }
    }
}