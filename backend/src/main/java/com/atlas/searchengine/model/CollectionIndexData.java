package com.atlas.searchengine.model;

import com.atlas.searchengine.service.AutocompleteTrie;

import java.util.Map;

public record CollectionIndexData(
    Map<String, Map<String, Integer>> invertedIndex,
    Map<String, Document> documents,
    Map<String, Integer> documentLengths,
    AutocompleteTrie trie
) {}
