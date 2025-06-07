package com.example._0.service;

import com.example._0.entity.Member;
import com.example._0.entity.Model;
import com.example._0.exception.UnauthorizedAccessException;
import com.example._0.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class VideoCreateService {

    private final RestTemplate restTemplate;
    private final ModelRepository modelRepository;

    @Value("${webClient.url}")
    private String baseUrl;


    public Map fetchVideoResult(Member member, String prompt, Long modelId) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("해당 모델이 존재하지 않습니다."));

        if (!model.getMember().getId().equals(member.getId()) && !model.isShared()) {
            throw new UnauthorizedAccessException("접근 권한이 없습니다.");
        }

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/get-video")
                .queryParam("prompt", prompt)
                .queryParam("model_name", modelId)
                .toUriString();

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return response.getBody();
    }

}

