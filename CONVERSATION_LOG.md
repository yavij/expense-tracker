# Expense Tracker - Development Conversation Log

**Project:** Expense Tracker (Java/Javalin Backend + React/Vite Frontend)
**Developer:** Vijay Kumar
**Date:** March 9, 2026
**Sessions:** 3 continued sessions

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Session 1 & 2 Summary](#session-1--2-summary)
4. [Session 3 - Current Session](#session-3---current-session)
5. [All 20 Pending Items Completed](#all-20-pending-items-completed)
6. [Files Created & Modified](#files-created--modified)
7. [Preview HTML - Full Feature Showcase](#preview-html---full-feature-showcase)
8. [Known Limitations & Next Steps](#known-limitations--next-steps)

---

## Project Overview

A comprehensive personal finance management application built with a Java backend and React frontend. Features include expense tracking, investment portfolio management, salary tracking, debt/loan management, analytics dashboards, recurring transactions, budget management, and an admin panel.

---

## Tech Stack

### Backend
- **Language:** Java 17
- **Framework:** Javalin 6.1.3
- **Database:** H2 (dev) + MySQL (prod) via JDBC
- **Connection Pool:** HikariCP
- **Auth:** JWT (jjwt 0.12.5), Google OAuth, Firebase Phone OTP
- **JSON:** Gson
- **Logging:** SLF4J with SimpleLogger
- **Build:** Maven

### Frontend
- **Library:** React 18
- **Routing:** React Router 6
- **Build Tool:** Vite 5
- **Auth:** Google Sign-In (GSI), Firebase Auth (Phone OTP)
- **Styling:** CSS Variables with 6 color themes

---

## Session 1 & 2 Summary

Work completed across the first two sessions:

- Initial project setup (Java/Javalin backend + React/Vite frontend)
- Expense CRUD operations with categories (Daily, Home, Savings, Loan-Personal, Loan-Office, Cosmetics, Trip)
- Investment portfolio tracking (MF, PPF, NPS, RD, Stocks)
- Salary tracker with bar chart visualization
- Debt/Loan management with EMI calculations and payoff strategies (Snowball/Avalanche)
- Analytics dashboard with monthly, yearly, and all-time views
- Google OAuth login integration
- Phone OTP login via Firebase
- Admin panel with Users and Login History tabs
- Draggable dashboard widgets with layout presets
- User profile dropdown with member info
- Configurable navigation bar (drag-drop reorder, icon/color/visibility pickers, rename labels)
- Sidebar layout mode (collapsible, 240px → 70px)
- 6 color themes: Light, Dark, Blue, Green, Purple, Sunset
- Bug fixes and UI improvements throughout

---

## Session 3 - Current Session

### User Request 1: "Run the program and show the output in localhost"

**Environment Discovery:**
- Java 11 JRE only (no JDK/javac) — project requires Java 17
- No Maven installed
- No sudo access available
- Network blocked (all outbound HTTP returns 403)
- node_modules built for wrong architecture (x86 vs aarch64)

**Workaround:** Created a Node.js HTTP server (`server.js`) serving `preview.html` on port 3000.

### User Request 2: "Suggest domain names for the app"

Suggested domain names including:
- expensetracker.in
- myfinancebuddy.com
- rupeesaver.in

**Tamil name suggestions:**
- Selvam (செல்வம்) - Wealth
- Kanakku (கணக்கு) - Accounts
- Panam (பணம்) - Money
- Seethanam (சீதனம்) - Fortune

### User Request 3: "What are the pending work items to complete this project"

Conducted a comprehensive audit and identified **20 pending items**.

### User Request 4: "Complete this one by one"

All 20 items were implemented sequentially (details below).

### User Request 5 (repeated 4+ times): "Now run the project"

Server was restarted each time. Due to sandbox limitations (no JDK 17, no Maven, no network), the workaround remained: serving preview.html via Node.js.

### User Request 6: "Prepare the preview HTML for all the changes"

Updated `preview.html` comprehensively to showcase ALL features with:
- All 9 pages fully featured with realistic Indian sample data
- Search bars, filters, Export CSV buttons on all list pages
- Summary cards with calculated totals
- Category badges with color coding
- Progress bars for budget tracking
- SVG charts for analytics
- Restored configurable navigation (drag-drop reorder, icon/color pickers)
- Sidebar layout mode with collapsible sidebar
- All 6 color themes functional
- Admin/User role toggle
- Google Sign-In + Phone OTP login

---

## All 20 Pending Items Completed

### 1. Analytics Charts Enhancement
- Fixed CSS classes for analytics components
- Dynamic comparison bar widths using calculated percentages
- Theme-aware donut chart (center fill uses `var(--bg-secondary)`)

### 2. Preferences Modal + Backend Persistence
- New `user_preferences` table in database
- `UserPreference.java` model
- `UserPreferenceDao.java` with MERGE (H2) / INSERT ON DUPLICATE KEY (MySQL)
- `PreferenceHandler.java` — GET/PUT `/api/preferences`
- Frontend syncs preferences to backend with localStorage fallback

### 3. Data Export (CSV)
- `ExportHandler.java` with proper CSV escaping
- Export endpoints for expenses, investments, salary, debts
- "Export CSV" buttons added to all list pages

### 4. Recurring Transactions
- `RecurringTransaction.java` model
- `RecurringTransactionDao.java` with full CRUD + findDueTransactions
- `RecurringHandler.java` — CRUD + processDue endpoint
- `RecurringTransactions.jsx` frontend page
- Added to nav config and App.jsx routes

### 5. Budget Limits & Alerts
- `BudgetHandler.java` — list, status with progress, upsert, delete
- `BudgetDao.java` for database operations
- `BudgetManager.jsx` — progress bars (green/yellow/red), CRUD form

### 6. Deployment Configuration
- `Dockerfile` — multi-stage build (Maven + JRE 17)
- `docker-compose.yml` — app + MySQL services
- `.gitignore` — comprehensive ignore rules

### 7. Environment Configuration
- `.env.example` for backend (DB, JWT, OAuth, email settings)
- `.env.example` for frontend (API URL, Firebase, Google Client ID)

### 8. API Documentation
- Comprehensive `API.md` documenting all endpoints
- Authentication, expenses, investments, salary, debts, analytics
- Budgets, recurring, export, backup, preferences, admin

### 9. Input Validation & Sanitization
- `ValidationUtil.java` — HTML sanitization, length limits, format validation

### 10. Security Hardening
- `RateLimiter.java` — IP-based in-memory rate limiting
- Auth endpoints: 10 requests/minute
- General API: 60 requests/minute
- Security headers: X-Frame-Options, CSP, HSTS, XSS-Protection

### 11. Error Recovery (Frontend)
- Retry utility with exponential backoff
- Frontend error handling improvements

### 12. Search Functionality
- `searchExpenses` API endpoint with query parameter
- Frontend search bar on Expenses page

### 13. Structured Logging
- `simplelogger.properties` configuration
- Request logging middleware with duration tracking
- Thread name and datetime format in logs

### 14. EMI Auto-Calculation
- EMI calculator in DebtForm using standard formula
- `EMI = P × r × (1+r)^n / ((1+r)^n - 1)`

### 15. Data Backup/Restore
- `BackupHandler.java`
- GET `/api/backup` — exports all user data as JSON
- POST `/api/backup/restore` — imports from JSON backup

### 16. Concurrent Edit Prevention
- Version-based optimistic locking
- Added `version INT NOT NULL DEFAULT 1` to all main tables
- `ConflictException.java` for version conflicts

### 17. Multi-Currency Conversion
- `currency.js` utility — exchange rates for INR, USD, EUR, GBP, AED, SGD
- `currencies.js` constants — labels, symbols, options
- `convertToINR`, `convertFromINR`, `convert`, `formatCurrency` functions

### 18. Email Notifications
- `EmailService.java` — placeholder service (logs for now)
- Methods: `sendBudgetAlert`, `sendDebtReminder`, `sendWeeklySummary`
- `NotificationScheduler.java` — ScheduledExecutorService
  - Daily budget alert checks
  - Debt EMI reminders

### 19. Mobile Responsiveness
- Comprehensive responsive CSS in `index.css`
- Media queries for 640px breakpoint
- Flexible grids with `auto-fit` and `minmax`

### 20. Improved Mobile CSS
- Enhanced touch targets
- Responsive table layouts
- Mobile-friendly forms and navigation

---

## Files Created & Modified

### Backend - New Files
| File | Description |
|------|-------------|
| `model/UserPreference.java` | User preference model |
| `model/RecurringTransaction.java` | Recurring transaction model |
| `dao/UserPreferenceDao.java` | Preferences CRUD operations |
| `dao/RecurringTransactionDao.java` | Recurring transactions CRUD |
| `handler/PreferenceHandler.java` | GET/PUT preferences API |
| `handler/ExportHandler.java` | CSV export for all entities |
| `handler/RecurringHandler.java` | Recurring transactions API |
| `handler/BudgetHandler.java` | Budget management API |
| `handler/BackupHandler.java` | Backup/restore API |
| `exception/ConflictException.java` | Optimistic locking exception |
| `util/RateLimiter.java` | IP-based rate limiting |
| `util/ValidationUtil.java` | Input validation & sanitization |
| `util/EmailService.java` | Email notification service |
| `util/NotificationScheduler.java` | Scheduled notifications |
| `resources/simplelogger.properties` | Logger configuration |

### Backend - Modified Files
| File | Changes |
|------|---------|
| `Main.java` | Added all new handlers, rate limiting, security headers, logging middleware, new routes |
| `Database.java` | Added `isH2()`, version columns, new tables (user_preferences, budgets, recurring_transactions), indexes |

### Frontend - New Files
| File | Description |
|------|-------------|
| `pages/RecurringTransactions.jsx` | Recurring transactions CRUD page |
| `pages/BudgetManager.jsx` | Budget status + management page |
| `utils/currency.js` | Currency conversion utilities |
| `constants/currencies.js` | Supported currencies list |

### Frontend - Modified Files
| File | Changes |
|------|---------|
| `App.jsx` | Added routes for /recurring and /budgets |
| `api/client.js` | Added API calls for search, export, preferences, recurring, budgets |
| `components/Layout.jsx` | Backend preference sync, recurring/budgets in nav |
| `pages/Analytics.jsx` | Fixed dynamic bar widths, theme-aware donut |
| `pages/ExpenseList.jsx` | Added export CSV button |
| `pages/InvestmentList.jsx` | Added export CSV button |
| `pages/SalaryTracker.jsx` | Added export CSV button |
| `pages/DebtList.jsx` | Added export CSV button |
| `index.css` | Analytics CSS, responsive improvements |

### Config Files (New)
| File | Description |
|------|-------------|
| `Dockerfile` | Multi-stage Docker build |
| `docker-compose.yml` | App + MySQL services |
| `.gitignore` | Comprehensive ignore rules |
| `backend/.env.example` | Backend environment template |
| `frontend/.env.example` | Frontend environment template |
| `API.md` | Complete API documentation |

### Preview
| File | Description |
|------|-------------|
| `preview.html` | Comprehensive single-file preview (2060 lines) showcasing all features |
| `server.js` | Node.js HTTP server for serving preview |

---

## Preview HTML - Full Feature Showcase

The `preview.html` file (2060 lines) is a self-contained HTML file that demonstrates all application features:

### Pages Included
1. **Login Page** — Google Sign-In + Phone OTP with animated background
2. **Dashboard** — Draggable cards, layout presets, month navigation
3. **Expenses** — Search, filters, category badges, Export CSV, 8 sample rows
4. **Investments** — Summary cards, type filter, gain/loss color coding
5. **Salary Tracker** — Add form, bar chart, history table, Export CSV
6. **Debts** — Summary cards, status filters, badges, Export CSV
7. **Analytics** — Monthly/Yearly/All-Time tabs with charts
8. **Recurring Transactions** — CRUD, frequency, status indicators
9. **Budget Manager** — Progress bars (green/yellow/red), manage form
10. **Admin** — Users tab + Login History tab

### Interactive Features
- Configurable navigation (drag-drop reorder, icon/color pickers, rename, visibility toggle)
- Sidebar layout mode with collapsible sidebar
- 6 color themes (Light, Dark, Blue, Green, Purple, Sunset)
- User profile dropdown with preferences
- Admin/User role toggle
- Draggable dashboard widgets

---

## Known Limitations & Next Steps

### Current Limitations (Sandbox Environment)
- Cannot compile Java backend (no JDK 17, only JRE 11)
- Cannot install Maven
- Cannot run `npm install` (network blocked, wrong architecture)
- No sudo access
- Preview served via static HTML (not full React app)

### To Run the Full Application
1. Install Java 17 JDK and Maven
2. Run backend: `cd backend && mvn clean package && java -jar target/expense-tracker.jar`
3. Install frontend deps: `cd frontend && npm install`
4. Run frontend: `npm run dev`
5. Or use Docker: `docker-compose up --build`

### Future Enhancements
- Real email service integration (SendGrid/SES)
- Push notifications
- Data visualization library (Chart.js/Recharts)
- PWA support for mobile
- Automated testing suite
- CI/CD pipeline
- Production database migration scripts

---

*Generated from Claude AI development session — March 9, 2026*
