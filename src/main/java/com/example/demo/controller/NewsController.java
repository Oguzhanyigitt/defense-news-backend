package com.example.demo.controller;

import com.example.demo.entity.News;
import com.example.demo.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
// Sadece kendi bilgisayarından ve ileride kuracağın Vercel sitesinden gelen isteklere izin ver
@CrossOrigin(origins = {"http://localhost:5173", "https://defense-news-frontend.vercel.app/"})@RequiredArgsConstructor
public class NewsController {

    private final NewsRepository newsRepository;

    // Uç Nokta 1: Tüm haberleri getirir
    // URL: http://localhost:8080/api/news
    @GetMapping
    public List<News> getAllNews() {
        return newsRepository.findAllByOrderByPublishedDateDesc();
    }


    // Seçilen kategoriye göre haberleri getirir
    // URL: http://localhost:8080/api/news/category/Yapay Zeka
    @GetMapping("/category/{category}")
    public List<News> getNewsByCategory(@PathVariable String category) {
        return newsRepository.findAllByCategoryOrderByPublishedDateDesc(category);
    }
}