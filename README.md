<div align="center">
  <img src="https://upload.wikimedia.org/wikipedia/commons/e/e1/Atom_icon_black.svg" width="80" alt="Atlas Logo"/>
  <h1>Atlas Search Engine</h1>
  <p><strong>Everything worth knowing, in one search.</strong></p>
  <a href="https://atlas-search-engine-lime.vercel.app/"><strong>🔗 Visit Live Demo (Vercel)</strong></a>
</div>

<br />

Atlas is a high-performance, full-stack **niche search engine** engineered from scratch. Rather than attempting to index the entire web, Atlas specializes in curated, high-quality information streams—specifically targeted at **News, Science, and Learning**. It features custom web crawlers, a global BM25 ranking algorithm, and real-time autocomplete—all built without relying on external search services like Elasticsearch or Algolia.

## 🚀 Key Features

* **Custom Web Crawlers**: Autonomous background Java agents that intelligently crawl and scrape data from world-renowned sources (BBC, Reuters, NASA, CERN, Wikipedia, etc.).
* **Content Freshness Extraction**: Uses Jsoup to parse hidden HTML `<meta>` and `<time>` tags, determining the true real-world publication date of articles to surface the freshest news.
* **Global BM25 Ranking Algorithm**: Custom-built search relevance algorithm featuring Term Frequency Saturation (TF) and Global Inverse Document Frequency (IDF) scoring across distributed collections.
* **Trie-Based Autocomplete**: Instant search suggestions powered by an optimized Trie data structure.
* **Levenshtein Spell Correction**: Intelligent "Did you mean?" functionality for fuzzy matching and typo tolerance.
* **Single-Writer Database Queue**: Engineered a custom asynchronous queueing system to handle high-concurrency DB writes for SQLite (Turso) without locking exceptions.

## 🛠 Tech Stack

* **Frontend**: React, Vite, Vanilla CSS
* **Backend**: Java 17, Spring Boot, Jsoup
* **Database**: Turso (Distributed SQLite)
* **Hosting**: Vercel (Frontend), Render (Backend Engine)

## 🧠 System Architecture

1. **Crawler Layer**: Runs scheduled tasks (`@Scheduled`) to scrape configured seed URLs. Extracts title, text, and publication metadata using `Jsoup`.
2. **Database Layer**: A single-threaded `DocumentWriterService` consumes from a blocking queue, safely persisting crawled pages into the Turso database to avoid SQLite concurrency locks.
3. **Indexing Layer**: Upon startup, the `IndexerService` loads documents into a highly optimized In-Memory Inverted Index and computes global corpus statistics.
4. **Search Layer**: Queries are tokenized, cleaned, and scored using a finely tuned BM25 formula that boosts title matches by 15x.

## 💻 Running Locally

### 1. Database Setup
Atlas uses Turso (libSQL). You can run it locally using the CLI:
```bash
turso dev
```
Create a `backend/.env` file:
```env
TURSO_DATABASE_URL=http://127.0.0.1:8080
# Or use your remote Turso URL and Token
```

### 2. Backend
```bash
cd backend
./mvnw spring-boot:run
```
*The Spring Boot server will start on port 8080 and immediately trigger the web crawlers in the background.*

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```
*The Vite development server will start on port 5173.*
