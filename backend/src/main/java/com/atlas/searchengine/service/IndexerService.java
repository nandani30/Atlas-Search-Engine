package com.atlas.searchengine.service;

import com.atlas.searchengine.model.CollectionIndexData;
import com.atlas.searchengine.model.Document;
import com.atlas.searchengine.model.PagedResult;
import com.atlas.searchengine.model.SearchResult;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class IndexerService {
    private final BM25Ranker bm25Ranker;
    private final CacheManager cacheManager;

    private final Map<String, CollectionIndexData> collections = new ConcurrentHashMap<>();

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with"
    );

    private static final Pattern PUNCTUATION = Pattern.compile("[^a-z0-9\\s]");

    public IndexerService(BM25Ranker bm25Ranker, CacheManager cacheManager) {
        this.bm25Ranker = bm25Ranker;
        this.cacheManager = cacheManager;
    }

    public void indexDocument(Document doc) {
        // Initial indexing path used by FallbackDataLoader / Startup
        String col = doc.getCollection();
        collections.putIfAbsent(col, new CollectionIndexData(
                new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new AutocompleteTrie()
        ));
        indexDocIntoData(doc, collections.get(col));
    }

    private void indexDocIntoData(Document doc, CollectionIndexData data) {
        String docId = doc.getId();
        if (docId == null || data.documents().containsKey(docId)) return;

        data.documents().put(docId, doc);

        List<String> titleTokens = tokenize(doc.getTitle());
        List<String> textTokens = tokenize(doc.getText());
        
        int docLength = titleTokens.size() + textTokens.size();
        data.documentLengths().put(docId, docLength);

        Map<String, Integer> termFrequencies = new HashMap<>();
        for (String token : titleTokens) {
            // Give title words a massive 15x weight boost for better search relevance
            termFrequencies.put(token, termFrequencies.getOrDefault(token, 0) + 15);
            data.trie().insert(token);
        }
        for (String token : textTokens) {
            termFrequencies.put(token, termFrequencies.getOrDefault(token, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
            String term = entry.getKey();
            int tf = entry.getValue();
            
            data.invertedIndex().putIfAbsent(term, new ConcurrentHashMap<>());
            data.invertedIndex().get(term).put(docId, tf);
        }
        
        String cleanTitle = doc.getTitle().toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
        data.trie().insert(cleanTitle);
        
        // At the very end, truncate the text payload permanently to save RAM on the 512MB Render free tier
        String originalText = doc.getText();
        if (originalText != null && originalText.length() > 500) {
            doc.setText(originalText.substring(0, 500) + "...");
        }
    }

    public void rebuildCollectionFromDB(String collection, com.atlas.searchengine.model.DocumentRepository repo) {
        System.out.println("Starting background rebuild for collection: " + collection);
        CollectionIndexData newData = new CollectionIndexData(
                new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new AutocompleteTrie()
        );

        // Limit to 50 documents per collection to prevent memory exhaustion on 512MB Render free tier
        org.springframework.data.domain.Page<Document> docPage = repo.findByCollection(
                collection, 
                org.springframework.data.domain.PageRequest.of(0, 50, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "crawledAt"))
        );
        
        for (Document doc : docPage.getContent()) {
            indexDocIntoData(doc, newData);
        }

        // Atomic swap
        collections.put(collection, newData);
        System.out.println("Finished rebuild for collection: " + collection);

        // Invalidate cache
        var cache = cacheManager.getCache("searchResults");
        if (cache != null) {
            cache.clear();
            System.out.println("Cleared searchResults cache due to collection rebuild.");
        }
    }

    public List<String> tokenize(String text) {
        if (text == null) return Collections.emptyList();
        String lowerText = text.toLowerCase();
        String noPunctuation = PUNCTUATION.matcher(lowerText).replaceAll("");
        String[] words = noPunctuation.split("\\s+");
        
        return Arrays.stream(words)
                .filter(w -> !w.isEmpty())
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "searchResults", key = "{#query, #collection, #page, #pageSize}")
    public PagedResult<SearchResult> search(String query, String collection, int page, int pageSize) {
        System.out.println("Executing search for query: '" + query + "' (cache miss)");
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) return new PagedResult<>(Collections.emptyList(), 0, 0);

        List<String> targetCollections;
        if ("all".equalsIgnoreCase(collection) || collection == null || collection.isEmpty()) {
            targetCollections = new ArrayList<>(collections.keySet());
        } else {
            targetCollections = List.of(collection);
        }

        Map<String, Double> docScores = new HashMap<>();
        Map<String, Document> matchedDocs = new HashMap<>();

        for (String col : targetCollections) {
            CollectionIndexData data = collections.get(col);
            if (data == null) continue;

            int totalDocs = data.documentLengths().size();
            double avgDocLength = data.documentLengths().values().stream().mapToInt(Integer::intValue).average().orElse(1.0);

            for (String token : queryTokens) {
                Map<String, Integer> docsWithTerm = data.invertedIndex().get(token);
                if (docsWithTerm == null) continue;

                int docCountWithTerm = docsWithTerm.size();

                for (Map.Entry<String, Integer> entry : docsWithTerm.entrySet()) {
                    String docId = entry.getKey();
                    int tf = entry.getValue();
                    int docLength = data.documentLengths().getOrDefault(docId, 1);

                    double score = bm25Ranker.scoreTerm(tf, docLength, avgDocLength, docCountWithTerm, totalDocs);
                    
                    String globalDocId = col + ":" + docId;
                    docScores.put(globalDocId, docScores.getOrDefault(globalDocId, 0.0) + score);
                    matchedDocs.put(globalDocId, data.documents().get(docId));
                }
            }
        }

        List<SearchResult> allResults = docScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> {
                    String globalId = entry.getKey();
                    Document doc = matchedDocs.get(globalId);
                    String snippet = generateSnippet(doc.getText(), queryTokens);
                    return new SearchResult(doc.getId(), doc.getSourceUrl(), doc.getTitle(), snippet, entry.getValue(), doc.getCollection());
                })
                .collect(Collectors.toList());

        long totalElements = allResults.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        
        int fromIndex = Math.min(page * pageSize, allResults.size());
        int toIndex = Math.min(fromIndex + pageSize, allResults.size());
        List<SearchResult> pagedResults = allResults.subList(fromIndex, toIndex);

        return new PagedResult<>(pagedResults, totalElements, totalPages);
    }

    private String generateSnippet(String text, List<String> queryTokens) {
        if (text == null || text.isEmpty()) return "";
        
        String lowerText = text.toLowerCase();
        int bestIdx = -1;
        for (String token : queryTokens) {
            int idx = lowerText.indexOf(token);
            if (idx != -1) {
                bestIdx = idx;
                break;
            }
        }
        
        if (bestIdx == -1) {
            return text.substring(0, Math.min(text.length(), 150)) + "...";
        }
        
        int start = Math.max(0, bestIdx - 75);
        int end = Math.min(text.length(), bestIdx + 75);
        
        String snippet = text.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < text.length()) snippet = snippet + "...";
        
        return snippet;
    }

    public List<String> getAutocompleteSuggestions(String prefix, String collection, int limit) {
        List<String> targetCollections = ("all".equalsIgnoreCase(collection) || collection == null || collection.isEmpty()) 
                ? new ArrayList<>(collections.keySet()) : List.of(collection);

        Set<String> results = new HashSet<>();
        for (String col : targetCollections) {
            CollectionIndexData data = collections.get(col);
            if (data != null) {
                results.addAll(data.trie().searchPrefix(prefix.toLowerCase(), limit));
            }
        }
        
        return results.stream().limit(limit).collect(Collectors.toList());
    }

    public List<String> getDidYouMean(String query, String collection) {
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) return Collections.emptyList();
        
        String lastToken = queryTokens.get(queryTokens.size() - 1);

        List<String> targetCollections = ("all".equalsIgnoreCase(collection) || collection == null || collection.isEmpty()) 
                ? new ArrayList<>(collections.keySet()) : List.of(collection);

        Set<String> results = new HashSet<>();
        for (String col : targetCollections) {
            CollectionIndexData data = collections.get(col);
            if (data != null) {
                results.addAll(data.trie().findClosest(lastToken, 5));
            }
        }
        
        return results.stream().limit(5).collect(Collectors.toList());
    }

    public List<SearchResult> getRandomSuggestions(String collection, int limit) {
        List<String> targetCollections = ("all".equalsIgnoreCase(collection) || collection == null || collection.isEmpty()) 
                ? new ArrayList<>(collections.keySet()) : List.of(collection);

        List<Document> allDocs = new ArrayList<>();
        for (String col : targetCollections) {
            CollectionIndexData data = collections.get(col);
            if (data != null) {
                allDocs.addAll(data.documents().values());
            }
        }

        Collections.shuffle(allDocs);
        return allDocs.stream()
                .limit(limit)
                .map(doc -> new SearchResult(doc.getId(), doc.getSourceUrl(), doc.getTitle(), "", 0.0, doc.getCollection()))
                .collect(Collectors.toList());
    }

    public long getCollectionSize(String collection) {
        CollectionIndexData data = collections.get(collection);
        return data == null ? 0 : data.documents().size();
    }
}
