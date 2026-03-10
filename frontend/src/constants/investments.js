export const INVESTMENT_TYPES = [
  { value: 'MF', label: 'Mutual Fund' },
  { value: 'PPF', label: 'Public Provident Fund' },
  { value: 'NPS', label: 'National Pension Scheme' },
  { value: 'RD', label: 'Recurring Deposit' },
  { value: 'STOCKS', label: 'Direct Stocks' },
]

export function getInvestmentTypeLabel(value) {
  return INVESTMENT_TYPES.find(t => t.value === value)?.label || value
}
