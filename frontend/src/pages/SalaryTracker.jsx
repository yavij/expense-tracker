import React, { useState, useEffect } from 'react';
import {
  getSalaryEntries,
  getSalaryEntry,
  getSalaryHistory,
  createSalaryEntry,
  updateSalaryEntry,
  deleteSalaryEntry,
  exportCsv,
} from '../api/client';


const SalaryTracker = () => {
  // Form state
  const [month, setMonth] = useState('');
  const [grossAmount, setGrossAmount] = useState('');
  const [deductions, setDeductions] = useState('');
  const [netAmount, setNetAmount] = useState('');
  const [notes, setNotes] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [errors, setErrors] = useState({});

  // Data state
  const [salaryEntries, setSalaryEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // Fetch salary history on mount
  useEffect(() => {
    let cancelled = false;

    const fetchData = async () => {
      try {
        setLoading(true);
        const data = await getSalaryHistory();
        if (!cancelled) {
          setSalaryEntries(data.sort((a, b) => new Date(b.month) - new Date(a.month)));
        }
      } catch (error) {
        console.error('Failed to fetch salary history:', error);
        if (!cancelled) {
          setSalaryEntries([]);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchData();

    return () => {
      cancelled = true;
    };
  }, []);

  // Auto-calculate net amount
  useEffect(() => {
    if (grossAmount && deductions !== '') {
      const gross = parseFloat(grossAmount) || 0;
      const ded = parseFloat(deductions) || 0;
      const net = gross - ded;
      setNetAmount(net.toString());
    }
  }, [grossAmount, deductions]);

  // Validate form
  const validateForm = () => {
    const newErrors = {};

    if (!month) {
      newErrors.month = 'Month is required';
    }

    if (!grossAmount || isNaN(parseFloat(grossAmount)) || parseFloat(grossAmount) < 0) {
      newErrors.grossAmount = 'Valid gross amount is required';
    }

    if (deductions === '' || isNaN(parseFloat(deductions)) || parseFloat(deductions) < 0) {
      newErrors.deductions = 'Valid deductions amount is required';
    }

    if (netAmount === '' || isNaN(parseFloat(netAmount))) {
      newErrors.netAmount = 'Valid net amount is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      setSubmitting(true);
      const data = {
        month: new Date(month + '-01').toISOString(),
        grossAmount: parseFloat(grossAmount),
        deductions: parseFloat(deductions),
        netAmount: parseFloat(netAmount),
        notes: notes || null,
      };

      if (editingId) {
        await updateSalaryEntry(editingId, data);
      } else {
        await createSalaryEntry(data);
      }

      // Refresh data
      const updatedData = await getSalaryHistory();
      setSalaryEntries(updatedData.sort((a, b) => new Date(b.month) - new Date(a.month)));

      // Reset form
      resetForm();
    } catch (error) {
      console.error('Failed to save salary entry:', error);
      setErrors({ submit: 'Failed to save entry. Please try again.' });
    } finally {
      setSubmitting(false);
    }
  };

  // Reset form
  const resetForm = () => {
    setMonth('');
    setGrossAmount('');
    setDeductions('');
    setNetAmount('');
    setNotes('');
    setEditingId(null);
    setErrors({});
  };

  // Handle edit
  const handleEdit = (entry) => {
    const monthDate = new Date(entry.month);
    const monthString = monthDate.toISOString().split('T')[0].slice(0, 7);

    setMonth(monthString);
    setGrossAmount(entry.grossAmount.toString());
    setDeductions(entry.deductions.toString());
    setNetAmount(entry.netAmount.toString());
    setNotes(entry.notes || '');
    setEditingId(entry.id);
    setErrors({});

    // Scroll to form
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Handle delete
  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this entry?')) {
      try {
        await deleteSalaryEntry(id);

        // Refresh data
        const updatedData = await getSalaryHistory();
        setSalaryEntries(updatedData.sort((a, b) => new Date(b.month) - new Date(a.month)));
      } catch (error) {
        console.error('Failed to delete salary entry:', error);
        setErrors({ submit: 'Failed to delete entry. Please try again.' });
      }
    }
  };

  // Format date
  const formatMonth = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', { month: 'short', year: 'numeric' });
  };

  // Format currency
  const formatCurrency = (amount) => {
    return `₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  // Get last 12 months data for chart
  const getChartData = () => {
    const last12 = salaryEntries.slice(0, 12).reverse();
    return last12;
  };

  // Get max value for chart scaling
  const getMaxNetAmount = () => {
    if (salaryEntries.length === 0) return 100000;
    return Math.max(...salaryEntries.map((entry) => entry.netAmount));
  };

  const maxValue = getMaxNetAmount();
  const chartData = getChartData();

  return (
    <div className="salary-tracker">
      <div className="page-header">
        <h2>Salary Tracker</h2>
        <button className="btn" style={{ background: '#059669', color: 'white' }}
          onClick={() => exportCsv('salary')}>Export CSV</button>
      </div>

      {/* Add/Edit Form Section */}
      <div className="form-section">
        <h2>{editingId ? 'Edit Salary Entry' : 'Add New Salary Entry'}</h2>

        {errors.submit && <div className="error">{errors.submit}</div>}

        <form onSubmit={handleSubmit}>
          <div className="salary-form">
            <div className="form-group">
              <label htmlFor="month">Month</label>
              <input
                type="month"
                id="month"
                value={month}
                onChange={(e) => {
                  setMonth(e.target.value);
                  if (errors.month) {
                    const newErrors = { ...errors };
                    delete newErrors.month;
                    setErrors(newErrors);
                  }
                }}
                className={errors.month ? 'input-error' : ''}
              />
              {errors.month && <span className="error" style={{ marginTop: '0.2rem' }}>{errors.month}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="grossAmount">Gross Amount (₹)</label>
              <input
                type="number"
                id="grossAmount"
                value={grossAmount}
                onChange={(e) => {
                  setGrossAmount(e.target.value);
                  if (errors.grossAmount) {
                    const newErrors = { ...errors };
                    delete newErrors.grossAmount;
                    setErrors(newErrors);
                  }
                }}
                placeholder="0.00"
                step="0.01"
                min="0"
                className={errors.grossAmount ? 'input-error' : ''}
              />
              {errors.grossAmount && <span className="error" style={{ marginTop: '0.2rem' }}>{errors.grossAmount}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="deductions">Deductions (₹)</label>
              <input
                type="number"
                id="deductions"
                value={deductions}
                onChange={(e) => {
                  setDeductions(e.target.value);
                  if (errors.deductions) {
                    const newErrors = { ...errors };
                    delete newErrors.deductions;
                    setErrors(newErrors);
                  }
                }}
                placeholder="0.00"
                step="0.01"
                min="0"
                className={errors.deductions ? 'input-error' : ''}
              />
              {errors.deductions && <span className="error" style={{ marginTop: '0.2rem' }}>{errors.deductions}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="netAmount">Net Amount (₹)</label>
              <input
                type="number"
                id="netAmount"
                value={netAmount}
                onChange={(e) => {
                  setNetAmount(e.target.value);
                  if (errors.netAmount) {
                    const newErrors = { ...errors };
                    delete newErrors.netAmount;
                    setErrors(newErrors);
                  }
                }}
                placeholder="0.00"
                step="0.01"
                min="0"
              />
              {errors.netAmount && <span className="error" style={{ marginTop: '0.2rem' }}>{errors.netAmount}</span>}
            </div>
          </div>

          <div className="form-group" style={{ marginTop: '0.5rem' }}>
            <label htmlFor="notes">Notes</label>
            <textarea
              id="notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Add any notes (optional)"
              rows="2"
            />
          </div>

          <div className="form-actions">
            <button type="submit" disabled={submitting} className="btn btn-primary">
              {submitting ? 'Saving...' : editingId ? 'Update Entry' : 'Add Entry'}
            </button>
            {editingId && (
              <button type="button" onClick={resetForm} className="btn btn-secondary">
                Cancel Edit
              </button>
            )}
          </div>
        </form>
      </div>

      {/* Salary History Table */}
      <div style={{ marginTop: '1.5rem' }}>
        <h2 style={{ marginBottom: '0.6rem' }}>Salary History</h2>

        {loading ? (
          <div className="loading">Loading salary history...</div>
        ) : salaryEntries.length === 0 ? (
          <div className="empty-state">
            <p>No salary entries yet. Add one to get started!</p>
          </div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Month</th>
                  <th style={{ textAlign: 'right' }}>Gross</th>
                  <th style={{ textAlign: 'right' }}>Deductions</th>
                  <th style={{ textAlign: 'right' }}>Net</th>
                  <th>Notes</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {salaryEntries.map((entry) => (
                  <tr key={entry.id}>
                    <td style={{ fontWeight: 500 }}>{formatMonth(entry.month)}</td>
                    <td style={{ textAlign: 'right' }}>{formatCurrency(entry.grossAmount)}</td>
                    <td style={{ textAlign: 'right', color: 'var(--text-secondary)' }}>{formatCurrency(entry.deductions)}</td>
                    <td style={{ textAlign: 'right', fontWeight: 600 }}>{formatCurrency(entry.netAmount)}</td>
                    <td style={{ color: entry.notes ? 'var(--text-primary)' : 'var(--text-muted)' }}>{entry.notes || '-'}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <button
                        onClick={() => handleEdit(entry)}
                        className="btn btn-secondary btn-sm"
                        title="Edit entry"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(entry.id)}
                        className="btn btn-danger btn-sm"
                        title="Delete entry"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Trend Bar Chart */}
      {chartData.length > 0 && (
        <div className="chart-wrap" style={{ marginTop: '1.5rem' }}>
          <h3>Last 12 Months Net Salary Trend</h3>
          <div className="bar-chart-container">
            {chartData.map((entry, index) => {
              const percentage = (entry.netAmount / (maxValue || 1)) * 100;
              const monthDate = new Date(entry.month);
              const monthAbbr = monthDate.toLocaleDateString('en-IN', { month: 'short' });

              return (
                <div key={entry.id || index} className="bar-col">
                  <div
                    className="bar-fill positive"
                    style={{ height: `${percentage}%` }}
                    title={formatCurrency(entry.netAmount)}
                  >
                    <span className="bar-tooltip">{formatCurrency(entry.netAmount)}</span>
                  </div>
                  <span className="bar-label">{monthAbbr}</span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};

export default SalaryTracker;
