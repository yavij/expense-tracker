import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  getDebt,
  createDebt,
  updateDebt,
} from '../api/client';
import {
  DEBT_TYPES,
  DEBT_STATUS,
  getDebtTypeLabel,
  getDebtStatusLabel,
} from '../constants/debts';


export default function DebtForm() {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEditMode = !!id;

  const [formData, setFormData] = useState({
    name: '',
    type: '',
    principalAmount: '',
    interestRate: '',
    emiAmount: '',
    tenure: '',
    remainingBalance: '',
    startDate: '',
    endDate: '',
    priority: 0,
    status: 'ACTIVE',
  });

  const [loading, setLoading] = useState(isEditMode);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Fetch debt if in edit mode
  useEffect(() => {
    if (!isEditMode) return;

    let cancelled = false;

    const fetchDebt = async () => {
      try {
        setLoading(true);
        setError(null);
        const debt = await getDebt(id);
        if (!cancelled) {
          setFormData({
            name: debt.name || '',
            type: debt.type || '',
            principalAmount: debt.principalAmount || '',
            interestRate: debt.interestRate || '',
            emiAmount: debt.emiAmount || '',
            tenure: debt.tenure || '',
            remainingBalance: debt.remainingBalance || '',
            startDate: debt.startDate || '',
            endDate: debt.endDate || '',
            priority: debt.priority || 0,
            status: debt.status || 'ACTIVE',
          });
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || 'Failed to fetch debt details');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchDebt();

    return () => {
      cancelled = true;
    };
  }, [id, isEditMode]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const calculateEMI = () => {
    const principal = parseFloat(formData.principalAmount) || 0;
    const annualRate = parseFloat(formData.interestRate) || 0;
    const months = parseInt(formData.tenure, 10) || 0;

    if (principal <= 0 || annualRate < 0 || months <= 0) {
      setError('Please enter valid principal amount, interest rate, and tenure (months)');
      return;
    }

    const monthlyRate = annualRate / 12 / 100;
    let emi;

    if (monthlyRate === 0) {
      emi = principal / months;
    } else {
      const numerator = monthlyRate * Math.pow(1 + monthlyRate, months);
      const denominator = Math.pow(1 + monthlyRate, months) - 1;
      emi = principal * (numerator / denominator);
    }

    setFormData((prev) => ({
      ...prev,
      emiAmount: emi.toFixed(2),
    }));
    setError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validation
    if (!formData.name.trim()) {
      setError('Debt name is required');
      return;
    }
    if (!formData.type) {
      setError('Debt type is required');
      return;
    }
    if (!formData.principalAmount || parseFloat(formData.principalAmount) <= 0) {
      setError('Principal amount must be greater than 0');
      return;
    }
    if (formData.interestRate && parseFloat(formData.interestRate) < 0) {
      setError('Interest rate cannot be negative');
      return;
    }
    if (!formData.startDate) {
      setError('Start date is required');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);

      const data = {
        name: formData.name.trim(),
        type: formData.type,
        principalAmount: parseFloat(formData.principalAmount),
        interestRate: parseFloat(formData.interestRate) || 0,
        emiAmount: parseFloat(formData.emiAmount) || 0,
        remainingBalance: parseFloat(formData.remainingBalance) || parseFloat(formData.principalAmount),
        startDate: formData.startDate,
        endDate: formData.endDate || null,
        priority: parseInt(formData.priority, 10) || 0,
        status: formData.status,
      };

      if (isEditMode) {
        await updateDebt(id, data);
      } else {
        await createDebt(data);
      }

      navigate('/app/debts');
    } catch (err) {
      setError(err.message || 'Failed to save debt');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    navigate('/app/debts');
  };

  if (loading) {
    return (
      <div className="debt-form-container">
        <p>Loading...</p>
      </div>
    );
  }

  return (
    <div className="debt-form-container">
      <div className="form-header">
        <h1>{isEditMode ? 'Edit Debt' : 'Add New Debt'}</h1>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <form className="debt-form" onSubmit={handleSubmit}>
        <div className="form-section">
          <div className="form-group">
            <label htmlFor="name">Debt Name *</label>
            <input
              id="name"
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              placeholder="e.g., Home Loan"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="type">Debt Type *</label>
            <select
              id="type"
              name="type"
              value={formData.type}
              onChange={handleChange}
              required
            >
              <option value="">Select a debt type</option>
              {DEBT_TYPES.map((type) => (
                <option key={type.value} value={type.value}>
                  {type.label}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="principalAmount">Principal Amount (₹) *</label>
            <input
              id="principalAmount"
              type="number"
              name="principalAmount"
              value={formData.principalAmount}
              onChange={handleChange}
              placeholder="0.00"
              step="0.01"
              min="0"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="remainingBalance">Remaining Balance (₹)</label>
            <input
              id="remainingBalance"
              type="number"
              name="remainingBalance"
              value={formData.remainingBalance}
              onChange={handleChange}
              placeholder="0.00"
              step="0.01"
              min="0"
            />
            <small>Defaults to principal amount if not specified</small>
          </div>
        </div>

        <div className="form-section">
          <div className="form-group">
            <label htmlFor="interestRate">Interest Rate (%) </label>
            <input
              id="interestRate"
              type="number"
              name="interestRate"
              value={formData.interestRate}
              onChange={handleChange}
              placeholder="0.00"
              step="0.01"
              min="0"
            />
          </div>

          <div className="form-group">
            <label htmlFor="tenure">Tenure (months)</label>
            <input
              id="tenure"
              type="number"
              name="tenure"
              value={formData.tenure}
              onChange={handleChange}
              placeholder="0"
              step="1"
              min="0"
            />
          </div>

          <div className="form-group">
            <label htmlFor="emiAmount">EMI Amount (₹)</label>
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-end' }}>
              <input
                id="emiAmount"
                type="number"
                name="emiAmount"
                value={formData.emiAmount}
                onChange={handleChange}
                placeholder="0.00"
                step="0.01"
                min="0"
                style={{ flex: 1 }}
              />
              <button
                type="button"
                onClick={calculateEMI}
                style={{
                  padding: '0.5rem 1rem',
                  backgroundColor: 'var(--accent-color)',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  fontSize: '0.9rem',
                }}
              >
                Calculate
              </button>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="priority">Priority (Lower = Higher Priority)</label>
            <input
              id="priority"
              type="number"
              name="priority"
              value={formData.priority}
              onChange={handleChange}
              placeholder="0"
              min="0"
            />
          </div>

          <div className="form-group">
            <label htmlFor="status">Status *</label>
            <select
              id="status"
              name="status"
              value={formData.status}
              onChange={handleChange}
              required
            >
              {DEBT_STATUS.map((status) => (
                <option key={status.value} value={status.value}>
                  {status.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="form-section">
          <div className="form-group">
            <label htmlFor="startDate">Start Date *</label>
            <input
              id="startDate"
              type="date"
              name="startDate"
              value={formData.startDate}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="endDate">End Date (Optional)</label>
            <input
              id="endDate"
              type="date"
              name="endDate"
              value={formData.endDate}
              onChange={handleChange}
            />
          </div>
        </div>

        <div className="form-actions">
          <button
            type="submit"
            className="btn btn-primary"
            disabled={submitting}
          >
            {submitting
              ? 'Saving...'
              : isEditMode
                ? 'Update Debt'
                : 'Create Debt'}
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={handleCancel}
            disabled={submitting}
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
