import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { googleLogin, phoneLogin, phoneLoginDev } from '../api/client'
import { initializeApp } from 'firebase/app'
import { getAuth, signInWithPhoneNumber, RecaptchaVerifier } from 'firebase/auth'

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || ''

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
}

let firebaseApp = null
let firebaseAuth = null

const isFirebaseConfigured = () => {
  const { apiKey, authDomain, projectId } = firebaseConfig
  if (!apiKey || !authDomain || !projectId) return false
  if (/your-|placeholder|\.example\.com|here$/i.test(apiKey + authDomain + projectId)) return false
  return true
}

try {
  if (isFirebaseConfigured()) {
    firebaseApp = initializeApp(firebaseConfig)
    firebaseAuth = getAuth(firebaseApp)
  }
} catch (e) {
  console.error('Firebase initialization failed:', e)
}

const COUNTRY_CODES = [
  { code: '+91', label: 'IN +91' },
  { code: '+1', label: 'US +1' },
  { code: '+44', label: 'UK +44' },
  { code: '+61', label: 'AU +61' },
  { code: '+65', label: 'SG +65' },
  { code: '+971', label: 'AE +971' },
  { code: '+966', label: 'SA +966' },
  { code: '+60', label: 'MY +60' },
  { code: '+81', label: 'JP +81' },
  { code: '+86', label: 'CN +86' },
]

const FEATURES = [
  { icon: '📊', title: 'Track Daily', desc: 'Log expenses by category instantly' },
  { icon: '💰', title: 'Savings Goals', desc: 'Monitor your savings each month' },
  { icon: '🏦', title: 'Loan Manager', desc: 'Personal & office loans in one place' },
]

function getFirebaseErrorMessage(error) {
  switch (error?.code) {
    case 'auth/invalid-phone-number': return 'Invalid phone number format.'
    case 'auth/too-many-requests': return 'Too many attempts. Please try again later.'
    case 'auth/quota-exceeded': return 'SMS quota exceeded. Please try again later.'
    case 'auth/captcha-check-failed': return 'reCAPTCHA verification failed. Please refresh and try again.'
    case 'auth/invalid-verification-code': return 'Invalid OTP. Please check and try again.'
    case 'auth/code-expired': return 'OTP has expired. Please request a new one.'
    case 'auth/missing-phone-number': return 'Phone number is required.'
    case 'auth/billing-not-enabled': return 'SMS billing not enabled. Add a test phone number in Firebase Console, or upgrade to Blaze plan.'
    case 'auth/operation-not-allowed': return 'Phone authentication is not enabled. Enable it in Firebase Console.'
    default: return error?.message || 'Something went wrong. Please try again.'
  }
}

export default function Login() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const divRef = useRef(null)
  const initializedRef = useRef(false)

  const [loginTab, setLoginTab] = useState('google')
  const [phone, setPhone] = useState('')
  const [countryCode, setCountryCode] = useState('+91')
  const [otpSent, setOtpSent] = useState(false)
  const [otpDigits, setOtpDigits] = useState(['', '', '', '', '', ''])
  const [phoneLoading, setPhoneLoading] = useState(false)
  const confirmationResultRef = useRef(null)
  const otpRefs = useRef([])

  const [resendTimer, setResendTimer] = useState(0)
  const timerRef = useRef(null)

  const otpValue = otpDigits.join('')

  useEffect(() => {
    return () => { if (timerRef.current) clearInterval(timerRef.current) }
  }, [])

  useEffect(() => {
    if (user) navigate('/app/dashboard', { replace: true })
  }, [user, navigate])

  useEffect(() => {
    if (initializedRef.current || !GOOGLE_CLIENT_ID) return

    function initGoogle() {
      if (!window.google?.accounts?.id || !divRef.current) return false
      initializedRef.current = true
      try {
        window.google.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: handleCredentialResponse,
        })
        window.google.accounts.id.renderButton(divRef.current, {
          type: 'standard',
          theme: 'filled_blue',
          size: 'large',
          text: 'continue_with',
          width: 300,
        })
        return true
      } catch (e) {
        setError('Google Sign-In failed to load. Check VITE_GOOGLE_CLIENT_ID.')
        return true
      }
    }

    if (initGoogle()) return
    const interval = setInterval(() => {
      if (initGoogle()) clearInterval(interval)
    }, 300)
    return () => clearInterval(interval)
  }, [])

  function startResendTimer() {
    setResendTimer(30)
    if (timerRef.current) clearInterval(timerRef.current)
    timerRef.current = setInterval(() => {
      setResendTimer(prev => {
        if (prev <= 1) { clearInterval(timerRef.current); return 0 }
        return prev - 1
      })
    }, 1000)
  }

  function handleOtpChange(index, value) {
    const digit = value.replace(/\D/g, '').slice(-1)
    const next = [...otpDigits]
    next[index] = digit
    setOtpDigits(next)
    if (digit && index < 5) otpRefs.current[index + 1]?.focus()
  }

  function handleOtpKeyDown(index, e) {
    if (e.key === 'Backspace' && !otpDigits[index] && index > 0) {
      otpRefs.current[index - 1]?.focus()
    }
  }

  function handleOtpPaste(e) {
    e.preventDefault()
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6)
    const next = ['', '', '', '', '', '']
    for (let i = 0; i < pasted.length; i++) next[i] = pasted[i]
    setOtpDigits(next)
    otpRefs.current[Math.min(pasted.length, 5)]?.focus()
  }

  function resetOtp() {
    setOtpDigits(['', '', '', '', '', ''])
  }

  async function handleCredentialResponse(response) {
    if (!response?.credential) return
    setLoading(true)
    setError('')
    try {
      const data = await googleLogin(response.credential)
      if (data?.token && data?.user) {
        login(data.token, data.user)
        navigate('/app/dashboard', { replace: true })
      } else {
        setError(data?.error || 'Login failed. Please try again.')
      }
    } catch (e) {
      setError('Login failed. Is the backend running?')
    } finally {
      setLoading(false)
    }
  }

  async function handleSendOtp() {
    if (!phone || phone.length < 6) {
      setError('Please enter a valid phone number')
      return
    }
    if (!firebaseAuth) {
      setError('Phone login is not configured. Check Firebase env variables.')
      return
    }

    setPhoneLoading(true)
    setError('')
    try {
      if (!window.recaptchaVerifier) {
        window.recaptchaVerifier = new RecaptchaVerifier(firebaseAuth, 'recaptcha-container', {
          size: 'invisible',
          callback: () => {}
        })
      }

      const fullPhone = countryCode + phone
      const result = await signInWithPhoneNumber(firebaseAuth, fullPhone, window.recaptchaVerifier)
      confirmationResultRef.current = result
      setOtpSent(true)
      resetOtp()
      startResendTimer()
    } catch (e) {
      setError(getFirebaseErrorMessage(e))
      if (window.recaptchaVerifier) {
        window.recaptchaVerifier.clear()
        window.recaptchaVerifier = null
      }
    } finally {
      setPhoneLoading(false)
    }
  }

  async function handleResendOtp() {
    if (resendTimer > 0) return
    if (window.recaptchaVerifier) {
      window.recaptchaVerifier.clear()
      window.recaptchaVerifier = null
    }
    await handleSendOtp()
  }

  async function handleDevPhoneLogin() {
    if (!phone || phone.length < 6) {
      setError('Please enter a valid phone number')
      return
    }
    if (otpValue.length !== 6) {
      setError('Please enter the 6-digit dev OTP')
      return
    }

    setPhoneLoading(true)
    setError('')
    try {
      const data = await phoneLoginDev(countryCode + phone, otpValue)
      if (data?.token && data?.user) {
        login(data.token, data.user)
        navigate('/app/dashboard', { replace: true })
      } else {
        setError(data?.error || 'Login failed. Set DEV_PHONE_OTP=123456 on backend.')
      }
    } catch (e) {
      setError(e.message || 'Login failed. Is the backend running with DEV_PHONE_OTP set?')
    } finally {
      setPhoneLoading(false)
    }
  }

  async function handleVerifyOtp() {
    if (otpValue.length !== 6) {
      setError('Please enter a valid 6-digit OTP')
      return
    }
    if (!confirmationResultRef.current) {
      setError('OTP session expired. Please try again.')
      return
    }

    setPhoneLoading(true)
    setError('')
    try {
      const cred = await confirmationResultRef.current.confirm(otpValue)
      const idToken = await cred.user.getIdToken()

      const data = await phoneLogin({
        phone: countryCode + phone,
        firebaseUid: cred.user.uid,
        idToken
      })

      if (data?.token && data?.user) {
        login(data.token, data.user)
        navigate('/app/dashboard', { replace: true })
      } else {
        setError(data?.error || 'Login failed. Please try again.')
      }
    } catch (e) {
      setError(getFirebaseErrorMessage(e))
    } finally {
      setPhoneLoading(false)
    }
  }

  function renderOtpBoxes() {
    return (
      <div className="otp-input-group" onPaste={handleOtpPaste}>
        {otpDigits.map((digit, i) => (
          <input
            key={i}
            ref={el => otpRefs.current[i] = el}
            type="text"
            inputMode="numeric"
            maxLength={1}
            value={digit}
            onChange={e => handleOtpChange(i, e.target.value)}
            onKeyDown={e => handleOtpKeyDown(i, e)}
            disabled={phoneLoading}
          />
        ))}
      </div>
    )
  }

  function renderPhoneInput() {
    return (
      <div className="phone-input-group">
        <select
          className="country-code-select"
          value={countryCode}
          onChange={e => setCountryCode(e.target.value)}
          disabled={phoneLoading}
        >
          {COUNTRY_CODES.map(c => (
            <option key={c.code} value={c.code}>{c.label}</option>
          ))}
        </select>
        <input
          type="tel"
          placeholder="Phone number"
          maxLength="15"
          value={phone}
          onChange={e => setPhone(e.target.value.replace(/\D/g, ''))}
          disabled={phoneLoading}
        />
      </div>
    )
  }

  return (
    <div className="login-page">
      <div className="login-bg">
        <div className="login-shape login-shape-1"></div>
        <div className="login-shape login-shape-2"></div>
        <div className="login-shape login-shape-3"></div>
        <div className="login-shape login-shape-4"></div>
      </div>

      <div className="login-container">
        <div className="login-card">
          <div className="login-logo">
            <div className="login-logo-icon">
              <span style={{ fontSize: '1.8rem', fontWeight: 700, color: 'white' }}>₹</span>
            </div>
          </div>

          <h1 className="login-title">Kanaku Book</h1>
          <p className="login-subtitle">Smart money management for everyday life</p>

          <div className="login-divider"></div>

          {error && <p className="login-error">{error}</p>}

          {loading ? (
            <div className="login-loading">
              <div className="login-spinner"></div>
              <span>Signing you in...</span>
            </div>
          ) : (
            <div className="login-signin-area">
              <div className="login-tabs">
                <button
                  className={loginTab === 'google' ? 'active' : ''}
                  onClick={() => { setLoginTab('google'); setError('') }}
                >
                  Google
                </button>
                <button
                  className={loginTab === 'phone' ? 'active' : ''}
                  onClick={() => { setLoginTab('phone'); setError('') }}
                >
                  Phone
                </button>
              </div>

              {loginTab === 'google' ? (
                <>
                  <div ref={divRef} id="google-signin-button" />
                  {!GOOGLE_CLIENT_ID && (
                    <p className="login-error" style={{ marginTop: '1rem' }}>
                      Set VITE_GOOGLE_CLIENT_ID in frontend/.env to enable sign-in.
                    </p>
                  )}
                </>
              ) : !firebaseAuth ? (
                <div style={{ width: '100%' }}>
                  <p className="otp-hint" style={{ marginBottom: '0.75rem' }}>
                    Dev mode — use the OTP set via DEV_PHONE_OTP on backend
                  </p>
                  {renderPhoneInput()}
                  <p className="otp-hint">Enter 6-digit OTP</p>
                  {renderOtpBoxes()}
                  <button
                    className="login-btn login-btn-primary"
                    onClick={handleDevPhoneLogin}
                    disabled={phoneLoading || !phone || otpValue.length !== 6}
                    style={{ marginTop: '0.5rem' }}
                  >
                    {phoneLoading ? 'Signing in...' : 'Sign in with Phone'}
                  </button>
                </div>
              ) : (
                <div style={{ width: '100%' }}>
                  {!otpSent ? (
                    <>
                      {renderPhoneInput()}
                      <button
                        className="login-btn login-btn-primary"
                        onClick={handleSendOtp}
                        disabled={phoneLoading || !phone}
                      >
                        {phoneLoading ? 'Sending OTP...' : 'Send OTP'}
                      </button>
                    </>
                  ) : (
                    <>
                      <p className="otp-sent-info">
                        OTP sent to <strong>{countryCode} {phone}</strong>
                      </p>
                      {renderOtpBoxes()}
                      <button
                        className="login-btn login-btn-primary"
                        onClick={handleVerifyOtp}
                        disabled={phoneLoading || otpValue.length !== 6}
                      >
                        {phoneLoading ? 'Verifying...' : 'Verify OTP'}
                      </button>
                      <div className="otp-actions">
                        <button
                          type="button"
                          className="otp-resend-btn"
                          onClick={handleResendOtp}
                          disabled={resendTimer > 0 || phoneLoading}
                        >
                          {resendTimer > 0 ? `Resend in ${resendTimer}s` : 'Resend OTP'}
                        </button>
                        <button
                          type="button"
                          className="otp-back-btn"
                          onClick={() => {
                            setOtpSent(false)
                            resetOtp()
                            setPhone('')
                            if (timerRef.current) clearInterval(timerRef.current)
                            setResendTimer(0)
                          }}
                          disabled={phoneLoading}
                        >
                          Change number
                        </button>
                      </div>
                    </>
                  )}
                  <div id="recaptcha-container"></div>
                </div>
              )}
            </div>
          )}

          <p className="login-privacy">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ display: 'inline', verticalAlign: '-2px', marginRight: '4px' }}>
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
            </svg>
            Your data is private and secure
          </p>
        </div>

        <div className="login-features">
          {FEATURES.map((f, i) => (
            <div className="login-feature" key={i}>
              <span className="login-feature-icon">{f.icon}</span>
              <div>
                <strong>{f.title}</strong>
                <span>{f.desc}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
