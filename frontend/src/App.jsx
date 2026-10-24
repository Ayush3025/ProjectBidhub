
import './App.css';
import { useEffect, useState } from 'react';
import {
  BrowserRouter,
  Routes,
  Route,
  Link,
  useNavigate,
} from 'react-router-dom';

import { getAllAuctions } from './services/auctionService';
import AuctionDetails from './pages/AuctionDetails';

function HomePage() {
  const [auctions, setAuctions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getAllAuctions()
      .then((data) => {
        setAuctions(data);
        setError('');
      })
      .catch((err) => {
        setError(err.message || 'Failed to load auctions.');
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  return (
    <div className="app">
      <nav className="navbar">
        <Link to="/" className="logo">
          Bid<span>Hub</span>
        </Link>

        <div className="nav-links">
          <Link to="/">Home</Link>
          <Link to="/#auctions">Auctions</Link>
          <Link to="/#how-it-works">How It Works</Link>
        </div>

        <div className="nav-actions">
          <button className="login-btn">Login</button>
          <button className="sell-btn">Start Selling</button>
        </div>
      </nav>

      <main id="home">
        <section className="hero">
          <p className="eyebrow">THE NEXT BID COULD BE YOURS</p>

          <h2>
            Discover. Bid. <span>Win.</span>
          </h2>

          <p className="hero-description">
            Find unique products, compete in live auctions,
            and make every bid count.
          </p>

          <div className="hero-actions">
            <Link to="/#auctions" className="primary-btn">
              Explore Auctions
            </Link>

            <Link to="/#how-it-works" className="secondary-btn">
              How It Works
            </Link>
          </div>
        </section>

        <section id="auctions" className="auctions-section">
          <div className="section-heading">
            <div>
              <p className="eyebrow">HANDPICKED FOR YOU</p>
              <h2>Featured Auctions</h2>
            </div>

            <span className="live-label">● LIVE BIDDING</span>
          </div>

          <div className="auction-grid">
            {loading && <p>Loading auctions...</p>}

            {error && (
              <p className="error-message">
                Unable to load auctions: {error}
              </p>
            )}

            {!loading && !error && auctions.length === 0 && (
              <p>No auctions are available right now.</p>
            )}

            {!loading &&
              !error &&
              auctions.map((auction) => (
                <article className="auction-card" key={auction.id}>
                  <div className="product-image">
                    {auction.imageUrl ? (
                      <img
                        src={auction.imageUrl}
                        alt={auction.title || 'Auction item'}
                        className="auction-image"
                      />
                    ) : (
                      <span className="fallback-icon">📦</span>
                    )}

                    {String(auction.status).toUpperCase() ===
                      'ACTIVE' && (
                      <span className="status-tag">LIVE</span>
                    )}
                  </div>

                  <div className="card-content">
                    <p className="category">
                      {auction.status || 'AUCTION'}
                    </p>

                    <h3>{auction.title || 'Untitled Auction'}</h3>

                    <p className="current-label">Current bid</p>

                    <div className="price-row">
                      <strong>
                        ₹
                        {Number(
                          auction.currentPrice ??
                            auction.basePrice ??
                            0
                        ).toLocaleString('en-IN')}
                      </strong>

                      <span>#{auction.id}</span>
                    </div>

                    <p className="auction-description">
                      {auction.description ||
                        'No description available.'}
                    </p>

                    <Link
                      to={`/auction/${auction.id}`}
                      className="view-auction-btn"
                    >
                      View Auction →
                    </Link>
                  </div>
                </article>
              ))}
          </div>
        </section>

        <section id="how-it-works" className="how-section">
          <p className="eyebrow">SIMPLE. TRANSPARENT. EXCITING.</p>
          <h2>Your next win starts here.</h2>
          <p>
            Create an account, find an auction, and place your bid.
          </p>
        </section>
      </main>

      <footer className="footer">
        <p>© 2026 BidHub. Every bid tells a story.</p>
      </footer>
    </div>
  );
}

function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <main className="details-message">
      <h2>Page not found</h2>
      <button onClick={() => navigate('/')}>Back to BidHub</button>
    </main>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/auction/:id" element={<AuctionDetails />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
