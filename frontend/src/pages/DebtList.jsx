import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getDebts,
  deleteDebt,
  addDebtPayment,
  getDebtPayments,
  exportCsv,
} from '../api/client';
import {
  DEBT_TYPES,
  DEBT_STATUS,
  getDebtTypeLabel,
  getDebtStatusLabel,
} from '../constants/debts';
import { useToast } from '../components/Toast';


export default function DebtList() {
  const navigate = useNavigate();
  const toast = useToast();
  const [debts, setDebts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('All');
  const [expandedPayments, setExpandedPayments] = useState({});
  const [paymentForms, setPaymentForms] = useState({});
  const [payments, setPayments] = useState({});
  const [submittingPayment, setSubmittingPayment] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Fetch debts on mount
  useEffect(() => {
    let cancelled = false;

    const fetchDebts = async () => {
      try {
        setLoading(true);
        setError(null);
        const params = statusFilter !== 'All' ? { status: statusFilter } : {};
        const data = await getDebts(params);
        if (!cancelled) {
          setDebts(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || 'Failed to fetch debts');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchDebts();

    return () => {
      cancelled = true;
    };
  }, [statusFilter]);

  // Calculate summary stats
  const totalRemainingDebt = debts.reduce(
    (sum, debt) => sum + (debt.remainingBalance || 0),
    0
  );
  const activeDebtsCount = debts.filter(
    (debt) => debt.status === 'ACTIVE'
  ).length;
  const monthlyEMITotal = debts.reduce(
    (sum, debt) => sum + (debt.emiAmount || 0),
    0
  );

  // Handle delete with confirmation
  const handleDelete = async (debtId, debtName) => {
    if (!window.confirm(`Are you sure you want to delete "${debtName}"?`)) {
      return;
    }

    try {
      await deleteDebt(debtId);
      setDebts((prev) => prev.filter((d) => d.id !== debtId));
    } catch (err) {
      setError(err.message || 'Failed to delete debt');
    }
  };

  // Toggle payment form
  const togglePaymentForm = (debtId) => {
    setPaymentForms((prev) => ({
      ...prev,
      [debtId]: !prev[debtId],
    }));
  };

  // Handle payment submission
  const handlePaymentSubmit = async (debtId, e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const paymentAmount = parseFloat(formData.get('paymentAmount'));
    const paymentDate = formData.get('paymentDate');
    const notes = formData.get('notes');

    if (!paymentAmount || !paymentDate) {
      toast.warning('Please fill in all required fields');
      return;
    }

    try {
      setSubmittingPayment(debtId);
      await addDebtPayment(debtId, {
        paymentAmount,
        paymentDate,
        notes,
      });

      // Refresh debts and clear form
      const data = await getDebts();
      setDebts(data);
      setPaymentForms((prev) => ({
        ...prev,
        [debtId]: false,
      }));
      e.target.reset();

      // Refresh payments if expanded
      if (expandedPayments[debtId]) {
        const debtPayments = await getDebtPayments(debtId);
        setPayments((prev) => ({
          ...prev,
          [debtId]: debtPayments,
        }));
      }
    } catch (err) {
      toast.error(err.message || 'Failed to add payment');
    } finally {
      setSubmittingPayment(null);
    }
  };

  // Toggle payment history
  const togglePaymentHistory = async (debtId) => {
    if (expandedPayments[debtId]) {
      setExpandedPayments((prev) => ({
        ...prev,
        [debtId]: false,
      }));
    } else {
      try {
        const debtPayments = await getDebtPayments(debtId);
        setPayments((prev) => ({
          ...prev,
          [debtId]: debtPayments,
        }));
        setExpandedPayments((prev) => ({
          ...prev,
          [debtId]: true,
        }));
      } catch (err) {
        toast.error(err.message || 'Failed to fetch payments');
      }
    }
  };

  // Get status badge color
  const getStatusColor = (status) => {
    if (status === 'PAID_OFF') return 'green';
    if (status === 'PAUSED') return 'yellow';
    if (status === 'ACTIVE') return 'red';
    return 'gray';
  };

  if (loading) {
    return <div className="debt-list-container"><p>Loading debts...</p></div>;
  }

  return (
    <div>
      <div className="page-header">
        <h2>Debt Overview</h2>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button className="btn" style={{ background: '#059669', color: 'white' }}
            onClick={() => exportCsv('debts')}>Export CSV</button>
          <button className="btn btn-primary" onClick={() => navigate('/app/debts/new')}>
            + Add Debt
          </button>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      {/* Summary Cards */}
      <div className="summary-cards">
        <div className="summary-card">
          <h3>Total Remaining Debt</h3>
          <div className="value loss">
            ₹{totalRemainingDebt.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
        </div>
        <div className="summary-card">
          <h3>Active Debts</h3>
          <div className="value">{activeDebtsCount}</div>
        </div>
        <div className="summary-card">
          <h3>Monthly EMI Total</h3>
          <div className="value">
            ₹{monthlyEMITotal.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
        </div>
      </div>

      {/* Search + Status Filter */}
      <div className="filters" style={{ marginBottom: '1rem' }}>
        <input type="text" placeholder="Search by name or type..." value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)} title="Search debts" />
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1rem' }}>
        <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Status:</span>
        <div className="strategy-toggle" style={{ marginBottom: 0 }}>
          <button type="button" className={statusFilter === 'All' ? 'active' : ''}
            onClick={() => setStatusFilter('All')}>All</button>
          {DEBT_STATUS.map((status) => (
            <button type="button" key={status.value}
              className={statusFilter === status.value ? 'active' : ''}
              onClick={() => setStatusFilter(status.value)}>
              {status.label}
            </button>
          ))}
        </div>
      </div>

      {/* Debts Table */}
      {debts.length === 0 ? (
        <div className="empty-state"><p>No debts found. Add one to get started!</p></div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th style={{ textAlign: 'right' }}>Principal</th>
                <th style={{ textAlign: 'right' }}>Remaining</th>
                <th style={{ textAlign: 'right' }}>Rate</th>
                <th style={{ textAlign: 'right' }}>EMI</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {debts.filter(debt => {
                if (!searchQuery.trim()) return true;
                const q = searchQuery.toLowerCase();
                return (debt.name || '').toLowerCase().includes(q) ||
                  (debt.type || '').toLowerCase().includes(q);
              }).map((debt) => (
                <React.Fragment key={debt.id}>
                  <tr>
                    <td style={{ fontWeight: 500 }}>{debt.name}</td>
                    <td>{getDebtTypeLabel(debt.type)}</td>
                    <td style={{ textAlign: 'right' }}>
                      ₹{debt.principalAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td style={{ textAlign: 'right', fontWeight: 600 }}>
                      ₹{(debt.remainingBalance || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td style={{ textAlign: 'right' }}>{debt.interestRate}%</td>
                    <td style={{ textAlign: 'right' }}>
                      ₹{(debt.emiAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td>
                      <span className={`badge ${debt.status === 'PAID_OFF' ? 'badge-paid-off' : debt.status === 'PAUSED' ? 'badge-paused' : 'badge-active'}`}>
                        {getDebtStatusLabel(debt.status)}
                      </span>
                    </td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <button className="btn btn-secondary btn-sm"
                        onClick={() => navigate(`/app/debts/${debt.id}/edit`)}>Edit</button>
                      <button className="btn btn-primary btn-sm"
                        onClick={() => togglePaymentForm(debt.id)}>Pay</button>
                      <button className="btn btn-danger btn-sm"
                        onClick={() => handleDelete(debt.id, debt.name)}>Del</button>
                    </td>
                  </tr>

                  {/* Payment Form Row */}
                  {paymentForms[debt.id] && (
                    <tr>
                      <td colSpan="8">
                        <form className="inline-form"
                          onSubmit={(e) => handlePaymentSubmit(debt.id, e)}>
                          <div className="form-group">
                            <label>Amount</label>
                            <input type="number" name="paymentAmount" step="0.01" min="0"
                              placeholder="₹ Amount" required />
                          </div>
                          <div className="form-group">
                            <label>Date</label>
                            <input type="date" name="paymentDate" required />
                          </div>
                          <div className="form-group">
                            <label>Notes</label>
                            <input type="text" name="notes" placeholder="Optional" />
                          </div>
                          <div className="form-actions" style={{ marginTop: 0 }}>
                            <button type="submit" className="btn btn-primary"
                              disabled={submittingPayment === debt.id}>
                              {submittingPayment === debt.id ? 'Saving...' : 'Submit'}
                            </button>
                            <button type="button" className="btn btn-secondary"
                              onClick={() => togglePaymentForm(debt.id)}>Cancel</button>
                          </div>
                        </form>
                      </td>
                    </tr>
                  )}

                  {/* Payment History */}
                  {expandedPayments[debt.id] && payments[debt.id] && payments[debt.id].length > 0 && (
                    <tr>
                      <td colSpan="8" style={{ padding: '0.5rem 1rem', background: 'var(--bg-tertiary)' }}>
                        <strong style={{ fontSize: '0.78rem' }}>Payment History</strong>
                        <table style={{ marginTop: '0.3rem' }}>
                          <thead><tr><th>Amount</th><th>Date</th><th>Notes</th></tr></thead>
                          <tbody>
                            {payments[debt.id].map((p, idx) => (
                              <tr key={idx}>
                                <td>₹{p.paymentAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                                <td>{p.paymentDate}</td>
                                <td style={{ color: p.notes ? 'var(--text-primary)' : 'var(--text-muted)' }}>{p.notes || '-'}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </td>
                    </tr>
                  )}

                  {/* Toggle Payment History */}
                  <tr>
                    <td colSpan="8" style={{ padding: '0.2rem 0.7rem', borderBottom: '2px solid var(--border-color)' }}>
                      <button className="toggle-btn" onClick={() => togglePaymentHistory(debt.id)}>
                        {expandedPayments[debt.id] ? '▾ Hide Payments' : '▸ Show Payments'}
                      </button>
                    </td>
                  </tr>
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
