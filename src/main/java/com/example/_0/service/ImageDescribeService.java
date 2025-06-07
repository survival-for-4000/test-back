package com.example._0.service;

import com.example._0.entity.Member;
import com.example._0.entity.Model;
import com.example._0.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageDescribeService {

    @Value("${webClient.url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ModelRepository modelRepository;

    public String processImages(List<MultipartFile> files) throws IOException {
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

        String fullUrl = baseUrl + "/describe-images";
        ResponseEntity<String> response = restTemplate.postForEntity(fullUrl, requestEntity, String.class);

        return response.getBody();
    }

    public Model createModel(Member member, String name) {
        Model model = Model.builder()
                .name(name)
                .member(member)
                .build();
        return modelRepository.save(model);
    }
}
