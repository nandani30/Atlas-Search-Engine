package com.atlas.searchengine.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    private String id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String text;

    private String collection;

    private String sourceUrl;

    private LocalDateTime crawledAt;

    private LocalDateTime publishedAt;

    public Document() {
    }

    public Document(String id, String title, String text, String collection, String sourceUrl, LocalDateTime crawledAt, LocalDateTime publishedAt) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.collection = collection;
        this.sourceUrl = sourceUrl;
        this.crawledAt = crawledAt;
        this.publishedAt = publishedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public LocalDateTime getCrawledAt() {
        return crawledAt;
    }

    public void setCrawledAt(LocalDateTime crawledAt) {
        this.crawledAt = crawledAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
