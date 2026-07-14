package com.atlas.searchengine.crawler;

import com.atlas.searchengine.model.Document;
import com.atlas.searchengine.model.DocumentWriteRequest;
import com.atlas.searchengine.service.DocumentWriteQueue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class LearningCrawler {

    private final DocumentWriteQueue documentWriteQueue;
    private final Set<String> visited = new HashSet<>();
    private final Queue<String> queue = new LinkedList<>();

    private static final List<String> SEED_URLS = Arrays.asList(
            "https://en.wikipedia.org/wiki/Main_Page",
            "https://en.wikibooks.org/wiki/Main_Page"
    );

    public LearningCrawler(DocumentWriteQueue documentWriteQueue) {
        this.documentWriteQueue = documentWriteQueue;
        queue.addAll(SEED_URLS);
    }

    public void crawlBatch(int batchSize) {
        System.out.println("Starting Learning Crawler batch...");
        int count = 0;

        while (!queue.isEmpty() && count < batchSize) {
            String url = queue.poll();
            if (url == null) continue;
            
            // Clean URL: Remove hash fragments and query parameters
            int hashIndex = url.indexOf('#');
            if (hashIndex != -1) url = url.substring(0, hashIndex);
            int queryIndex = url.indexOf('?');
            if (queryIndex != -1) url = url.substring(0, queryIndex);
            
            if (visited.contains(url)) continue;
            visited.add(url);

            try {
                Thread.sleep(1500); // 1.5s delay
                org.jsoup.nodes.Document jsoupDoc = Jsoup.connect(url)
                        .userAgent("AtlasSearchBot/1.0 (+http://localhost:5173)")
                        .get();

                String title = jsoupDoc.title();
                String text = jsoupDoc.body().text();

                LocalDateTime publishedAt = LocalDateTime.now(); // default to now
                try {
                    Element metaPublishedTime = jsoupDoc.selectFirst("meta[property=article:published_time], meta[name=pubdate], meta[name=publishdate]");
                    if (metaPublishedTime != null && metaPublishedTime.hasAttr("content")) {
                        String content = metaPublishedTime.attr("content");
                        if (content.length() >= 19) {
                            publishedAt = LocalDateTime.parse(content.substring(0, 19));
                        }
                    } else {
                        Element timeEl = jsoupDoc.selectFirst("time[datetime]");
                        if (timeEl != null) {
                            String datetime = timeEl.attr("datetime");
                            if (datetime.length() >= 19) {
                                publishedAt = LocalDateTime.parse(datetime.substring(0, 19));
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not parse date for " + url + ": " + e.getMessage());
                }

                if (!text.isEmpty()) {
                    DocumentWriteRequest req = new DocumentWriteRequest(
                            Base64.getUrlEncoder().withoutPadding().encodeToString(url.getBytes()),
                            title,
                            text,
                            "learning",
                            url,
                            LocalDateTime.now(),
                            publishedAt
                    );
                    documentWriteQueue.enqueue(req);
                    count++;
                    System.out.println("Learning Crawled: " + count + " - " + title);
                }

                Elements links = jsoupDoc.select("a[href]");
                int addedLinks = 0;
                for (Element link : links) {
                    if (addedLinks >= 5) break;
                    String nextUrl = link.absUrl("href");
                    if (nextUrl.startsWith("https://en.wikipedia.org/wiki/") || nextUrl.startsWith("https://en.wikibooks.org/wiki/")) {
                        if (!visited.contains(nextUrl) && !queue.contains(nextUrl)) {
                            queue.add(nextUrl);
                            addedLinks++;
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("Learning Crawler failed on " + url + ": " + e.getMessage());
            }
        }
        System.out.println("Learning Crawler batch finished. Crawled " + count + " pages.");
    }
}
