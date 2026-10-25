import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  getAuctionById,
  placeBid,
} from '../services/auctionService';
import { connectToAuction } from '../services/websocketService';
import './AuctionDetails.css';

function AuctionDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [auction, setAuction] = useState(null);
  const [bidderId, setBidderId] = useState('');
  const [bidAmount, setBidAmount] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [liveConnected, setLiveConnected] = useState(false);

  const loadAuction = useCallback(async () => {
    const data = await getAuctionById(id);
    setAuction(data);
    return data;
  }, [id]);

  // Load auction details when the auction ID changes.
  useEffect(() => {
    let cancelled = false;

    setLoading(true);
    setError('');
    setAuction(null);

    getAuctionById(id)
      .then((data) => {
        if (!cancelled) {
          setAuction(data);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.message || 'Unable to load auction.');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [id]);

  // Subscribe to real-time updates for this auction.
  useEffect(() => {
    setLiveConnected(false);

    const disconnect = connectToAuction(
      id,
      (bidUpdate) => {
        setAuction((previousAuction) => {
          if (!previousAuction) {
            return previousAuction;
          }

          return {
            ...previousAuction,
            currentPrice: bidUpdate.currentPrice,
          };
        });

        setMessage(
          `New bid received: ₹${Number(
            bidUpdate.currentPrice
          ).toLocaleString('en-IN')}`
        );

        setError('');
      },
      setLiveConnected
    );

    return () => {
      disconnect();
    };
  }, [id]);

  async function handlePlaceBid(event) {
    event.preventDefault();
    setError('');
    setMessage('');

    const parsedBidderId = Number(bidderId);
    const parsedAmount = Number(bidAmount);

    if (!Number.isInteger(parsedBidderId) || parsedBidderId <= 0) {
      setError('Enter a valid bidder ID.');
      return;
    }

    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setError('Enter a valid bid amount.');
      return;
    }

    if (String(auction.status).toUpperCase() !== 'ACTIVE') {
      setError('This auction is not active.');
      return;
    }

    setSubmitting(true);

    try {
      await placeBid(id, parsedBidderId, parsedAmount);

      setBidAmount('');
      setMessage('Bid placed successfully!');

      // Refresh from the server to confirm the latest auction state.
      try {
        await loadAuction();
      } catch (refreshError) {
        console.error(
          'Bid was placed, but refreshing auction details failed:',
          refreshError
        );
      }
    } catch (err) {
      setError(err.message || 'Unable to place bid.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <p className="details-message">Loading auction...</p>;
  }

  if (error && !auction) {
    return (
      <p className="details-message error-message">{error}</p>
    );
  }

  if (!auction) {
    return <p className="details-message">Auction not found.</p>;
  }

  const currentPrice = Number(
    auction.currentPrice ?? auction.basePrice ?? 0
  );

  const isActive =
    String(auction.status).toUpperCase() === 'ACTIVE';

  const isEnded =
    String(auction.status).toUpperCase() === 'ENDED';

  const isScheduled =
    String(auction.status).toUpperCase() === 'SCHEDULED';

  return (
    <main className="auction-details-page">
      <button
        className="back-button"
        onClick={() => navigate('/')}
      >
        ← Back to auctions
      </button>

      <section className="details-card">
        <div className="details-image">
          {auction.imageUrl ? (
            <img src={auction.imageUrl} alt={auction.title} />
          ) : (
            <span>📦</span>
          )}
        </div>

        <div className="details-content">
          <span className="details-status">
            {auction.status}
          </span>

          <h1>{auction.title}</h1>

          <p>
            {auction.description || 'No description available.'}
          </p>

          <div className="current-price">
            <span>{isEnded ? 'Final price' : 'Current price'}</span>
            <h2>
              ₹
              {Number(
                isEnded
                  ? auction.winningAmount ?? currentPrice
                  : currentPrice
              ).toLocaleString('en-IN')}
            </h2>
          </div>

          <p>Auction ID: {auction.id}</p>
          <p>
            Start time: {auction.startTime || 'Not specified'}
          </p>
          <p>
            End time: {auction.endTime || 'Not specified'}
          </p>

          {!isEnded && (
            <p className="live-status">
              {liveConnected
                ? '🟢 Live updates connected'
                : '🟠 Connecting to live updates...'}
            </p>
          )}

          {isEnded ? (
            <section className="bid-section winner-info">
              <h2>🏆 Auction Results</h2>

              {auction.winnerId != null &&
              auction.winnerName ? (
                <>
                  <p>
                    <strong>Winner:</strong>{' '}
                    {auction.winnerName}
                  </p>

                  <p>
                    <strong>Winner ID:</strong>{' '}
                    {auction.winnerId}
                  </p>

                  <p>
                    <strong>Winning amount:</strong>{' '}
                    ₹
                    {Number(
                      auction.winningAmount ?? currentPrice
                    ).toLocaleString('en-IN')}
                  </p>
                </>
              ) : (
                <p>No winning bids were placed in this auction.</p>
              )}
            </section>
          ) : (
            <section className="bid-section">
              <h2>Place Your Bid</h2>

              {!isActive && (
                <p>
                  {isScheduled
                    ? 'This auction has not started yet.'
                    : 'This auction is not currently active.'}
                </p>
              )}

              <form onSubmit={handlePlaceBid}>
                <label htmlFor="bidderId">Bidder ID</label>
                <input
                  id="bidderId"
                  type="number"
                  min="1"
                  value={bidderId}
                  onChange={(event) =>
                    setBidderId(event.target.value)
                  }
                  placeholder="Enter your existing user ID"
                  required
                />

                <label htmlFor="bidAmount">Your bid (₹)</label>
                <input
                  id="bidAmount"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={bidAmount}
                  onChange={(event) =>
                    setBidAmount(event.target.value)
                  }
                  placeholder="Enter your bid amount"
                  required
                />

                <button
                  className="place-bid-btn"
                  type="submit"
                  disabled={!isActive || submitting}
                >
                  {submitting ? 'Submitting...' : 'Place Bid'}
                </button>
              </form>

              {message && (
                <p className="bid-success">{message}</p>
              )}

              {error && (
                <p className="error-message">{error}</p>
              )}
            </section>
          )}
        </div>
      </section>
    </main>
  );
}

export default AuctionDetails;
