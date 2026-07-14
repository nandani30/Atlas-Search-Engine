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
public class ScienceCrawler {

    private final DocumentWriteQueue documentWriteQueue;
    private final Set<String> visited = new HashSet<>();
    private final Queue<String> queue = new LinkedList<>();

    private static final List<String> SEED_URLS = Arrays.asList(
            "https://www.nasa.gov/",
            "https://home.cern/",
            "https://www.esa.int/",
            "https://www.nature.com/",
            "https://www.sciencedaily.com/",
            "https://www.newscientist.com/"
    );

    public ScienceCrawler(DocumentWriteQueue documentWriteQueue) {
        this.documentWriteQueue = documentWriteQueue;
        queue.addAll(SEED_URLS);
    }

    public void crawlBatch(int batchSize) {
        System.out.println("Starting Science Crawler batch...");
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

            boolean success = false;
            int retries = 0;
            int maxRetries = 3;
            long backoff = 2000;

            while (!success && retries < maxRetries) {
                try {
                    Thread.sleep(backoff); // Initial 2s delay, then exponential
                    org.jsoup.nodes.Document jsoupDoc = Jsoup.connect(url)
                            .userAgent("AtlasSearchBot/1.0 (+http://localhost:5173)")
                            .timeout(10000) // 10s timeout for resilience
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
                                "science",
                                url,
                                LocalDateTime.now(),
                                publishedAt
                        );
                        documentWriteQueue.enqueue(req);
                        count++;
                        System.out.println("Science Crawled: " + count + " - " + title);
                    }

                    Elements links = jsoupDoc.select("a[href]");
                    int addedLinks = 0;
                    for (Element link : links) {
                        if (addedLinks >= 5) break;
                        String nextUrl = link.absUrl("href");
                        
                        boolean isAllowed = false;
                        for (String seed : SEED_URLS) {
                            if (nextUrl.startsWith(seed)) {
                                isAllowed = true;
                                break;
                            }
                        }
                        
                        if (isAllowed) {
                            if (!visited.contains(nextUrl) && !queue.contains(nextUrl)) {
                                queue.add(nextUrl);
                                addedLinks++;
                            }
                        }
                    }
                    success = true;

                } catch (Exception e) {
                    retries++;
                    System.err.println("Science Crawler failed on " + url + " (Retry " + retries + "): " + e.getMessage());
                    backoff *= 2; // Exponential backoff
                }
            }
            if (!success) {
                System.err.println("Science Crawler skipping " + url + " after " + maxRetries + " failures.");
            }
        }
        System.out.println("Science Crawler batch finished. Crawled " + count + " pages.");
    }
}
