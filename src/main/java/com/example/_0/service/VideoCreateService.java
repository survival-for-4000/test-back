package com.example._0.service;

import com.example._0.entity.Member;
import com.example._0.entity.Model;
import com.example._0.exception.UnauthorizedAccessException;
import com.example._0.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    public ResponseEntity<byte[]> downloadVideoFile(String promptId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/video-result/" + promptId)
                .toUriString();

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    byte[].class
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment().filename("video.mp4").build());

            return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new RuntimeException("비디오 다운로드 실패: " + e.getMessage(), e);
        }
    }




}

