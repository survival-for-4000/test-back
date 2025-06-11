package com.example._0.dto;

import com.example._0.entity.Model;
import com.example._0.entity.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelDto {
    private long id;
    private String name;
    private LocalDateTime createdAt;
    private Status status;

    public static ModelDto of(Model model) {
        return ModelDto.builder()
                .id(model.getId())
                .name(model.getName())
                .createdAt(model.getCreatedAt())
                .status(model.getStatus())
                .build();
    }
}