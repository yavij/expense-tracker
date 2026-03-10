export const DEBT_TYPES = [
  { value: 'HOME_LOAN', label: 'Home Loan' },
  { value: 'CAR_LOAN', label: 'Car Loan' },
  { value: 'PERSONAL_LOAN', label: 'Personal Loan' },
  { value: 'EDUCATION_LOAN', label: 'Education Loan' },
  { value: 'CREDIT_CARD', label: 'Credit Card' },
]

export const DEBT_STATUS = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'PAID_OFF', label: 'Paid Off' },
  { value: 'PAUSED', label: 'Paused' },
]

export function getDebtTypeLabel(value) {
  return DEBT_TYPES.find(t => t.value === value)?.label || value
}

export function getDebtStatusLabel(value) {
  return DEBT_STATUS.find(s => s.value === value)?.label || value
}
