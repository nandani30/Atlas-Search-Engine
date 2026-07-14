import { useState, useEffect, useRef } from 'react';
import './index.css';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

function App() {
  const [query, setQuery] = useState('');
  const [collection, setCollection] = useState('all');
  const [autocompleteSuggestions, setAutocompleteSuggestions] = useState([]);
  const [showAutocomplete, setShowAutocomplete] = useState(false);
  
  const [examples, setExamples] = useState([]);
  const [trending, setTrending] = useState([]);
  const [results, setResults] = useState(null); // null means haven't searched yet
  const [didYouMean, setDidYouMean] = useState([]);
  const [searchMessage, setSearchMessage] = useState('');
  
  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalResults, setTotalResults] = useState(0);
  const pageSize = 10;
  
  const [loading, setLoading] = useState(false);
  const debounceRef = useRef(null);

  useEffect(() => {
    if (!results) {
      fetch(`${API_BASE}/random-suggestions?collection=${collection}&count=5`)
        .then(res => res.json())
        .then(data => setExamples(data))
        .catch(err => console.error(err));

      fetch(`${API_BASE}/trending?collection=${collection}&limit=10`)
        .then(res => res.json())
        .then(data => setTrending(data))
        .catch(err => console.error(err));
    }
  }, [collection, results]);
  const handleQueryChange = (e) => {
    const val = e.target.value;
    setQuery(val);
    setShowAutocomplete(true);
    
    if (val.trim() === '') {
      setAutocompleteSuggestions([]);
      return;
    }

    if (debounceRef.current) clearTimeout(debounceRef.current);
    
    debounceRef.current = setTimeout(() => {
      fetch(`${API_BASE}/autocomplete?prefix=${encodeURIComponent(val)}&collection=${collection}&limit=5`)
        .then(res => res.json())
        .then(data => setAutocompleteSuggestions(data))
        .catch(err => console.error("Autocomplete error:", err));
    }, 200);
  };

  const handleSearch = (searchQuery = query, targetPage = 0) => {
    if (!searchQuery.trim()) return;
    setQuery(searchQuery);
    setPage(targetPage);
    setShowAutocomplete(false);
    setLoading(true);
    
    // Only clear results if it's a completely new search to avoid flashing
    if (targetPage === 0 && searchQuery !== query) {
        setResults(null);
    }
    
    setDidYouMean([]);
    setSearchMessage('');

    fetch(`${API_BASE}/search?q=${encodeURIComponent(searchQuery)}&collection=${collection}&page=${targetPage}&pageSize=${pageSize}`)
      .then(res => res.json())
      .then(data => {
        if (data.message === "no results") {
          setResults([]);
          setSearchMessage("No results found.");
          setDidYouMean(data.didYouMeanSuggestions || []);
          setTotalResults(0);
          setTotalPages(0);
        } else {
          setResults(data.results);
          setTotalResults(data.totalResults);
          setTotalPages(data.totalPages);
        }
      })
      .catch(err => {
        console.error("Search error:", err);
        setSearchMessage("Error occurred while searching. Please check console or API connection.");
        setResults([]); // Set to empty array so the error UI actually renders
      })
      .finally(() => setLoading(false));
  };

  return (
    <div className="container">
      <header className={`header ${results ? 'header-top' : ''}`}>
        <h1 className="logo" onClick={() => {setResults(null); setQuery('');}}>Atlas</h1>
        <div className="search-container">
          <div className="search-bar">
            <select 
              value={collection} 
              onChange={e => setCollection(e.target.value)}
              className="collection-select"
            >
              <option value="all">All</option>
              <option value="learning">Learning</option>
              <option value="science">Science</option>
              <option value="news">News</option>
            </select>
            <input 
              type="text" 
              value={query} 
              onChange={handleQueryChange}
              onKeyDown={e => e.key === 'Enter' && handleSearch()}
              placeholder="Search the world's knowledge..."
              className="search-input"
              onFocus={() => setShowAutocomplete(true)}
              onBlur={() => setTimeout(() => setShowAutocomplete(false), 200)}
            />
            <button onClick={() => handleSearch()} className="search-button">
              Search
            </button>
          </div>
          
          {showAutocomplete && autocompleteSuggestions.length > 0 && (
            <div className="autocomplete-dropdown">
              {autocompleteSuggestions.map((s, idx) => (
                <div 
                  key={idx} 
                  className="autocomplete-item"
                  onClick={() => handleSearch(s)}
                >
                  {s}
                </div>
              ))}
            </div>
          )}
        </div>
      </header>

      <main className="main-content">
        {loading && <div className="loading">Searching...</div>}

        {results && results.length > 0 && !loading && (
          <div className="results-container">
            <p className="results-count">About {totalResults} results</p>
            <div className="results-list">
              {results.map(r => (
                <div key={r.id + r.title} className="result-item">
                  <div className="result-header">
                    <a href={r.url} className="result-title-link">
                      <h3 className="result-title">{r.title}</h3>
                    </a>
                    <span className={`badge badge-${r.sourceCollection}`}>{r.sourceCollection}</span>
                    <span className="result-score">Score: {r.score.toFixed(2)}</span>
                  </div>
                  <p className="result-snippet">{r.snippet}</p>
                </div>
              ))}
            </div>
            
            {totalPages > 1 && (
              <div className="pagination">
                <button 
                  className="page-btn" 
                  disabled={page === 0}
                  onClick={() => handleSearch(query, page - 1)}
                >
                  Previous
                </button>
                <span className="page-info">Page {page + 1} of {totalPages}</span>
                <button 
                  className="page-btn" 
                  disabled={page >= totalPages - 1}
                  onClick={() => handleSearch(query, page + 1)}
                >
                  Next
                </button>
              </div>
            )}
          </div>
        )}

        {results && results.length === 0 && !loading && (
          <div className="no-results">
            <h2>{searchMessage}</h2>
            {didYouMean.length > 0 && (
              <div className="did-you-mean">
                <p>Did you mean:</p>
                <div className="suggestions">
                  {didYouMean.map((word, idx) => (
                    <button 
                      key={idx} 
                      className="suggestion-btn"
                      onClick={() => handleSearch(word)}
                    >
                      {word}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
}

export default App;
