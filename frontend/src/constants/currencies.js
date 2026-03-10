export const CURRENCIES = [
  { value: 'INR', label: 'INR - Indian Rupee', symbol: '₹' },
  { value: 'USD', label: 'USD - US Dollar', symbol: '$' },
  { value: 'EUR', label: 'EUR - Euro', symbol: '€' },
  { value: 'GBP', label: 'GBP - British Pound', symbol: '£' },
  { value: 'AED', label: 'AED - UAE Dirham', symbol: 'د.إ' },
  { value: 'SGD', label: 'SGD - Singapore Dollar', symbol: 'S$' },
];

export function getCurrencyOptions() {
  return CURRENCIES.map(c => ({ value: c.value, label: c.label }));
}

export function getCurrencyLabel(value) {
  return CURRENCIES.find(c => c.value === value)?.label || value;
}

export function getCurrencySymbol(value) {
  return CURRENCIES.find(c => c.value === value)?.symbol || value;
}
