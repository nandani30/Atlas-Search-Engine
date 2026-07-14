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
public class NewsCrawler {

    private final DocumentWriteQueue documentWriteQueue;
    private final Set<String> visited = new HashSet<>();
    private final Queue<String> queue = new LinkedList<>();

    private static final List<String> SEED_URLS = Arrays.asList(
            "https://www.bbc.com/news",
            "https://www.reuters.com/",
            "https://apnews.com/"
    );

    public NewsCrawler(DocumentWriteQueue documentWriteQueue) {
        this.documentWriteQueue = documentWriteQueue;
        queue.addAll(SEED_URLS);
    }

    public void crawlBatch(int batchSize) {
        System.out.println("Starting News Crawler batch...");
        int count = 0;

        while (!queue.isEmpty() && count < batchSize) {
            String url = queue.poll();
            if (visited.contains(url)) continue;
            visited.add(url);

            try {
                Thread.sleep(3000); // 3s delay to avoid 429
                org.jsoup.nodes.Document jsoupDoc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .get();

                String title = jsoupDoc.title();
                String text = jsoupDoc.body().text();

                if (!text.isEmpty()) {
                    DocumentWriteRequest req = new DocumentWriteRequest(
                            UUID.randomUUID().toString(),
                            title,
                            text,
                            "news",
                            url,
                            LocalDateTime.now()
                    );
                    documentWriteQueue.enqueue(req);
                    count++;
                    System.out.println("News Crawled: " + count + " - " + title);
                }

                Elements links = jsoupDoc.select("a[href]");
                int addedLinks = 0;
                for (Element link : links) {
                    if (addedLinks >= 5) break;
                    String nextUrl = link.absUrl("href");
                    if (nextUrl.startsWith("https://www.bbc.com/news") || nextUrl.startsWith("https://www.reuters.com/") || nextUrl.startsWith("https://apnews.com/")) {
                        if (!visited.contains(nextUrl) && !queue.contains(nextUrl)) {
                            queue.add(nextUrl);
                            addedLinks++;
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("News Crawler failed on " + url + ": " + e.getMessage());
            }
        }
        System.out.println("News Crawler batch finished. Crawled " + count + " pages.");
    }
}
