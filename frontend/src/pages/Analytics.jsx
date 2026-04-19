import React, { useState, useEffect } from 'react';
import { getMonthlyAnalytics, getYearlyAnalytics, getNetworth } from '../api/client';


export default function Analytics() {
  const [activeTab, setActiveTab] = useState('monthly');
  const [monthlyData, setMonthlyData] = useState(null);
  const [yearlyData, setYearlyData] = useState(null);
  const [networthData, setNetworthData] = useState(null);
  const [selectedMonth, setSelectedMonth] = useState(
    new Date().toISOString().slice(0, 7)
  );
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Generate last 5 years
  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: 5 }, (_, i) => currentYear - 4 + i);

  // Fetch monthly analytics
  useEffect(() => {
    if (activeTab !== 'monthly') return;

    const fetchMonthly = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getMonthlyAnalytics(selectedMonth);
        setMonthlyData(data);
      } catch (err) {
        setError('Failed to load monthly analytics');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchMonthly();
  }, [activeTab, selectedMonth]);

  // Fetch yearly analytics
  useEffect(() => {
    if (activeTab !== 'yearly') return;

    const fetchYearly = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getYearlyAnalytics(selectedYear);
        setYearlyData(data);
      } catch (err) {
        setError('Failed to load yearly analytics');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchYearly();
  }, [activeTab, selectedYear]);

  // Fetch networth on mount and when all-time tab is active
  useEffect(() => {
    if (activeTab !== 'alltime') return;

    const fetchNetworth = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getNetworth();
        setNetworthData(data);
      } catch (err) {
        setError('Failed to load net worth data');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchNetworth();
  }, [activeTab]);

  return (
    <div className="analytics-container">
      <h1>Analytics Dashboard</h1>

      {/* Tab Navigation */}
      <div className="tab-navigation">
        <button
          className={`tab-btn ${activeTab === 'monthly' ? 'active' : ''}`}
          onClick={() => setActiveTab('monthly')}
        >
          Monthly
        </button>
        <button
          className={`tab-btn ${activeTab === 'yearly' ? 'active' : ''}`}
          onClick={() => setActiveTab('yearly')}
        >
          Yearly
        </button>
        <button
          className={`tab-btn ${activeTab === 'alltime' ? 'active' : ''}`}
          onClick={() => setActiveTab('alltime')}
        >
          All-Time
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* MONTHLY TAB */}
      {activeTab === 'monthly' && (
        <div className="tab-content monthly-tab">
          <div className="month-selector">
            <label htmlFor="month-input">Select Month:</label>
            <input
              id="month-input"
              type="month"
              value={selectedMonth}
              onChange={(e) => setSelectedMonth(e.target.value)}
            />
          </div>

          {loading ? (
            <div className="loading">Loading...</div>
          ) : monthlyData ? (
            (() => {
              const salary = typeof monthlyData.salary === 'object' ? (monthlyData.salary?.net ?? 0) : (monthlyData.salary ?? 0);
              const totalExp = monthlyData.expenses ?? monthlyData.totalExpenses ?? 0;
              const savings = monthlyData.netChange ?? monthlyData.savings ?? (salary - totalExp);
              const savingsRate = salary > 0 ? (savings / salary) * 100 : 0;
              const breakdown = monthlyData.expenseBreakdown ?? monthlyData.categoryBreakdown ?? [];
              const breakdownTotal = breakdown.reduce((s, c) => s + (c.amount ?? 0), 0) || 1;
              return (
                <>
                  <div className="cards-grid">
                    <div className="info-card">
                      <div className="card-label">Salary (Net)</div>
                      <div className="card-value">
                        ₹{Number(salary).toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                      </div>
                    </div>
                    <div className="info-card">
                      <div className="card-label">Total Expenses</div>
                      <div className="card-value">
                        ₹{Number(totalExp).toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                      </div>
                    </div>
                    <div className={`info-card ${savings >= 0 ? 'positive' : 'negative'}`}>
                      <div className="card-label">Net Change</div>
                      <div className="card-value">
                        ₹{Number(savings).toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                      </div>
                    </div>
                    <div className={`info-card ${savingsRate >= 0 ? 'positive' : 'negative'}`}>
                      <div className="card-label">Savings Rate</div>
                      <div className="card-value">{savingsRate.toFixed(1)}%</div>
                    </div>
                  </div>

                  {breakdown.length > 0 && (
                    <section className="chart-section">
                      <h2>Expense Breakdown by Category</h2>
                      <div className="horizontal-bars">
                        {breakdown.map((cat, idx) => {
                          const pct = cat.percentage ?? ((cat.amount / breakdownTotal) * 100);
                          return (
                            <div key={idx} className="bar-item">
                              <div className="bar-label">
                                <span className="category-name">{cat.category}</span>
                                <span className="bar-value">
                                  ₹{Number(cat.amount).toLocaleString('en-IN', { maximumFractionDigits: 0 })} ({pct.toFixed(1)}%)
                                </span>
                              </div>
                              <div className="bar-container">
                                <div className="bar" style={{ width: `${pct}%` }}></div>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </section>
                  )}
                  {breakdown.length > 0 && (
                    <section className="chart-section">
                      <h2>Expense Distribution</h2>
                      <DonutChart data={breakdown.map(c => ({ category: c.category, amount: c.amount }))} />
                    </section>
                  )}
                  {breakdown.length === 0 && <div className="no-data">No expense data for this month</div>}
                </>
              );
            })()
          ) : null}
        </div>
      )}

      {/* YEARLY TAB */}
      {activeTab === 'yearly' && (
        <div className="tab-content yearly-tab">
          <div className="year-selector">
            <label htmlFor="year-select">Select Year:</label>
            <select
              id="year-select"
              value={selectedYear}
              onChange={(e) => setSelectedYear(parseInt(e.target.value, 10))}
            >
              {years.map((year) => (
                <option key={year} value={year}>
                  {year}
                </option>
              ))}
            </select>
          </div>

          {loading ? (
            <div className="loading">Loading...</div>
          ) : yearlyData ? (
            (() => {
              const totalIncome = Number(yearlyData.totalSalary ?? yearlyData.totalIncome ?? 0);
              const totalExp = Number(yearlyData.totalExpenses ?? 0);
              const totalInv = Number(yearlyData.totalInvestments ?? 0);
              const totalSaved = totalIncome - totalExp - totalInv;
              const avgMonthly = totalSaved / 12;
              const trend = yearlyData.monthlyTrend ?? [];
              const salaryTrend = trend.map(t => Number(t.salary ?? t ?? 0));
              const savingsTrend = trend.map(t => Number(t.savings ?? 0));
              return (
                <>
                  <div className="cards-grid">
                    <div className="info-card">
                      <div className="card-label">Total Income</div>
                      <div className="card-value">
                        ₹{totalIncome.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                      </div>
                    </div>
                    <div className="info-card">
                      <div className="card-label">Total Expenses</div>
                      <div className="card-value">
                        ₹{totalExp.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                      </div>
                    </div>
                    <div className={`info-card ${totalSaved >= 0 ? 'positive' : 'negative'}`}>
                      <div className="card-label">Total Saved</div>
                      <div className="card-value">
                        ₹{totalSaved.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                      </div>
                    </div>
                    <div className="info-card">
                      <div className="card-label">Avg Monthly Savings</div>
                      <div className="card-value">
                        ₹{avgMonthly.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                      </div>
                    </div>
                  </div>

                  {salaryTrend.length > 0 && (
                    <section className="chart-section">
                      <h2>12-Month Salary Trend</h2>
                      <SalaryTrendChart data={salaryTrend} />
                    </section>
                  )}

                  {savingsTrend.length > 0 && (
                    <section className="chart-section">
                      <h2>Monthly Savings Trend</h2>
                      <MonthlyBarsChart data={savingsTrend} />
                    </section>
                  )}

                  {savingsTrend.length > 0 && (
                    <section className="chart-section">
                      <h2>Cumulative Savings</h2>
                      <CumulativeSavingsChart data={savingsTrend.reduce((acc, val) => {
                        const prev = acc.length > 0 ? acc[acc.length - 1] : 0;
                        acc.push(prev + val);
                        return acc;
                      }, [])} />
                    </section>
                  )}
                </>
              );
            })()
          ) : null}
        </div>
      )}

      {/* ALL-TIME TAB */}
      {activeTab === 'alltime' && (
        <div className="tab-content alltime-tab">
          {loading ? (
            <div className="loading">Loading...</div>
          ) : networthData ? (
            (() => {
              const netWorth = Number(networthData.netWorth ?? 0);
              const totalInvested = Number(networthData.investmentValue ?? networthData.totalInvested ?? 0);
              const totalDebt = Number(networthData.totalDebt ?? 0);
              const maxVal = Math.max(totalInvested, totalDebt, 1);
              return (
                <>
                  <div className="networth-card">
                    <div className="card-label">Current Net Worth</div>
                    <div className="card-value large">
                      ₹{netWorth.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                    </div>
                  </div>

                  <section className="chart-section">
                    <h2>Investments vs Remaining Debt</h2>
                    <div className="comparison-bars">
                      <div className="comparison-item">
                        <div className="comp-label">Total Investments</div>
                        <div className="comp-value">
                          ₹{totalInvested.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                        </div>
                        <div className="comp-bar-container">
                          <div className="comp-bar investments" style={{ width: `${(totalInvested / maxVal) * 100}%` }}></div>
                        </div>
                      </div>
                      <div className="comparison-item">
                        <div className="comp-label">Total Debt</div>
                        <div className="comp-value">
                          ₹{totalDebt.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                        </div>
                        <div className="comp-bar-container">
                          <div className="comp-bar debt" style={{ width: `${(totalDebt / maxVal) * 100}%` }}></div>
                        </div>
                      </div>
                    </div>
                  </section>

                  {networthData.investmentBreakdown && networthData.investmentBreakdown.length > 0 && (
                    <section className="chart-section">
                      <h2>Investment Breakdown by Type</h2>
                      <div className="horizontal-bars">
                        {networthData.investmentBreakdown.map((item, idx) => {
                          const pct = totalInvested > 0 ? (Number(item.value) / totalInvested) * 100 : 0;
                          return (
                            <div key={idx} className="bar-item">
                              <div className="bar-label">
                                <span className="category-name">{item.type}</span>
                                <span className="bar-value">
                                  ₹{Number(item.value).toLocaleString('en-IN', { maximumFractionDigits: 0 })} ({pct.toFixed(1)}%)
                                </span>
                              </div>
                              <div className="bar-container">
                                <div className="bar" style={{ width: `${pct}%` }}></div>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </section>
                  )}
                </>
              );
            })()
          ) : null}
        </div>
      )}
    </div>
  );
}

// SVG Chart Components

function SalaryTrendChart({ data }) {
  if (!data || data.length === 0) {
    return <div className="no-data">No data available</div>;
  }

  const months = data.map((_, idx) => {
    const date = new Date(new Date().getFullYear(), idx, 1);
    return date.toLocaleDateString('en-IN', { month: 'short' });
  });

  const values = data;
  const maxValue = Math.max(...values) || 1;
  const padding = 50;
  const width = 900;
  const height = 300;

  // Calculate points for polyline
  const points = values
    .map((val, idx) => {
      const x = padding + (idx / (values.length - 1 || 1)) * (width - 2 * padding);
      const y = height - padding - (val / maxValue) * (height - 2 * padding);
      return `${x},${y}`;
    })
    .join(' ');

  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="chart-svg">
      {/* Grid lines */}
      {[0, 0.25, 0.5, 0.75, 1].map((ratio, idx) => {
        const y = height - padding - ratio * (height - 2 * padding);
        return (
          <line
            key={`grid-${idx}`}
            x1={padding}
            y1={y}
            x2={width - padding}
            y2={y}
            stroke="var(--border-color)"
            strokeWidth="1"
          />
        );
      })}

      {/* Y-axis */}
      <line x1={padding} y1={padding} x2={padding} y2={height - padding} stroke="var(--text-secondary)" strokeWidth="2" />

      {/* X-axis */}
      <line x1={padding} y1={height - padding} x2={width - padding} y2={height - padding} stroke="var(--text-secondary)" strokeWidth="2" />

      {/* Polyline */}
      <polyline points={points} fill="none" stroke="#4CAF50" strokeWidth="3" className="chart-line" />

      {/* Data points */}
      {values.map((val, idx) => {
        const x = padding + (idx / (values.length - 1 || 1)) * (width - 2 * padding);
        const y = height - padding - (val / maxValue) * (height - 2 * padding);
        return <circle key={`dot-${idx}`} cx={x} cy={y} r="4" fill="#4CAF50" />;
      })}

      {/* X-axis labels */}
      {months.map((month, idx) => {
        const x = padding + (idx / (values.length - 1 || 1)) * (width - 2 * padding);
        return (
          <text
            key={`label-${idx}`}
            x={x}
            y={height - 20}
            textAnchor="middle"
            fontSize="12"
            fill="var(--text-secondary)"
          >
            {month}
          </text>
        );
      })}

      {/* Y-axis labels */}
      {[0, 0.25, 0.5, 0.75, 1].map((ratio, idx) => {
        const y = height - padding - ratio * (height - 2 * padding);
        const value = Math.round(ratio * maxValue);
        return (
          <text key={`y-label-${idx}`} x={padding - 10} y={y + 5} textAnchor="end" fontSize="12" fill="var(--text-secondary)">
            ₹{(value / 100000).toFixed(1)}L
          </text>
        );
      })}
    </svg>
  );
}

function MonthlyBarsChart({ data }) {
  if (!data || data.length === 0) {
    return <div className="no-data">No data available</div>;
  }

  const months = data.map((_, idx) => {
    const date = new Date(new Date().getFullYear(), idx, 1);
    return date.toLocaleDateString('en-IN', { month: 'short' });
  });

  const maxValue = Math.max(...data.map(Math.abs)) || 1;

  return (
    <div className="vertical-bars">
      {data.map((amount, idx) => (
        <div key={idx} className="bar-item vertical">
          <div className={`bar-value ${amount >= 0 ? 'positive' : 'negative'}`}>
            ₹{(amount / 1000).toFixed(0)}K
          </div>
          <div className="bar-container vertical">
            <div
              className={`bar vertical ${amount >= 0 ? 'positive' : 'negative'}`}
              style={{
                height: `${(Math.abs(amount) / maxValue) * 100}%`,
              }}
            ></div>
          </div>
          <div className="bar-label">{months[idx]}</div>
        </div>
      ))}
    </div>
  );
}

function DonutChart({ data }) {
  if (!data || data.length === 0) {
    return <div className="no-data">No data available</div>;
  }

  const total = data.reduce((sum, item) => sum + item.amount, 0);
  const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8'];

  let cumulativePercentage = 0;
  const segments = data.map((item, idx) => {
    const percentage = (item.amount / total) * 100;
    const startPercentage = cumulativePercentage;
    cumulativePercentage += percentage;

    const startAngle = (startPercentage / 100) * 360;
    const endAngle = (cumulativePercentage / 100) * 360;

    return { ...item, percentage, startAngle, endAngle, color: colors[idx % colors.length] };
  });

  const radius = 80;
  const circumference = 2 * Math.PI * radius;

  return (
    <div className="donut-chart-container">
      <svg viewBox="0 0 200 200" className="donut-svg">
        <circle cx="100" cy="100" r="70" fill="none" stroke="var(--border-color)" strokeWidth="30" />
        {segments.map((seg, idx) => {
          const startRad = (seg.startAngle * Math.PI) / 180;
          const endRad = (seg.endAngle * Math.PI) / 180;

          const x1 = 100 + radius * Math.cos(startRad - Math.PI / 2);
          const y1 = 100 + radius * Math.sin(startRad - Math.PI / 2);
          const x2 = 100 + radius * Math.cos(endRad - Math.PI / 2);
          const y2 = 100 + radius * Math.sin(endRad - Math.PI / 2);

          const largeArc = seg.percentage > 50 ? 1 : 0;

          const pathData = [
            `M ${x1} ${y1}`,
            `A ${radius} ${radius} 0 ${largeArc} 1 ${x2} ${y2}`,
          ].join(' ');

          return (
            <path
              key={idx}
              d={pathData}
              fill="none"
              stroke={seg.color}
              strokeWidth="30"
              strokeLinecap="round"
            />
          );
        })}
        <circle cx="100" cy="100" r="45" fill="var(--bg-secondary, white)" />
      </svg>

      <div className="donut-legend">
        {segments.map((seg, idx) => (
          <div key={idx} className="legend-item">
            <span className="legend-color" style={{ backgroundColor: seg.color }}></span>
            <span className="legend-label">{seg.category}</span>
            <span className="legend-value">{seg.percentage.toFixed(1)}%</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function CumulativeSavingsChart({ data }) {
  if (!data || data.length === 0) {
    return <div className="no-data">No data available</div>;
  }

  const months = data.map((_, idx) => {
    const date = new Date(2024, idx % 12, 1);
    return date.toLocaleDateString('en-IN', { month: 'short' });
  });

  const maxValue = Math.max(...data) || 1;
  const padding = 50;
  const width = 900;
  const height = 300;

  const points = data
    .map((val, idx) => {
      const x = padding + (idx / (data.length - 1 || 1)) * (width - 2 * padding);
      const y = height - padding - (val / maxValue) * (height - 2 * padding);
      return `${x},${y}`;
    })
    .join(' ');

  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="chart-svg">
      {/* Grid lines */}
      {[0, 0.25, 0.5, 0.75, 1].map((ratio, idx) => {
        const y = height - padding - ratio * (height - 2 * padding);
        return (
          <line
            key={`grid-${idx}`}
            x1={padding}
            y1={y}
            x2={width - padding}
            y2={y}
            stroke="var(--border-color)"
            strokeWidth="1"
          />
        );
      })}

      {/* Y-axis */}
      <line x1={padding} y1={padding} x2={padding} y2={height - padding} stroke="var(--text-secondary)" strokeWidth="2" />

      {/* X-axis */}
      <line x1={padding} y1={height - padding} x2={width - padding} y2={height - padding} stroke="var(--text-secondary)" strokeWidth="2" />

      {/* Polyline */}
      <polyline points={points} fill="none" stroke="#2196F3" strokeWidth="3" className="chart-line" />

      {/* Data points */}
      {data.map((val, idx) => {
        const x = padding + (idx / (data.length - 1 || 1)) * (width - 2 * padding);
        const y = height - padding - (val / maxValue) * (height - 2 * padding);
        return <circle key={`dot-${idx}`} cx={x} cy={y} r="4" fill="#2196F3" />;
      })}

      {/* X-axis labels - show every Nth month */}
      {months.map((month, idx) => {
        if (months.length > 12 && idx % Math.ceil(months.length / 12) !== 0) return null;
        const x = padding + (idx / (data.length - 1 || 1)) * (width - 2 * padding);
        return (
          <text
            key={`label-${idx}`}
            x={x}
            y={height - 20}
            textAnchor="middle"
            fontSize="12"
            fill="var(--text-secondary)"
          >
            {month}
          </text>
        );
      })}

      {/* Y-axis labels */}
      {[0, 0.25, 0.5, 0.75, 1].map((ratio, idx) => {
        const y = height - padding - ratio * (height - 2 * padding);
        const value = Math.round(ratio * maxValue);
        return (
          <text key={`y-label-${idx}`} x={padding - 10} y={y + 5} textAnchor="end" fontSize="12" fill="var(--text-secondary)">
            ₹{(value / 100000).toFixed(1)}L
          </text>
        );
      })}
    </svg>
  );
}
