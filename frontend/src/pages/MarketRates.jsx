import { useState, useEffect, useCallback } from 'react';
import { getMarketRates } from '../api/client';

export default function MarketRates() {
  const [rates, setRates] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lastUpdated, setLastUpdated] = useState(null);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const fetchRates = useCallback(async () => {
    try {
      setError('');
      const data = await getMarketRates();
      setRates(data);
      setLastUpdated(new Date());
    } catch (err) {
      setError('Failed to fetch market rates. Please try again.');
      console.error('Market rates error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRates();
  }, [fetchRates]);

  // Auto-refresh every 5 minutes
  useEffect(() => {
    if (!autoRefresh) return;
    const interval = setInterval(fetchRates, 5 * 60 * 1000);
    return () => clearInterval(interval);
  }, [autoRefresh, fetchRates]);

  const formatINR = (val) => {
    if (!val && val !== 0) return '—';
    return '₹' + Number(val).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };

  if (loading) {
    return (
      <div className="market-rates-loading">
        <div className="loading-spinner"></div>
        <p>Fetching live market rates...</p>
      </div>
    );
  }

  return (
    <div className="market-rates-page">
      <div className="page-header">
        <h2>Market Rates</h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <label style={{ fontSize: '0.82rem', color: '#6b7280', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
              style={{ accentColor: 'var(--accent-color)' }}
            />
            Auto-refresh
          </label>
          <button className="btn btn-outline btn-sm" onClick={fetchRates}>
            ↻ Refresh
          </button>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {rates && (
        <>
          {/* Status Bar */}
          <div className="rates-status-bar">
            <span>
              {rates.isFallback ? '⚠️ Showing cached rates (API unavailable)' : '🟢 Live rates'}
            </span>
            <span>Source: {rates.source}</span>
            {lastUpdated && (
              <span>Updated: {lastUpdated.toLocaleTimeString('en-IN')}</span>
            )}
          </div>

          {/* Gold Section */}
          <div className="rates-section">
            <div className="rates-section-header gold">
              <span className="rates-icon">🥇</span>
              <h3>Gold Rates</h3>
              <span className="rates-subtitle">Today's live gold price in India</span>
            </div>

            <div className="rates-cards">
              <div className="rate-card gold-primary">
                <div className="rate-card-label">Gold 24K (99.9%)</div>
                <div className="rate-card-price">{formatINR(rates.gold?.gold24kPerGram)}</div>
                <div className="rate-card-unit">per gram</div>
                <div className="rate-card-secondary">
                  {formatINR(rates.gold?.gold24kPer10Gram)} / 10 gram
                </div>
              </div>

              <div className="rate-card gold-secondary">
                <div className="rate-card-label">Gold 22K (91.6%)</div>
                <div className="rate-card-price">{formatINR(rates.gold?.gold22kPerGram)}</div>
                <div className="rate-card-unit">per gram</div>
                <div className="rate-card-secondary">
                  {formatINR(rates.gold?.gold22kPer10Gram)} / 10 gram
                </div>
              </div>

              <div className="rate-card gold-tertiary">
                <div className="rate-card-label">Gold 18K (75%)</div>
                <div className="rate-card-price">{formatINR(rates.gold?.gold18kPerGram)}</div>
                <div className="rate-card-unit">per gram</div>
              </div>
            </div>
          </div>

          {/* Silver Section */}
          <div className="rates-section">
            <div className="rates-section-header silver">
              <span className="rates-icon">🥈</span>
              <h3>Silver Rates</h3>
              <span className="rates-subtitle">Today's live silver price in India</span>
            </div>

            <div className="rates-cards">
              <div className="rate-card silver-primary">
                <div className="rate-card-label">Silver</div>
                <div className="rate-card-price">{formatINR(rates.silver?.silverPerGram)}</div>
                <div className="rate-card-unit">per gram</div>
              </div>

              <div className="rate-card silver-secondary">
                <div className="rate-card-label">Silver</div>
                <div className="rate-card-price">{formatINR(rates.silver?.silverPer100Gram)}</div>
                <div className="rate-card-unit">per 100 gram</div>
              </div>

              <div className="rate-card silver-tertiary">
                <div className="rate-card-label">Silver</div>
                <div className="rate-card-price">{formatINR(rates.silver?.silverPerKg)}</div>
                <div className="rate-card-unit">per kg</div>
              </div>
            </div>
          </div>

          {/* Info Note */}
          <div className="rates-info">
            <p>
              <strong>Note:</strong> Rates shown are indicative and sourced from international spot markets.
              Actual jewellery prices may include making charges, GST (3%), and hallmarking charges.
              Rates refresh automatically every 5 minutes.
            </p>
          </div>
        </>
      )}
    </div>
  );
}
