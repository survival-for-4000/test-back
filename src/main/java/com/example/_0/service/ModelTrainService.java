package com.example._0.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModelTrainService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${webClient.url}")
    private String baseUrl;

    public boolean requestFastApiTrain(String modelName) {
        try {
            String fastApiUrl = baseUrl +"/8005" + "/run-deepspeed";

            Map<String, String> payload = new HashMap<>();
            payload.put("model_name", modelName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(fastApiUrl, entity, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

