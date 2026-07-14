import { useState, useEffect, useRef } from 'react';
import { IconSearch, IconAtom, IconBooks, IconFlask, IconNews } from '@tabler/icons-react';
import './index.css';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

function App() {
  const [query, setQuery] = useState('');
  const [collection, setCollection] = useState('all');
  const [autocompleteSuggestions, setAutocompleteSuggestions] = useState([]);
  const [showAutocomplete, setShowAutocomplete] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  
  const [examples, setExamples] = useState([]);
  const [results, setResults] = useState(null);
  const [didYouMean, setDidYouMean] = useState([]);
  const [searchMessage, setSearchMessage] = useState('');
  
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalResults, setTotalResults] = useState(0);
  const pageSize = 10;
  
  const [loading, setLoading] = useState(false);
  const debounceRef = useRef(null);
  const searchInputRef = useRef(null);

  useEffect(() => {
    if (!results) {
      fetch(`${API_BASE}/random-suggestions?collection=${collection}&count=5`)
        .then(res => res.json())
        .then(data => setExamples(data))
        .catch(err => console.error(err));
    }
  }, [collection, results]);

  const handleQueryChange = (e) => {
    const val = e.target.value;
    setQuery(val);
    setShowAutocomplete(true);
    setSelectedIndex(-1);
    
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

  const handleKeyDown = (e) => {
    if (!showAutocomplete || autocompleteSuggestions.length === 0) {
      if (e.key === 'Enter') {
        handleSearch();
      }
      return;
    }

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex(prev => 
        prev < autocompleteSuggestions.length - 1 ? prev + 1 : prev
      );
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex(prev => (prev > -1 ? prev - 1 : -1));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (selectedIndex >= 0) {
        handleSearch(autocompleteSuggestions[selectedIndex]);
      } else {
        handleSearch();
      }
    }
  };

  const handleSearch = (searchQuery = query, targetPage = 0) => {
    if (!searchQuery.trim()) return;
    setQuery(searchQuery);
    setPage(targetPage);
    setShowAutocomplete(false);
    setSelectedIndex(-1);
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
    const isSelected = idx === selectedIndex;
    
    let content;
    if (matchIndex >= 0) {
      const before = s.substring(0, matchIndex);
      const match = s.substring(matchIndex, matchIndex + query.length);
      const after = s.substring(matchIndex + query.length);
      content = (
        <span>
          {before}<span className="bold-prefix">{match}</span>{after}
        </span>
      );
    } else {
      content = <span>{s}</span>;
    }

    return (
      <div 
        key={idx} 
        className={`autocomplete-item ${isSelected ? 'selected' : ''}`}
        onMouseDown={(e) => { e.preventDefault(); handleSearch(s); }}
        onMouseEnter={() => setSelectedIndex(idx)}
      >
        <IconSearch size={14} className="autocomplete-item-icon" />
        {content}
      </div>
    );
  };

  return (
    <div className="container">
      {!results && (
        <div className="landing-header">
          <IconAtom size={36} className="logo-icon" style={{ marginBottom: '8px' }} />
          <span className="logo-text">Atlas</span>
          <span className="header-subtitle">Everything worth knowing, in one search.</span>
        </div>
      )}

      {results && (
        <div className="results-header" onClick={() => {setResults(null); setQuery('');}}>
          <IconAtom size={22} className="logo-icon" />
          <span className="logo-text">Atlas</span>
        </div>
      )}

      <div className={`search-container ${results ? 'results-search-container' : ''}`}>
        <div className="search-bar">
          <input 
            ref={searchInputRef}
            type="text" 
            value={query} 
            onChange={handleQueryChange}
            onKeyDown={handleKeyDown}
            className="search-input"
            placeholder="Search learning, science, or news"
            onFocus={() => setShowAutocomplete(true)}
            onBlur={() => setShowAutocomplete(false)}
          />
          <IconSearch size={16} className="search-icon" />
        </div>
        
        {showAutocomplete && autocompleteSuggestions.length > 0 && (
          <div className="autocomplete-dropdown">
            {autocompleteSuggestions.map((s, idx) => renderAutocompleteItem(s, idx))}
          </div>
        )}
      </div>

      {results && (
        <div className="collection-filters">
          <button 
            className={`filter-pill ${collection === 'all' ? 'active' : ''}`}
            onClick={() => { setCollection('all'); setTimeout(() => handleSearch(query, 0), 0); }}
          >
            All
          </button>
          <button 
            className={`filter-pill ${collection === 'learning' ? 'active' : ''}`}
            onClick={() => { setCollection('learning'); setTimeout(() => handleSearch(query, 0), 0); }}
          >
            <IconBooks size={14} style={{ marginRight: '4px', verticalAlign: '-2px' }} />
            Learning
          </button>
          <button 
            className={`filter-pill ${collection === 'science' ? 'active' : ''}`}
            onClick={() => { setCollection('science'); setTimeout(() => handleSearch(query, 0), 0); }}
          >
            <IconFlask size={14} style={{ marginRight: '4px', verticalAlign: '-2px' }} />
            Science
          </button>
          <button 
            className={`filter-pill ${collection === 'news' ? 'active' : ''}`}
            onClick={() => { setCollection('news'); setTimeout(() => handleSearch(query, 0), 0); }}
          >
            <IconNews size={14} style={{ marginRight: '4px', verticalAlign: '-2px' }} />
            News
          </button>
        </div>
      )}

      <main>
        {loading && <div className="loading">Searching...</div>}

        {!results && !loading && (
          <div className="landing-section">
            <p className="landing-title">Try searching</p>
            <div className="landing-pills">
              {examples.map((ex, idx) => (
                <button key={`ex-${idx}`} className="suggestion-pill" onClick={() => handleSearch(ex.title)}>
                  {ex.title}
                </button>
              ))}
            </div>
          </div>
        )}

        {results && results.length > 0 && !loading && (
          <div>
            <div className="results-count">About {totalResults} results</div>
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
                <span className="page-info">
                  Page {page + 1} of {totalPages}
                </span>
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
