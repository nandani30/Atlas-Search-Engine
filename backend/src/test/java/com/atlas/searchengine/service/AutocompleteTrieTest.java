package com.atlas.searchengine.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AutocompleteTrieTest {

    @Test
    void testSearchPrefix() {
        AutocompleteTrie trie = new AutocompleteTrie();
        trie.insert("hello");
        trie.insert("helicopter");
        trie.insert("hero");
        trie.insert("world");
        
        List<String> results = trie.searchPrefix("hel", 5);
        assertEquals(2, results.size());
        assertTrue(results.contains("hello"));
        assertTrue(results.contains("helicopter"));
    }

    @Test
    void testFindClosest() {
        AutocompleteTrie trie = new AutocompleteTrie();
        trie.insert("hello");
        trie.insert("help");
        trie.insert("world");
        
        // "helo" has distance 1 from "hello"
        List<String> results = trie.findClosest("helo", 5);
        assertTrue(results.contains("hello"));
        assertTrue(results.contains("help")); // also distance 1
    }
}
