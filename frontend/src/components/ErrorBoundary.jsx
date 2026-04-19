import { Component } from 'react';

class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center',
          justifyContent: 'center', minHeight: '60vh', padding: '2rem',
          textAlign: 'center'
        }}>
          <div style={{
            fontSize: '3rem', marginBottom: '1rem',
            background: '#fef2f2', width: '80px', height: '80px',
            borderRadius: '50%', display: 'flex', alignItems: 'center',
            justifyContent: 'center'
          }}>⚠️</div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#1a1a2e', marginBottom: '0.5rem' }}>
            Something went wrong
          </h2>
          <p style={{ color: '#6b7280', fontSize: '0.95rem', marginBottom: '1.5rem', maxWidth: '400px' }}>
            An unexpected error occurred. Please try refreshing the page.
          </p>
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button
              onClick={() => window.location.reload()}
              style={{
                padding: '0.6rem 1.2rem', background: '#2563eb', color: 'white',
                border: 'none', borderRadius: '8px', cursor: 'pointer',
                fontWeight: 500, fontSize: '0.95rem'
              }}
            >
              Refresh Page
            </button>
            <button
              onClick={() => { window.location.href = '/app/dashboard'; }}
              style={{
                padding: '0.6rem 1.2rem', background: '#e5e7eb', color: '#374151',
                border: 'none', borderRadius: '8px', cursor: 'pointer',
                fontWeight: 500, fontSize: '0.95rem'
              }}
            >
              Go to Dashboard
            </button>
          </div>
          {process.env.NODE_ENV === 'development' && this.state.error && (
            <details style={{
              marginTop: '2rem', padding: '1rem', background: '#fef2f2',
              borderRadius: '8px', textAlign: 'left', maxWidth: '600px', width: '100%'
            }}>
              <summary style={{ cursor: 'pointer', fontWeight: 600, color: '#dc2626' }}>
                Error Details (dev only)
              </summary>
              <pre style={{ marginTop: '0.5rem', fontSize: '0.8rem', whiteSpace: 'pre-wrap', color: '#991b1b' }}>
                {this.state.error.toString()}
              </pre>
            </details>
          )}
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
