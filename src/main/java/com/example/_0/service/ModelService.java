package com.example._0.service;

import com.example._0.dto.ModelDto;
import com.example._0.entity.Member;
import com.example._0.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelService {
    private final ModelRepository modelRepository;

    public List<ModelDto> getMyModels(Member member) {
        return modelRepository.findByMember(member)
                .stream()
                .map(ModelDto::of)
                .toList();
    }

    public List<ModelDto> getSharedModels() {
        return modelRepository.findBySharedIsTrue()
                .stream()
                .map(ModelDto::of)
                .toList();
    }
}
