import { useState, useEffect } from 'react'
import { useAuth } from '../auth/AuthContext'
import { createPaymentOrder, verifyPayment, getSubscriptionStatus } from '../api/client'

const PREMIUM_PRICE = 49
const CACHE_KEY = 'premium-subscription'

function getCachedSub() {
  try {
    const stored = localStorage.getItem(CACHE_KEY)
    if (!stored) return null
    const sub = JSON.parse(stored)
    if (sub && new Date(sub.expiresAt) > new Date()) return sub
    localStorage.removeItem(CACHE_KEY)
    return null
  } catch { return null }
}

export default function PaymentGate({ feature, children }) {
  const { user } = useAuth()
  const [subscription, setSubscription] = useState(() => getCachedSub())
  const [loading, setLoading] = useState(!getCachedSub())
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (subscription) { setLoading(false); return }
    getSubscriptionStatus()
      .then(res => {
        if (res?.active && res.subscription) {
          localStorage.setItem(CACHE_KEY, JSON.stringify(res.subscription))
          setSubscription(res.subscription)
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="payment-gate"><p style={{ color: 'var(--text-secondary)' }}>Checking subscription...</p></div>
  if (subscription) return children

  const handlePayment = async () => {
    setProcessing(true)
    setError('')
    try {
      const order = await createPaymentOrder()
      if (!order?.orderId) {
        setError(order?.error || 'Failed to create order. Check Razorpay keys.')
        setProcessing(false)
        return
      }

      const options = {
        key: order.keyId || import.meta.env.VITE_RAZORPAY_KEY_ID,
        amount: order.amount,
        currency: order.currency,
        name: 'Kanaku Book',
        description: 'Premium Plan - 30 days',
        order_id: order.orderId,
        prefill: {
          email: user?.email || '',
          contact: user?.phoneNumber || user?.phone || '',
        },
        theme: { color: '#2563eb' },
        handler: async (response) => {
          try {
            const result = await verifyPayment({
              razorpay_order_id: response.razorpay_order_id,
              razorpay_payment_id: response.razorpay_payment_id,
              razorpay_signature: response.razorpay_signature,
            })
            if (result?.success) {
              localStorage.setItem(CACHE_KEY, JSON.stringify(result.subscription))
              setSubscription(result.subscription)
            } else {
              setError('Payment verification failed. Please contact support.')
            }
          } catch (e) {
            setError('Payment verification failed. Please try again.')
          }
          setProcessing(false)
        },
        modal: {
          ondismiss: () => { setProcessing(false) },
        },
      }

      const rzp = new window.Razorpay(options)
      rzp.on('payment.failed', (resp) => {
        setError(resp.error?.description || 'Payment failed. Please try again.')
        setProcessing(false)
      })
      rzp.open()
    } catch (e) {
      setError(e.message || 'Something went wrong. Please try again.')
      setProcessing(false)
    }
  }

  return (
    <div className="payment-gate">
      <div className="payment-card">
        <div className="payment-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--accent-color)" strokeWidth="1.5">
            <rect x="1" y="4" width="22" height="16" rx="2" ry="2"/>
            <line x1="1" y1="10" x2="23" y2="10"/>
          </svg>
        </div>
        <h2 className="payment-title">Unlock {feature}</h2>
        <p className="payment-desc">
          Get access to <strong>{feature}</strong> and all premium features with our monthly plan.
        </p>

        <div className="payment-price-box">
          <span className="payment-currency">₹</span>
          <span className="payment-amount">{PREMIUM_PRICE}</span>
          <span className="payment-period">/month</span>
        </div>

        <ul className="payment-features">
          <li>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            Analytics Dashboard
          </li>
          <li>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            Recurring Transactions
          </li>
          <li>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            Budget Manager
          </li>
          <li>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            UPI, Cards, NetBanking supported
          </li>
        </ul>

        <button
          className={`btn btn-primary payment-btn ${processing ? 'processing' : ''}`}
          onClick={handlePayment}
          disabled={processing}
        >
          {processing ? 'Processing...' : `Pay ₹${PREMIUM_PRICE} / month`}
        </button>

        {error && <div className="payment-error">{error}</div>}

        <p className="payment-note">
          Powered by Razorpay. Secure payment.
        </p>
      </div>
    </div>
  )
}
