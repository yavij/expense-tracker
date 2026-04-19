# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability in Expense Tracker, please report it responsibly.

**Do NOT open a public GitHub issue for security vulnerabilities.**

Instead, please email: **security@expensetracker.example.com**

Include the following in your report:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

We will acknowledge your report within 48 hours and aim to provide a fix within 7 days for critical issues.

## Security Measures

This application implements the following security measures:

### Authentication & Authorization
- JWT-based authentication with configurable expiration
- Google OAuth 2.0 integration
- Firebase Phone OTP verification
- Role-based access control (USER / ADMIN)

### API Security
- IP-based rate limiting (10 req/min for auth, 60 req/min for API)
- Security headers (X-Frame-Options, CSP, HSTS, X-XSS-Protection)
- Input validation and HTML sanitization on all user inputs
- Optimistic locking to prevent concurrent edit conflicts

### Data Protection
- Passwords are never stored (OAuth/OTP only)
- JWT secrets must be configured via environment variables
- Database credentials externalized via environment variables
- `.env` files excluded from version control via `.gitignore`

### Deployment
- Docker images use minimal base images (JRE slim)
- Health checks configured for container orchestration
- MySQL credentials configurable via Docker environment variables

## Best Practices for Deployment

1. **Change all default secrets** before deploying to production
2. **Use HTTPS** in production (configure a reverse proxy like Nginx)
3. **Restrict database access** to the application server only
4. **Enable email notifications** for budget alerts and security events
5. **Monitor logs** for suspicious activity (rate limit hits, auth failures)
6. **Keep dependencies updated** - run `mvn versions:display-dependency-updates` regularly
