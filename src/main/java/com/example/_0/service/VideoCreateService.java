package com.example._0.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.util.UriComponentsBuilder;


import java.util.Map;

@Service
@RequiredArgsConstructor
public class VideoCreateService {

    private final RestTemplate restTemplate;

    @Value("${webClient.url}")
    private String baseUrl;

    public Map fetchVideoResult(String prompt, String userId, String modelName) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/get-video")
                .queryParam("prompt", prompt)
                .queryParam("user_id", userId)
                .queryParam("model_name", modelName)
                .toUriString();

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return response.getBody();
    }
}

