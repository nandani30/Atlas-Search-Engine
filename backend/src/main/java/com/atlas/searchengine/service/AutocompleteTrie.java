package com.atlas.searchengine.service;

import java.util.*;

public class AutocompleteTrie {
    private final TrieNode root;

    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isWord = false;
        String word = null;
    }

    public AutocompleteTrie() {
        this.root = new TrieNode();
    }

    public void insert(String word) {
        if (word == null || word.isEmpty()) return;
        TrieNode current = root;
        for (char c : word.toCharArray()) {
            current.children.putIfAbsent(c, new TrieNode());
            current = current.children.get(c);
        }
        current.isWord = true;
        current.word = word;
    }

    public List<String> searchPrefix(String prefix, int limit) {
        List<String> results = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) return results;
        
        TrieNode current = root;
        for (char c : prefix.toCharArray()) {
            if (!current.children.containsKey(c)) {
                return results;
            }
            current = current.children.get(c);
        }
        
        dfs(current, results, limit);
        return results;
    }

    private void dfs(TrieNode node, List<String> results, int limit) {
        if (results.size() >= limit) return;
        if (node.isWord) {
            results.add(node.word);
        }
        for (TrieNode child : node.children.values()) {
            dfs(child, results, limit);
        }
    }

    // Levenshtein distance search in Trie
    public List<String> findClosest(String word, int limit) {
        List<String> results = new ArrayList<>();
        if (word == null || word.isEmpty()) return results;

        int[] currentRow = new int[word.length() + 1];
        for (int i = 0; i <= word.length(); i++) {
            currentRow[i] = i;
        }

        PriorityQueue<WordDistance> pq = new PriorityQueue<>(Comparator.comparingInt(w -> w.distance));

        for (Map.Entry<Character, TrieNode> entry : root.children.entrySet()) {
            searchRecursive(entry.getValue(), entry.getKey(), word, currentRow, pq);
        }

        while (!pq.isEmpty() && results.size() < limit) {
            results.add(pq.poll().word);
        }
        
        return results;
    }

    private void searchRecursive(TrieNode node, char letter, String word, int[] previousRow, PriorityQueue<WordDistance> pq) {
        int columns = word.length() + 1;
        int[] currentRow = new int[columns];
        currentRow[0] = previousRow[0] + 1;

        int minDistance = currentRow[0];

        for (int c = 1; c < columns; c++) {
            int insertCost = currentRow[c - 1] + 1;
            int deleteCost = previousRow[c] + 1;
            int replaceCost = word.charAt(c - 1) == letter ? previousRow[c - 1] : previousRow[c - 1] + 1;

            currentRow[c] = Math.min(Math.min(insertCost, deleteCost), replaceCost);
            minDistance = Math.min(minDistance, currentRow[c]);
        }

        if (node.isWord && currentRow[columns - 1] <= 3) {
            pq.add(new WordDistance(node.word, currentRow[columns - 1]));
        }

        if (minDistance <= 3) {
            for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
                searchRecursive(entry.getValue(), entry.getKey(), word, currentRow, pq);
            }
        }
    }

    private record WordDistance(String word, int distance) {}
}
