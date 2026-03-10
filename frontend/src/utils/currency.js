// Exchange rates (can be updated from API later)
const EXCHANGE_RATES = {
  INR: 1,
  USD: 83.5,  // 1 USD = 83.5 INR
  EUR: 90.2,
  GBP: 105.8,
  AED: 22.7,
  SGD: 62.1,
};

export const CURRENCIES = Object.keys(EXCHANGE_RATES);

export function convertToINR(amount, fromCurrency) {
  const rate = EXCHANGE_RATES[fromCurrency] || 1;
  return amount * rate;
}

export function convertFromINR(amountINR, toCurrency) {
  const rate = EXCHANGE_RATES[toCurrency] || 1;
  return amountINR / rate;
}

export function convert(amount, from, to) {
  const inrAmount = convertToINR(amount, from);
  return convertFromINR(inrAmount, to);
}

export function formatCurrency(amount, currency = 'INR') {
  const symbol = { INR: '₹', USD: '$', EUR: '€', GBP: '£', AED: 'د.إ', SGD: 'S$' };
  return (symbol[currency] || currency + ' ') + amount.toLocaleString('en-IN', { maximumFractionDigits: 2 });
}

export function getCurrencySymbol(currency) {
  const symbols = { INR: '₹', USD: '$', EUR: '€', GBP: '£', AED: 'د.إ', SGD: 'S$' };
  return symbols[currency] || currency;
}
