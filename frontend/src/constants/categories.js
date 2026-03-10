export const CATEGORIES = [
  { value: 'LOAN_PERSONAL', label: 'Personal Loan' },
  { value: 'LOAN_OFFICE', label: 'Office Loan' },
  { value: 'SAVINGS', label: 'Savings' },
  { value: 'DAILY', label: 'Daily' },
  { value: 'HOME', label: 'Home' },
  { value: 'COSMETICS', label: 'Cosmetics' },
  { value: 'TRIP', label: 'Trip' },
]

export function getCategoryLabel(value) {
  return CATEGORIES.find(c => c.value === value)?.label || value
}

export function isLoanCategory(value) {
  return value === 'LOAN_PERSONAL' || value === 'LOAN_OFFICE'
}
