# Atlas Search Engine

Atlas is a full-stack search engine with a Spring Boot backend and React/Vite frontend. It includes web crawlers for Wikipedia, BBC News, and Movies, as well as a BM25 ranking algorithm and a Levenshtein-distance based autocomplete trie.

## Prerequisites

- Java 17
- Maven
- Node.js (for frontend)

## Project Structure

- `/backend`: Spring Boot application.
- `/frontend`: React + Vite application.
- `/data`: Directory where crawled and fallback JSON data is stored.

## Running Locally

### Database Setup (Turso/libSQL)

Atlas uses Turso (libSQL) instead of PostgreSQL. Turso is a distributed, SQLite-compatible database.
Because SQLite does not fully support highly concurrent writers without throwing "database is locked" exceptions, Atlas implements a **Single-Writer Queue**. The crawlers enqueue their parsed pages into this in-memory queue, and a dedicated `DocumentWriterService` thread processes the writes sequentially.

1. **Local Dev**: Use the Turso CLI:
   ```bash
   turso dev
   ```
   Set your `backend/.env` to point to the local URL (usually `http://127.0.0.1:8080`).

2. **Cloud Dev**: Create a `.env` file in `backend/`:
   ```env
   TURSO_DATABASE_URL=https://<your-turso-db>.turso.io
   TURSO_AUTH_TOKEN=<your-token>
   ```

### Backend

1. **Start the server**:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
   The backend runs on `http://localhost:8080`.
   On startup, the crawler jobs are scheduled to run every 1 hour automatically, seeding the Turso DB.

### Frontend

1. **Install and Run**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   The frontend runs on `http://localhost:5173` (by default with Vite).

## Deployment

### Backend (e.g. Render / Railway)
1. Use the provided `Dockerfile` in the `backend/` directory.
2. The `Dockerfile` exposes port `8080`.
3. Set any necessary environment variables (e.g. `PORT=8080`).
4. Keep in mind that since the index is built in memory, the server needs enough RAM to hold the index, and file storage for the crawler output is ephemeral on some cloud platforms unless a persistent volume is mounted at `/app/data`.

### Frontend (e.g. Vercel / Netlify)
1. In your frontend hosting platform, set the build command to `npm run build` and output directory to `dist`.
2. Add an environment variable in your frontend `.env` (or directly in the code `API_BASE`) pointing to your deployed backend URL.

## Architecture Highlights
- **BM25 Ranker**: Custom implementation of BM25 term frequency saturation and document length normalization.
- **Autocomplete Trie**: Fast prefix search and Levenshtein distance "did you mean" spelling correction.
- **Crawlers**: Standalone Jsoup web crawlers with BFS traversal, delay logic, and visited sets.
