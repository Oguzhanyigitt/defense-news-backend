package com.example.demo.scheduler;

import com.example.demo.repository.NewsRepository;
import com.example.demo.service.RssCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScheduler {

    private final RssCrawlerService rssCrawlerService;
    private final NewsRepository newsRepository; // Temizlik için Repository'yi dahil ettik

    // HER SAAT BAŞI ÇALIŞAN HABER TOPLAYICI
    @Scheduled(initialDelay = 10000, fixedRate = 3600000)
    public void scheduleNewsFetching() {
        log.info("Zamanlanmış görev tetiklendi: Haberler çekiliyor...");
        rssCrawlerService.fetchNewsFromFeeds();
    }

    // HER GECE YARISI ÇALIŞAN TEMİZLİK MOTORU (Otonom Bakım)
    // cron = "Saniye Dakika Saat Gün Ay HaftanınGünü" (0 0 0 * * ? = Her gece 00:00:00)
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanOldNews() {
        log.info("Otonom veritabanı bakımı başlatıldı: 7 günden eski haberler siliniyor...");

        // Şu anki zamandan tam 7 gün öncesini hesapla
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // 7 günden daha eski olan tüm kayıtları Supabase'den kalıcı olarak sil
        newsRepository.deleteByPublishedDateBefore(sevenDaysAgo);

        log.info("Veritabanı temizliği başarıyla tamamlandı.");
    }
}