# Expense Tracker

Daily/monthly expense tracker with Gmail login, loans (Personal/Office), savings, and categories. Plain Java backend + React frontend.

## Prerequisites

- Java 17+
- Maven 3.6+
- Node.js 18+
- (Optional) MySQL 8+ — the app defaults to an embedded H2 database if no MySQL is configured

## Backend

### Quick Start (H2 — no database setup needed)

```bash
cd backend && mvn compile exec:java -Dexec.mainClass="expensetracker.Main"
```

Tables are created automatically on startup. Data is stored in `./expensetracker.mv.db`.

### Using MySQL (production)

1. Create database and run schema:
   ```bash
   mysql -u root -p < backend/src/main/resources/schema.sql
   ```
2. Set environment variables:
   - `JDBC_URL` — e.g. `jdbc:mysql://localhost:3306/expensetracker?useSSL=false&serverTimezone=UTC`
   - `DB_USER`, `DB_PASSWORD`
3. Run:
   ```bash
   cd backend && mvn compile exec:java -Dexec.mainClass="expensetracker.Main"
   ```

### Other env vars

- `JWT_SECRET` — min 32 chars (a default dev key is used if not set)
- `PORT` — default 7000
- `DEV_PHONE_OTP` — when set (e.g. `123456`), enables phone login without Firebase. Use this OTP on the Phone tab for development.
- `FIREBASE_PROJECT_ID` — your Firebase project ID. When set, the backend verifies Firebase ID tokens server-side. Without it, tokens are accepted without verification (fine for dev mode with `DEV_PHONE_OTP`).

### Building a fat JAR

```bash
cd backend && mvn package
java -jar target/backend-1.0.0.jar
```

## Frontend

1. Install and run:
   ```bash
   cd frontend && npm install && npm run dev
   ```
2. Set Google OAuth Client ID (for Sign in with Google):
   - Create a project in [Google Cloud Console](https://console.cloud.google.com/), enable Google Identity, create OAuth 2.0 Client ID (Web).
   - Create `frontend/.env`:
     ```
     VITE_GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
     ```
   - Add `http://localhost:3000` to authorized JavaScript origins.
3. Open http://localhost:3000. The dev server proxies `/api` to the backend (port 7000).

### Phone Login (Firebase OTP)

To enable real SMS-based phone login:

1. Create a project at [Firebase Console](https://console.firebase.google.com).
2. Enable **Phone** under Authentication > Sign-in method.
3. Add a Web app under Project Settings > General > Your apps.
4. Copy the config values into `frontend/.env`:
   ```
   VITE_FIREBASE_API_KEY=AIzaSy...
   VITE_FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
   VITE_FIREBASE_PROJECT_ID=your-project-id
   ```
5. Add `localhost` to authorized domains: Authentication > Settings > Authorized domains.
6. Set `FIREBASE_PROJECT_ID` on the backend (same value as `VITE_FIREBASE_PROJECT_ID`) for server-side token verification:
   ```bash
   export FIREBASE_PROJECT_ID=your-project-id
   ```

For **development without Firebase**, set `DEV_PHONE_OTP=123456` when starting the backend. The login page will show a dev mode where you enter this OTP directly.

## First user

The first user who signs in with Google is assigned the **ADMIN** role and can open the Admin page to see all users and login history. Other users are **USER** and only see their own expenses.
