import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getInvestments, deleteInvestment, exportCsv } from '../api/client';
import { INVESTMENT_TYPES, getInvestmentTypeLabel } from '../constants/investments';


export default function InvestmentList() {
  const navigate = useNavigate();
  const [investments, setInvestments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedType, setSelectedType] = useState('ALL');

  // Fetch investments
  useEffect(() => {
    let cancelled = false;

    const fetchInvestments = async () => {
      try {
        setLoading(true);
        setError(null);
        const params = selectedType !== 'ALL' ? { type: selectedType } : {};
        const data = await getInvestments(params);
        if (!cancelled) {
          setInvestments(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || 'Failed to fetch investments');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchInvestments();

    return () => {
      cancelled = true;
    };
  }, [selectedType]);

  // Calculate portfolio summary
  const summary = investments.reduce(
    (acc, inv) => {
      acc.totalInvested += inv.investedAmount || 0;
      acc.currentValue += inv.currentValue || 0;
      return acc;
    },
    { totalInvested: 0, currentValue: 0 }
  );

  const totalGainLoss = summary.currentValue - summary.totalInvested;
  const gainLossPercent = summary.totalInvested > 0
    ? ((totalGainLoss / summary.totalInvested) * 100).toFixed(2)
    : 0;

  const isPositive = totalGainLoss >= 0;

  // Handle delete
  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this investment?')) {
      try {
        await deleteInvestment(id);
        setInvestments(investments.filter((inv) => inv.id !== id));
      } catch (err) {
        setError(err.message || 'Failed to delete investment');
      }
    }
  };

  // Format currency
  const formatCurrency = (amount) => {
    return `₹${(amount || 0).toLocaleString('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;
  };

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN');
  };

  return (
    <div>
      <div className="page-header">
        <h2>Portfolio Overview</h2>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button className="btn" style={{ background: '#059669', color: 'white' }}
            onClick={() => exportCsv('investments')}>Export CSV</button>
          <button className="btn btn-primary" onClick={() => navigate('/app/investments/new')}>
            + Add Investment
          </button>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      {/* Summary Cards */}
      <div className="summary-cards">
        <div className="summary-card">
          <h3>Total Invested</h3>
          <div className="value">{formatCurrency(summary.totalInvested)}</div>
        </div>
        <div className="summary-card">
          <h3>Current Value</h3>
          <div className="value">{formatCurrency(summary.currentValue)}</div>
        </div>
        <div className="summary-card">
          <h3>Total Gain/Loss</h3>
          <div className={`value ${isPositive ? 'gain' : 'loss'}`}>
            {isPositive ? '↑' : '↓'}{formatCurrency(Math.abs(totalGainLoss))}
            <span className="sub" style={{ marginLeft: '0.3rem' }}>({gainLossPercent}%)</span>
          </div>
        </div>
      </div>

      {/* Filter */}
      <div className="filters" style={{ marginBottom: '1rem' }}>
        <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Type:</span>
        <select value={selectedType} onChange={(e) => setSelectedType(e.target.value)}>
          <option value="ALL">All Types</option>
          {INVESTMENT_TYPES.map((type) => (
            <option key={type.value} value={type.value}>{type.label}</option>
          ))}
        </select>
      </div>

      {/* Investments Table */}
      {loading ? (
        <div className="loading">Loading investments...</div>
      ) : investments.length === 0 ? (
        <div className="empty-state"><p>No investments found. Start by adding your first investment!</p></div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th style={{ textAlign: 'right' }}>Invested</th>
                <th style={{ textAlign: 'right' }}>Current Value</th>
                <th style={{ textAlign: 'right' }}>Gain/Loss</th>
                <th>Entry Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {investments.map((investment) => {
                const gainLoss = investment.currentValue - investment.investedAmount;
                const glPercent = investment.investedAmount > 0
                  ? ((gainLoss / investment.investedAmount) * 100).toFixed(2)
                  : 0;
                const isGain = gainLoss >= 0;

                return (
                  <tr key={investment.id}>
                    <td style={{ fontWeight: 500 }}>{investment.name}</td>
                    <td>{getInvestmentTypeLabel(investment.type)}</td>
                    <td style={{ textAlign: 'right' }}>{formatCurrency(investment.investedAmount)}</td>
                    <td style={{ textAlign: 'right', fontWeight: 600 }}>{formatCurrency(investment.currentValue)}</td>
                    <td style={{ textAlign: 'right', color: isGain ? '#16a34a' : '#dc2626', fontWeight: 600 }}>
                      {isGain ? '↑' : '↓'}{formatCurrency(Math.abs(gainLoss))}
                      <span style={{ fontSize: '0.72rem', opacity: 0.8, marginLeft: '0.2rem' }}>({glPercent}%)</span>
                    </td>
                    <td>{formatDate(investment.entryDate)}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <button className="btn btn-secondary btn-sm"
                        onClick={() => navigate(`/app/investments/${investment.id}/edit`)}>Edit</button>
                      <button className="btn btn-danger btn-sm"
                        onClick={() => handleDelete(investment.id)}>Delete</button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
