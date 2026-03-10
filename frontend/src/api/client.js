const API_BASE = import.meta.env.VITE_API_URL || '';

function getToken() {
  return localStorage.getItem('token');
}

export async function api(path, options = {}) {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const url = API_BASE + path;

  let res;
  try {
    res = await fetch(url, { ...options, headers });
  } catch (err) {
    throw new Error('Network error. Is the backend running?');
  }

  if (res.status === 401) {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/';
    return;
  }

  if (res.status === 204) return null;

  const text = await res.text();
  if (!text) return null;

  let data;
  try {
    data = JSON.parse(text);
  } catch {
    return text;
  }

  // Propagate server errors
  if (!res.ok && data?.error) {
    throw new Error(data.error);
  }

  return data;
}

export async function googleLogin(idToken) {
  return api('/api/auth/google', {
    method: 'POST',
    body: JSON.stringify({ idToken }),
  });
}

export async function phoneLogin(data) {
  return api('/api/auth/phone', { method: 'POST', body: JSON.stringify(data) });
}

/** Dev mode: login with phone + OTP when DEV_PHONE_OTP is set on backend */
export async function phoneLoginDev(phone, otp) {
  return api('/api/auth/phone', {
    method: 'POST',
    body: JSON.stringify({ phone, otp }),
  });
}

export async function updateProfile(data) {
  return api('/api/me', { method: 'PUT', body: JSON.stringify(data) });
}

export async function getMe() {
  return api('/api/me');
}

export async function getExpenses(params = {}) {
  const q = new URLSearchParams(params).toString();
  return api('/api/expenses' + (q ? '?' + q : ''));
}

export async function getExpense(id) {
  return api('/api/expenses/' + encodeURIComponent(id));
}

export async function getExpenseSummary(month) {
  return api('/api/expenses/summary' + (month ? '?month=' + encodeURIComponent(month) : ''));
}

export async function createExpense(data) {
  return api('/api/expenses', { method: 'POST', body: JSON.stringify(data) });
}

export async function updateExpense(id, data) {
  return api('/api/expenses/' + encodeURIComponent(id), { method: 'PUT', body: JSON.stringify(data) });
}

export async function deleteExpense(id) {
  return api('/api/expenses/' + encodeURIComponent(id), { method: 'DELETE' });
}

export async function searchExpenses(query) {
  return api('/api/expenses/search?q=' + encodeURIComponent(query));
}

// ── Investments ──────────────────────────────────────────
export async function getInvestments(params = {}) {
  const q = new URLSearchParams(params).toString();
  return api('/api/investments' + (q ? '?' + q : ''));
}
export async function getInvestment(id) {
  return api('/api/investments/' + encodeURIComponent(id));
}
export async function getPortfolio() {
  return api('/api/investments/portfolio');
}
export async function createInvestment(data) {
  return api('/api/investments', { method: 'POST', body: JSON.stringify(data) });
}
export async function updateInvestment(id, data) {
  return api('/api/investments/' + encodeURIComponent(id), { method: 'PUT', body: JSON.stringify(data) });
}
export async function deleteInvestment(id) {
  return api('/api/investments/' + encodeURIComponent(id), { method: 'DELETE' });
}

// ── Salary ───────────────────────────────────────────────
export async function getSalaryEntries(params = {}) {
  const q = new URLSearchParams(params).toString();
  return api('/api/salary' + (q ? '?' + q : ''));
}
export async function getSalaryEntry(id) {
  return api('/api/salary/' + encodeURIComponent(id));
}
export async function getSalaryHistory() {
  return api('/api/salary/history');
}
export async function createSalaryEntry(data) {
  return api('/api/salary', { method: 'POST', body: JSON.stringify(data) });
}
export async function updateSalaryEntry(id, data) {
  return api('/api/salary/' + encodeURIComponent(id), { method: 'PUT', body: JSON.stringify(data) });
}
export async function deleteSalaryEntry(id) {
  return api('/api/salary/' + encodeURIComponent(id), { method: 'DELETE' });
}

// ── Debts ────────────────────────────────────────────────
export async function getDebts(params = {}) {
  const q = new URLSearchParams(params).toString();
  return api('/api/debts' + (q ? '?' + q : ''));
}
export async function getDebt(id) {
  return api('/api/debts/' + encodeURIComponent(id));
}
export async function createDebt(data) {
  return api('/api/debts', { method: 'POST', body: JSON.stringify(data) });
}
export async function updateDebt(id, data) {
  return api('/api/debts/' + encodeURIComponent(id), { method: 'PUT', body: JSON.stringify(data) });
}
export async function deleteDebt(id) {
  return api('/api/debts/' + encodeURIComponent(id), { method: 'DELETE' });
}
export async function getDebtPayments(debtId) {
  return api('/api/debts/' + encodeURIComponent(debtId) + '/payments');
}
export async function addDebtPayment(debtId, data) {
  return api('/api/debts/' + encodeURIComponent(debtId) + '/payments', { method: 'POST', body: JSON.stringify(data) });
}
export async function getPayoffSchedule(params = {}) {
  const q = new URLSearchParams(params).toString();
  return api('/api/debts/schedule' + (q ? '?' + q : ''));
}

// ── Analytics ────────────────────────────────────────────
export async function getMonthlyAnalytics(month) {
  return api('/api/analytics/monthly' + (month ? '?month=' + encodeURIComponent(month) : ''));
}
export async function getYearlyAnalytics(year) {
  return api('/api/analytics/yearly' + (year ? '?year=' + encodeURIComponent(year) : ''));
}
export async function getNetworth() {
  return api('/api/analytics/networth');
}

// ── Export ───────────────────────────────────────────────
export async function exportCsv(type, params = {}) {
  const token = localStorage.getItem('token');
  const q = new URLSearchParams(params).toString();
  const url = (API_BASE || '') + '/api/export/' + type + (q ? '?' + q : '');
  const res = await fetch(url, { headers: { 'Authorization': `Bearer ${token}` } });
  if (!res.ok) throw new Error('Export failed');
  const blob = await res.blob();
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = type + '.csv';
  a.click();
  URL.revokeObjectURL(a.href);
}

// ── Preferences ─────────────────────────────────────────
export async function getPreferences() {
  return api('/api/preferences');
}
export async function savePreferences(prefs) {
  return api('/api/preferences', { method: 'PUT', body: JSON.stringify(prefs) });
}

// ── Recurring Transactions ───────────────────────────────
export async function getRecurringTransactions() {
  return api('/api/recurring');
}
export async function getRecurringTransaction(id) {
  return api('/api/recurring/' + encodeURIComponent(id));
}
export async function createRecurringTransaction(data) {
  return api('/api/recurring', { method: 'POST', body: JSON.stringify(data) });
}
export async function updateRecurringTransaction(id, data) {
  return api('/api/recurring/' + encodeURIComponent(id), { method: 'PUT', body: JSON.stringify(data) });
}
export async function deleteRecurringTransaction(id) {
  return api('/api/recurring/' + encodeURIComponent(id), { method: 'DELETE' });
}
export async function processRecurringTransactions() {
  return api('/api/recurring/process', { method: 'POST' });
}

// ── Budgets ─────────────────────────────────────────────
export async function getBudgets() {
  return api('/api/budgets');
}
export async function getBudgetStatus(month) {
  return api('/api/budgets/status' + (month ? '?month=' + encodeURIComponent(month) : ''));
}
export async function upsertBudget(data) {
  return api('/api/budgets', { method: 'POST', body: JSON.stringify(data) });
}
export async function deleteBudget(id) {
  return api('/api/budgets/' + encodeURIComponent(id), { method: 'DELETE' });
}

// ── Admin ────────────────────────────────────────────────
export async function getAdminUsers() {
  return api('/api/admin/users');
}

export async function getAdminLogins(limit = 50, offset = 0) {
  return api(`/api/admin/logins?limit=${limit}&offset=${offset}`);
}

// ── Payment ─────────────────────────────────────────────
export async function createPaymentOrder() {
  return api('/api/payment/create-order', { method: 'POST' });
}
export async function verifyPayment(data) {
  return api('/api/payment/verify', { method: 'POST', body: JSON.stringify(data) });
}
export async function getSubscriptionStatus() {
  return api('/api/payment/status');
}
