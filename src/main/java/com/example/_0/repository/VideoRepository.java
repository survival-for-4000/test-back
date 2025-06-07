package com.example._0.repository;

import com.example._0.entity.Member;
import com.example._0.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByMember(Member member);
}
