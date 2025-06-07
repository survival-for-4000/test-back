package com.example._0.repository;

import com.example._0.entity.Member;
import com.example._0.entity.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {
    List<Model> findByMember(Member member);
    List<Model> findBySharedIsTrue();
}
