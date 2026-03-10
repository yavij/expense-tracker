import React, { useState, useEffect } from 'react';
import { getPayoffSchedule, getDebts } from '../api/client';


export default function DebtPayoff() {
  const [strategy, setStrategy] = useState('snowball');
  const [extraPayment, setExtraPayment] = useState(0);
  const [sliderPayment, setSliderPayment] = useState(0);
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [debts, setDebts] = useState([]);
  const [error, setError] = useState(null);

  // Fetch existing debts on mount
  useEffect(() => {
    const fetchDebts = async () => {
      try {
        const data = await getDebts();
        setDebts(data);
      } catch (err) {
        setError(err.message);
      }
    };
    fetchDebts();
  }, []);

  const handleCalculate = async () => {
    if (debts.length === 0) return;

    setLoading(true);
    setError(null);
    try {
      const currentPayment = extraPayment || sliderPayment;
      const data = await getPayoffSchedule({
        strategy,
        extraPayment: currentPayment
      });
      setResults(data);
    } catch (err) {
      setError('Failed to calculate payoff schedule');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleSliderChange = async (e) => {
    const value = parseInt(e.target.value, 10);
    setSliderPayment(value);
    setExtraPayment(0); // Reset text input

    if (debts.length === 0) return;

    setLoading(true);
    setError(null);
    try {
      const data = await getPayoffSchedule({
        strategy,
        extraPayment: value
      });
      setResults(data);
    } catch (err) {
      setError('Failed to calculate payoff schedule');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleStrategyChange = (newStrategy) => {
    setStrategy(newStrategy);
    if (results) {
      // Recalculate with new strategy
      const currentPayment = extraPayment || sliderPayment;
      handleCalculate();
    }
  };

  if (debts.length === 0) {
    return (
      <div className="debt-payoff-container">
        <h1>Debt Payoff Strategy</h1>
        <div className="empty-state">
          <p>No debts found. Add a debt to get started with a payoff strategy.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="debt-payoff-container">
      <h1>Debt Payoff Strategy</h1>

      {error && <div className="error-message">{error}</div>}

      {/* Strategy Selection */}
      <section className="strategy-section">
        <h2>Choose Your Strategy</h2>
        <div className="strategy-buttons">
          <button
            className={`strategy-btn ${strategy === 'snowball' ? 'active' : ''}`}
            onClick={() => handleStrategyChange('snowball')}
          >
            Snowball
            <span className="strategy-desc">Lowest balance first</span>
          </button>
          <button
            className={`strategy-btn ${strategy === 'avalanche' ? 'active' : ''}`}
            onClick={() => handleStrategyChange('avalanche')}
          >
            Avalanche
            <span className="strategy-desc">Highest interest first</span>
          </button>
        </div>
      </section>

      {/* Extra Payment Input */}
      <section className="input-section">
        <h2>Monthly Extra Payment</h2>
        <div className="input-group">
          <label htmlFor="extra-payment">Additional amount (beyond minimum EMIs):</label>
          <input
            id="extra-payment"
            type="number"
            min="0"
            step="100"
            value={extraPayment}
            onChange={(e) => setExtraPayment(parseInt(e.target.value, 10) || 0)}
            placeholder="0"
          />
          <span className="currency">₹</span>
        </div>
        <button
          className="calculate-btn"
          onClick={handleCalculate}
          disabled={loading}
        >
          {loading ? 'Calculating...' : 'Calculate'}
        </button>
      </section>

      {/* Results Section */}
      {results && (
        <>
          {/* Summary Cards */}
          <section className="summary-section">
            <h2>Payoff Summary</h2>
            <div className="summary-cards">
              <div className="card">
                <div className="card-label">Total Interest Paid</div>
                <div className="card-value">
                  ₹{results.totalInterest.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                </div>
              </div>
              <div className="card">
                <div className="card-label">Months to Debt-Free</div>
                <div className="card-value">{results.monthsToDebtFree}</div>
              </div>
              <div className="card">
                <div className="card-label">Interest Saved vs Minimum</div>
                <div className="card-value">
                  ₹{results.interestSaved.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                </div>
              </div>
            </div>
          </section>

          {/* Payoff Schedule Table */}
          <section className="schedule-section">
            <h2>Payoff Schedule</h2>
            <div className="table-wrapper">
              <table className="payoff-table">
                <thead>
                  <tr>
                    <th>Month</th>
                    <th>Debt Name</th>
                    <th>Payment</th>
                    <th>Principal</th>
                    <th>Interest</th>
                    <th>Remaining Balance</th>
                  </tr>
                </thead>
                <tbody>
                  {results.schedule.map((row, idx) => (
                    <tr key={idx}>
                      <td>{row.month}</td>
                      <td>{row.debtName}</td>
                      <td>₹{row.payment.toLocaleString('en-IN', { maximumFractionDigits: 0 })}</td>
                      <td>₹{row.principal.toLocaleString('en-IN', { maximumFractionDigits: 0 })}</td>
                      <td>₹{row.interest.toLocaleString('en-IN', { maximumFractionDigits: 0 })}</td>
                      <td>₹{row.remainingBalance.toLocaleString('en-IN', { maximumFractionDigits: 0 })}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          {/* What-If Slider */}
          <section className="slider-section">
            <h2>What-If Scenario</h2>
            <div className="slider-container">
              <label htmlFor="payment-slider">
                Monthly Extra Payment: ₹{sliderPayment.toLocaleString('en-IN')}
              </label>
              <input
                id="payment-slider"
                type="range"
                min="0"
                max="50000"
                step="1000"
                value={sliderPayment}
                onChange={handleSliderChange}
                className="slider"
              />
              <div className="slider-labels">
                <span>₹0</span>
                <span>₹50,000</span>
              </div>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
