package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "news")
@Data // Lombok kütüphanesi sayesinde Get/Set metodlarını yazma ameleliğinden kurtuluyoruz
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sourceUrl;

    private String imageUrl;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String language; // "tr" veya "en"

    private String category;

    private LocalDateTime publishedDate;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}