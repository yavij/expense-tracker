import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  getInvestment,
  createInvestment,
  updateInvestment,
} from '../api/client';
import { INVESTMENT_TYPES, getInvestmentTypeLabel } from '../constants/investments';


export default function InvestmentForm() {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEditMode = !!id;

  // Form state
  const [formData, setFormData] = useState({
    type: '',
    name: '',
    investedAmount: '',
    currentValue: '',
    units: '',
    navPrice: '',
    entryDate: '',
    notes: '',
  });

  const [loading, setLoading] = useState(isEditMode);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // Fetch investment if in edit mode
  useEffect(() => {
    if (!isEditMode) {
      setLoading(false);
      return;
    }

    let cancelled = false;

    const fetchInvestment = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await getInvestment(id);
        if (!cancelled) {
          setFormData({
            type: data.type || '',
            name: data.name || '',
            investedAmount: data.investedAmount || '',
            currentValue: data.currentValue || '',
            units: data.units || '',
            navPrice: data.navPrice || '',
            entryDate: data.entryDate ? data.entryDate.split('T')[0] : '',
            notes: data.notes || '',
          });
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || 'Failed to load investment');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchInvestment();

    return () => {
      cancelled = true;
    };
  }, [id, isEditMode]);

  // Handle form field changes
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validation
    if (
      !formData.type ||
      !formData.name ||
      !formData.investedAmount ||
      !formData.currentValue ||
      !formData.entryDate
    ) {
      setError('Please fill in all required fields');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);

      const payload = {
        type: formData.type,
        name: formData.name,
        investedAmount: parseFloat(formData.investedAmount),
        currentValue: parseFloat(formData.currentValue),
        entryDate: formData.entryDate,
      };

      // Add optional fields only if they have values
      if (formData.units) {
        payload.units = parseFloat(formData.units);
      }
      if (formData.navPrice) {
        payload.navPrice = parseFloat(formData.navPrice);
      }
      if (formData.notes) {
        payload.notes = formData.notes;
      }

      if (isEditMode) {
        await updateInvestment(id, payload);
      } else {
        await createInvestment(payload);
      }

      navigate('/app/investments');
    } catch (err) {
      setError(err.message || 'Failed to save investment');
      setSubmitting(false);
    }
  };

  // Check if type requires units and nav price fields
  const shouldShowUnitsNav =
    formData.type === 'MF' || formData.type === 'STOCKS';

  const pageTitle = isEditMode ? 'Edit Investment' : 'Add New Investment';

  return (
    <div className="investment-form-container">
      <div className="form-header">
        <h1>{pageTitle}</h1>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {loading ? (
        <div className="loading-spinner">Loading investment...</div>
      ) : (
        <form onSubmit={handleSubmit} className="investment-form">
          {/* Type */}
          <div className="form-group">
            <label htmlFor="type">
              Investment Type <span className="required">*</span>
            </label>
            <select
              id="type"
              name="type"
              value={formData.type}
              onChange={handleChange}
              className="form-control"
              required
            >
              <option value="">Select a type...</option>
              {INVESTMENT_TYPES.map((type) => (
                <option key={type.value} value={type.value}>
                  {type.label}
                </option>
              ))}
            </select>
          </div>

          {/* Name */}
          <div className="form-group">
            <label htmlFor="name">
              Investment Name <span className="required">*</span>
            </label>
            <input
              id="name"
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              placeholder="e.g., ICICI Prudential Growth"
              className="form-control"
              required
            />
          </div>

          {/* Invested Amount */}
          <div className="form-group">
            <label htmlFor="investedAmount">
              Invested Amount (₹) <span className="required">*</span>
            </label>
            <input
              id="investedAmount"
              type="number"
              name="investedAmount"
              value={formData.investedAmount}
              onChange={handleChange}
              placeholder="0.00"
              className="form-control"
              step="0.01"
              min="0"
              required
            />
          </div>

          {/* Current Value */}
          <div className="form-group">
            <label htmlFor="currentValue">
              Current Value (₹) <span className="required">*</span>
            </label>
            <input
              id="currentValue"
              type="number"
              name="currentValue"
              value={formData.currentValue}
              onChange={handleChange}
              placeholder="0.00"
              className="form-control"
              step="0.01"
              min="0"
              required
            />
          </div>

          {/* Units (conditional) */}
          {shouldShowUnitsNav && (
            <div className="form-group">
              <label htmlFor="units">Units</label>
              <input
                id="units"
                type="number"
                name="units"
                value={formData.units}
                onChange={handleChange}
                placeholder="e.g., 100.5"
                className="form-control"
                step="0.01"
                min="0"
              />
            </div>
          )}

          {/* NAV Price (conditional) */}
          {shouldShowUnitsNav && (
            <div className="form-group">
              <label htmlFor="navPrice">NAV Price (₹)</label>
              <input
                id="navPrice"
                type="number"
                name="navPrice"
                value={formData.navPrice}
                onChange={handleChange}
                placeholder="0.00"
                className="form-control"
                step="0.01"
                min="0"
              />
            </div>
          )}

          {/* Entry Date */}
          <div className="form-group">
            <label htmlFor="entryDate">
              Entry Date <span className="required">*</span>
            </label>
            <input
              id="entryDate"
              type="date"
              name="entryDate"
              value={formData.entryDate}
              onChange={handleChange}
              className="form-control"
              required
            />
          </div>

          {/* Notes */}
          <div className="form-group">
            <label htmlFor="notes">Notes</label>
            <textarea
              id="notes"
              name="notes"
              value={formData.notes}
              onChange={handleChange}
              placeholder="Add any additional notes..."
              className="form-control form-textarea"
              rows="4"
            />
          </div>

          {/* Form Actions */}
          <div className="form-actions">
            <button
              type="submit"
              disabled={submitting}
              className="btn btn-primary btn-submit"
            >
              {submitting ? 'Saving...' : isEditMode ? 'Update Investment' : 'Add Investment'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/app/investments')}
              className="btn btn-secondary btn-cancel"
              disabled={submitting}
            >
              Cancel
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
