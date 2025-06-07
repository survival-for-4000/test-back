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
                .map(model -> new ModelDto(model.getId(), model.getName(), model.getCreatedAt()))
                .toList();
    }

    public List<ModelDto> getSharedModels() {
        return modelRepository.findBySharedIsTrue()
                .stream()
                .map(model -> new ModelDto(model.getId(), model.getName(), model.getCreatedAt()))
                .toList();
    }
}
