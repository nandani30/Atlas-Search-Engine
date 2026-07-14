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
            if (visited.contains(url)) continue;
            visited.add(url);

            try {
                Thread.sleep(1500); // 1.5s delay
                org.jsoup.nodes.Document jsoupDoc = Jsoup.connect(url)
                        .userAgent("AtlasSearchBot/1.0 (+http://localhost:5173)")
                        .get();

                String title = jsoupDoc.title();
                String text = jsoupDoc.body().text();

                if (!text.isEmpty()) {
                    DocumentWriteRequest req = new DocumentWriteRequest(
                            Base64.getUrlEncoder().withoutPadding().encodeToString(url.getBytes()),
                            title,
                            text,
                            "learning",
                            url,
                            LocalDateTime.now()
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
