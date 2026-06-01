package com.example.demo.service;

import com.example.demo.entity.News;
import com.example.demo.repository.NewsRepository;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Element;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssCrawlerService {

    private final NewsRepository newsRepository;

    // 3 Kategoriye Ait Zengin Haber Kaynakları
    private final List<String> rssFeeds = List.of(
            // Savunma Sanayi
            "https://www.defensenews.com/arc/outboundfeeds/rss/",
            "https://www.savunmasanayist.com/feed/",
            // Yapay Zeka
            "https://techcrunch.com/category/artificial-intelligence/feed/",
            "https://artificialintelligence-news.com/feed/",
            // Teknoloji
            "https://www.theverge.com/tech/rss/index.xml",
            "https://shiftdelete.net/feed"
    );

    public void fetchNewsFromFeeds() {
        log.info("Proxy Zırhlı Haber & Web Kazıma motoru çalıştırıldı...");

        for (String feedUrl : rssFeeds) {
            try {
                // CLOUDFLARE BYPASS: Sunucu IP'mizi gizlemek için AllOrigins Proxy kullanıyoruz
                String encodedUrl = URLEncoder.encode(feedUrl, StandardCharsets.UTF_8.toString());
                String bypassUrl = "https://api.allorigins.win/raw?url=" + encodedUrl;

                URL url = new URL(bypassUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Kimlik Gizleme (Spoofing) Başlıkları
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                connection.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*");
                connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9,tr;q=0.8");

                // Zaman Aşımı (Timeout) Toleransları
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(connection.getInputStream()));
                int savedCount = 0;

                for (SyndEntry entry : feed.getEntries()) {
                    if (!newsRepository.existsBySourceUrl(entry.getLink())) {

                        News news = new News();
                        news.setTitle(entry.getTitle());
                        news.setSourceUrl(entry.getLink());

                        if (entry.getDescription() != null) {
                            news.setSummary(entry.getDescription().getValue());
                        }

                        // Gelişmiş Görsel Ayıklama Motoru (Proxy destekli)
                        news.setImageUrl(extractImageUrl(entry, entry.getLink()));

                        // Gizli/Boş Tarih (Null) Tuzağını Engelleme
                        Date publishedDate = entry.getPublishedDate();
                        if (publishedDate != null) {
                            news.setPublishedDate(publishedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                        } else {
                            news.setPublishedDate(LocalDateTime.now());
                        }

                        // Kategori ve Dil Etiketleme Mantığı
                        if (feedUrl.contains("artificial-intelligence") || feedUrl.contains("artificialintelligence")) {
                            news.setCategory("Yapay Zeka");
                            news.setLanguage("en");
                        } else if (feedUrl.contains("techcrunch") || feedUrl.contains("verge") || feedUrl.contains("shiftdelete")) {
                            news.setCategory("Teknoloji");
                            news.setLanguage(feedUrl.contains("shiftdelete") ? "tr" : "en");
                        } else {
                            news.setCategory("Savunma Sanayi");
                            news.setLanguage(feedUrl.contains("savunmasanayist") ? "tr" : "en");
                        }

                        newsRepository.save(news);
                        savedCount++;
                    }
                }
                log.info("Kaynaktan {} yeni haber başarıyla çekildi: {}", savedCount, feedUrl);

            } catch (Exception e) {
                log.error("Haberler çekilirken bir hata oluştu. Kaynak: {} Hata: {}", feedUrl, e.getMessage());
            }
        }
    }

    private String extractImageUrl(SyndEntry entry, String articleUrl) {
        // İhtimal 1: Standart <enclosure>
        if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
            return entry.getEnclosures().get(0).getUrl();
        }

        // İhtimal 2: <media:content> veya <media:thumbnail>
        if (entry.getForeignMarkup() != null) {
            for (Element element : entry.getForeignMarkup()) {
                if ("content".equals(element.getName()) || "thumbnail".equals(element.getName())) {
                    String url = element.getAttributeValue("url");
                    if (url != null) return url;
                }
            }
        }

        // İhtimal 3: WordPress stili <content:encoded> içine gömülü resimler
        Pattern pattern = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
        if (entry.getContents() != null && !entry.getContents().isEmpty()) {
            for (SyndContent content : entry.getContents()) {
                if (content.getValue() != null) {
                    Matcher matcher = pattern.matcher(content.getValue());
                    if (matcher.find()) return matcher.group(1);
                }
            }
        }

        // İhtimal 4: Açıklama (<description>) içine gömülmüş HTML <img>
        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            Matcher matcher = pattern.matcher(entry.getDescription().getValue());
            if (matcher.find()) return matcher.group(1);
        }

        // NÜKLEER SEÇENEK: JSOUP İLE WEB KAZIMA (Cloudflare engeline takılmamak için Proxy Üzerinden)
        try {
            String encodedArticleUrl = URLEncoder.encode(articleUrl, StandardCharsets.UTF_8.toString());
            String proxyArticleUrl = "https://api.allorigins.win/raw?url=" + encodedArticleUrl;

            Document doc = Jsoup.connect(proxyArticleUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();
            org.jsoup.nodes.Element ogImage = doc.select("meta[property=og:image]").first();
            if (ogImage != null) {
                return ogImage.attr("content");
            }
        } catch (Exception e) {
            log.warn("Web scraping başarısız oldu (Timeout veya Engel): {}", articleUrl);
        }

        // En Kötü Senaryo: Güvenlik Ağı (Yer Tutucu Görsel)
        return "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=800&auto=format&fit=crop";
    }
}