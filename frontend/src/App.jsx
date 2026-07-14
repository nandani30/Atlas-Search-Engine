import { useState, useEffect, useRef } from 'react';
import './index.css';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const SearchIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="search-icon">
    <circle cx="11" cy="11" r="8"></circle>
    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
  </svg>
);

const FlameIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="trending-icon">
    <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"></path>
  </svg>
);

function App() {
  const [query, setQuery] = useState('');
  const [collection, setCollection] = useState('all');
  const [autocompleteSuggestions, setAutocompleteSuggestions] = useState([]);
  const [showAutocomplete, setShowAutocomplete] = useState(false);
  
  const [examples, setExamples] = useState([]);
  const [trending, setTrending] = useState([]);
  const [results, setResults] = useState(null);
  const [didYouMean, setDidYouMean] = useState([]);
  const [searchMessage, setSearchMessage] = useState('');
  
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
        setSearchMessage("Error occurred while searching.");
        setResults([]);
      })
      .finally(() => setLoading(false));
  };

  const renderAutocompleteItem = (s, idx) => {
    const matchIndex = s.toLowerCase().indexOf(query.toLowerCase());
    if (matchIndex >= 0) {
      const before = s.substring(0, matchIndex);
      const match = s.substring(matchIndex, matchIndex + query.length);
      const after = s.substring(matchIndex + query.length);
      return (
        <div key={idx} className="autocomplete-item" onMouseDown={(e) => { e.preventDefault(); handleSearch(s); }}>
          <SearchIcon />
          <span>
            {before}<span className="bold-prefix">{match}</span>{after}
          </span>
        </div>
      );
    }
    return (
      <div key={idx} className="autocomplete-item" onMouseDown={(e) => { e.preventDefault(); handleSearch(s); }}>
        <SearchIcon />
        <span>{s}</span>
      </div>
    );
  };

  return (
    <div className="container">
      <header className={`header ${results ? 'header-top' : ''}`}>
        <div className="logo-container" onClick={() => {setResults(null); setQuery('');}}>
          <div className="logo-square"></div>
        </div>
        
        {!results && (
          <div className="header-subtitle">
            Search learning, science, and news
          </div>
        )}

        <div className="search-container">
          <div className="search-bar">
            <SearchIcon />
            <input 
              type="text" 
              value={query} 
              onChange={handleQueryChange}
              onKeyDown={e => e.key === 'Enter' && handleSearch()}
              className="search-input"
              onFocus={() => setShowAutocomplete(true)}
              onBlur={() => setShowAutocomplete(false)}
            />
          </div>
          
          {showAutocomplete && autocompleteSuggestions.length > 0 && (
            <div className="autocomplete-dropdown">
              {autocompleteSuggestions.map((s, idx) => renderAutocompleteItem(s, idx))}
            </div>
          )}
        </div>

        {results && (
          <div className="collection-filters">
            {['all', 'learning', 'science', 'news'].map(col => (
              <button 
                key={col}
                className={`filter-pill ${collection === col ? 'active' : ''}`}
                onClick={() => {
                  setCollection(col);
                  if (query) {
                    setTimeout(() => handleSearch(query, 0), 0);
                  }
                }}
              >
                {col.charAt(0).toUpperCase() + col.slice(1)}
              </button>
            ))}
          </div>
        )}
      </header>

      <main className="main-content">
        {loading && <div className="loading">Searching...</div>}

        {!results && !loading && (
          <div className="landing-section">
            <div className="landing-title">Try searching</div>
            <div className="landing-pills">
              {examples.map((ex, idx) => (
                <button key={`ex-${idx}`} className="suggestion-pill" onClick={() => handleSearch(ex.title)}>
                  {ex.title}
                </button>
              ))}
            </div>
            
            {trending.length > 0 && (
              <div style={{ marginTop: '2.5rem' }}>
                <div className="landing-title">Trending searches</div>
                <div className="landing-pills">
                  {trending.map((trend, idx) => (
                    <button key={`tr-${idx}`} className="suggestion-pill" onClick={() => handleSearch(trend)}>
                      <FlameIcon />
                      {trend}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {results && results.length > 0 && !loading && (
          <div className="results-container">
            <p className="results-count">About {totalResults} results</p>
            <div className="results-list">
              {results.map(r => (
                <div key={r.id + r.title} className="result-item">
                  <div className="result-meta">
                    <span className={`badge badge-${r.sourceCollection}`}>
                      {r.sourceCollection.charAt(0).toUpperCase() + r.sourceCollection.slice(1)}
                    </span>
                    <span className="result-url">{new URL(r.url).hostname}</span>
                  </div>
                  <a href={r.url} className="result-title-link">
                    <h3 className="result-title">{r.title}</h3>
                  </a>
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
                <div className="page-info">
                  <span>Page {page + 1} of</span>
                  <span>{totalPages}</span>
                </div>
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
