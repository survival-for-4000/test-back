package com.example._0.service;

import com.example._0.entity.Member;
import com.example._0.entity.Model;
import com.example._0.repository.ModelRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example._0.entity.Status.NOT_STARTED;

@Service
@RequiredArgsConstructor
@Transactional
public class ImageDescribeService {

    @Value("${webClient.url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ModelRepository modelRepository;

    private String videoBaseUrl;

    @PostConstruct
    public void init() {
        this.videoBaseUrl = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/8005")
                .toUriString();  // ← 여기가 중요!
    }

    public String processImages(Long id, List<MultipartFile> files) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (MultipartFile file : files) {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("files", resource);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String fullUrl = videoBaseUrl + "/describe-images?model_id=" + id;
        ResponseEntity<String> response = restTemplate.postForEntity(fullUrl, requestEntity, String.class);
        requestFastApiTrain(String.valueOf(id));


        return response.getBody();
    }

    public Long createModel(Member member, String name) {
        Model model = Model.builder()
                .name(name)
                .member(member)
                .shared(false)
                .status(NOT_STARTED)
                .build();
        modelRepository.save(model);
        return model.getId();
    }

    public boolean requestFastApiTrain(String modelName) {
        try {
            String fastApiUrl = videoBaseUrl + "/run-deepspeed";

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
