package com.example.demo.repository;

import com.example.demo.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    boolean existsBySourceUrl(String sourceUrl);

    List<News> findAllByOrderByPublishedDateDesc();
    List<News> findAllByCategoryOrderByPublishedDateDesc(String category);

    List<News> findAllByLanguageOrderByPublishedDateDesc(String language);

    // 7 GÜNLÜK TEMİZLİK İÇİN YENİ METOD
    @Transactional
    @Modifying
    void deleteByPublishedDateBefore(LocalDateTime expiryDate);
}