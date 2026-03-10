# Expense Tracker API Documentation

## Overview

The Expense Tracker API is a comprehensive RESTful service for managing personal finances, including expenses, investments, salary tracking, debt management, budgets, recurring transactions, and financial analytics.

**Base URL:** `http://localhost:7000/api`

**API Version:** 1.0.0

**Authentication:** JWT Bearer Token (required for all endpoints except Google OAuth and Phone login)

**Content-Type:** All endpoints accept and return JSON (`application/json`)

---

## Authentication

All endpoints except authentication endpoints require a JWT Bearer token in the `Authorization` header:
```
Authorization: Bearer <token>
```

### POST /api/auth/google
**Description:** Authenticate user via Google OAuth

**Authentication Required:** No

**Request Body:**
```json
{
  "idToken": "google_id_token_string"
}
```

**Response (200 OK):**
```json
{
  "token": "jwt_token_string",
  "userId": "user_id",
  "email": "user@example.com",
  "name": "User Name"
}
```

**Error Responses:**
- 400 Bad Request - Invalid or missing idToken
- 401 Unauthorized - Google token validation failed

---

### POST /api/auth/phone
**Description:** Authenticate user via Firebase Phone Authentication

**Authentication Required:** No

**Request Body:**
```json
{
  "uid": "firebase_uid",
  "phoneNumber": "+1234567890"
}
```

**Response (200 OK):**
```json
{
  "token": "jwt_token_string",
  "userId": "user_id",
  "phoneNumber": "+1234567890"
}
```

**Error Responses:**
- 400 Bad Request - Invalid or missing authentication data
- 401 Unauthorized - Firebase authentication failed

---

### GET /api/me
**Description:** Get current authenticated user's profile information

**Authentication Required:** Yes

**Query Parameters:** None

**Response (200 OK):**
```json
{
  "id": "user_id",
  "email": "user@example.com",
  "name": "User Name",
  "phoneNumber": "+1234567890",
  "role": "USER",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-03-08T14:45:00Z"
}
```

**Error Responses:**
- 401 Unauthorized - Invalid or missing token

---

### PUT /api/me
**Description:** Update current user's profile information

**Authentication Required:** Yes

**Request Body:**
```json
{
  "name": "Updated Name",
  "email": "newemail@example.com",
  "phoneNumber": "+1987654321"
}
```

**Response (200 OK):**
```json
{
  "id": "user_id",
  "email": "newemail@example.com",
  "name": "Updated Name",
  "phoneNumber": "+1987654321",
  "role": "USER",
  "updatedAt": "2024-03-08T15:00:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body
- 401 Unauthorized - Invalid or missing token
- 409 Conflict - Email already exists

---

## Expenses

### GET /api/expenses/summary
**Description:** Get a summary of expenses (total, by category, average, etc.)

**Authentication Required:** Yes

**Query Parameters:**
- `startDate` (optional, ISO 8601 format) - Filter expenses from this date
- `endDate` (optional, ISO 8601 format) - Filter expenses until this date
- `category` (optional, string) - Filter by expense category

**Response (200 OK):**
```json
{
  "totalExpenses": 2500.50,
  "averageExpense": 125.25,
  "expenseCount": 20,
  "byCategory": {
    "Food": 450.00,
    "Transportation": 300.00,
    "Entertainment": 200.00,
    "Utilities": 150.00,
    "Other": 1400.50
  },
  "monthlyTrend": [
    {
      "month": "2024-01",
      "total": 800.00
    },
    {
      "month": "2024-02",
      "total": 950.00
    }
  ]
}
```

---

### GET /api/expenses
**Description:** List all expenses for the authenticated user

**Authentication Required:** Yes

**Query Parameters:**
- `page` (optional, integer, default: 1) - Pagination page number
- `limit` (optional, integer, default: 20) - Items per page
- `startDate` (optional, ISO 8601 format) - Filter expenses from this date
- `endDate` (optional, ISO 8601 format) - Filter expenses until this date
- `category` (optional, string) - Filter by category
- `sortBy` (optional, string: "date", "amount", "category") - Sort field
- `sortOrder` (optional, string: "asc", "desc") - Sort order

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "expense_id_1",
      "userId": "user_id",
      "amount": 45.99,
      "category": "Food",
      "description": "Groceries",
      "date": "2024-03-08T10:30:00Z",
      "paymentMethod": "CREDIT_CARD",
      "tags": ["groceries", "weekly"],
      "createdAt": "2024-03-08T10:30:00Z",
      "updatedAt": "2024-03-08T10:30:00Z"
    },
    {
      "id": "expense_id_2",
      "userId": "user_id",
      "amount": 12.50,
      "category": "Transportation",
      "description": "Taxi ride",
      "date": "2024-03-07T18:15:00Z",
      "paymentMethod": "DEBIT_CARD",
      "tags": ["transport"],
      "createdAt": "2024-03-07T18:15:00Z",
      "updatedAt": "2024-03-07T18:15:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 125,
    "pages": 7
  }
}
```

---

### GET /api/expenses/{id}
**Description:** Get a specific expense by ID

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Expense ID

**Response (200 OK):**
```json
{
  "id": "expense_id_1",
  "userId": "user_id",
  "amount": 45.99,
  "category": "Food",
  "description": "Groceries at Whole Foods",
  "date": "2024-03-08T10:30:00Z",
  "paymentMethod": "CREDIT_CARD",
  "tags": ["groceries", "weekly", "organic"],
  "attachments": ["receipt_url"],
  "notes": "Weekly grocery shopping",
  "createdAt": "2024-03-08T10:30:00Z",
  "updatedAt": "2024-03-08T10:30:00Z"
}
```

**Error Responses:**
- 404 Not Found - Expense not found or unauthorized access

---

### POST /api/expenses
**Description:** Create a new expense

**Authentication Required:** Yes

**Request Body:**
```json
{
  "amount": 45.99,
  "category": "Food",
  "description": "Groceries",
  "date": "2024-03-08T10:30:00Z",
  "paymentMethod": "CREDIT_CARD",
  "tags": ["groceries", "weekly"],
  "notes": "Weekly grocery shopping"
}
```

**Response (201 Created):**
```json
{
  "id": "expense_id_new",
  "userId": "user_id",
  "amount": 45.99,
  "category": "Food",
  "description": "Groceries",
  "date": "2024-03-08T10:30:00Z",
  "paymentMethod": "CREDIT_CARD",
  "tags": ["groceries", "weekly"],
  "createdAt": "2024-03-08T10:30:00Z",
  "updatedAt": "2024-03-08T10:30:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body or missing required fields
- 401 Unauthorized - Invalid or missing token

---

### PUT /api/expenses/{id}
**Description:** Update an existing expense

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Expense ID

**Request Body:**
```json
{
  "amount": 50.99,
  "category": "Food",
  "description": "Updated groceries description",
  "date": "2024-03-08T10:30:00Z",
  "paymentMethod": "DEBIT_CARD",
  "tags": ["groceries", "weekly", "updated"],
  "notes": "Updated note"
}
```

**Response (200 OK):**
```json
{
  "id": "expense_id",
  "userId": "user_id",
  "amount": 50.99,
  "category": "Food",
  "description": "Updated groceries description",
  "date": "2024-03-08T10:30:00Z",
  "paymentMethod": "DEBIT_CARD",
  "tags": ["groceries", "weekly", "updated"],
  "updatedAt": "2024-03-08T11:00:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body
- 404 Not Found - Expense not found or unauthorized access

---

### DELETE /api/expenses/{id}
**Description:** Delete an expense

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Expense ID

**Response (204 No Content)**

**Error Responses:**
- 404 Not Found - Expense not found or unauthorized access

---

## Investments

### GET /api/investments/portfolio
**Description:** Get investment portfolio summary with allocation and performance metrics

**Authentication Required:** Yes

**Query Parameters:**
- `includePerformance` (optional, boolean, default: true) - Include performance calculations

**Response (200 OK):**
```json
{
  "totalValue": 25000.00,
  "totalInvested": 20000.00,
  "gainLoss": 5000.00,
  "gainLossPercentage": 25.0,
  "allocations": [
    {
      "type": "STOCKS",
      "value": 10000.00,
      "percentage": 40.0
    },
    {
      "type": "BONDS",
      "value": 8000.00,
      "percentage": 32.0
    },
    {
      "type": "MUTUAL_FUNDS",
      "value": 7000.00,
      "percentage": 28.0
    }
  ],
  "topPerformers": [
    {
      "id": "investment_id",
      "symbol": "AAPL",
      "gainLossPercentage": 35.5
    }
  ]
}
```

---

### GET /api/investments
**Description:** List all investments for the authenticated user

**Authentication Required:** Yes

**Query Parameters:**
- `page` (optional, integer, default: 1) - Pagination page number
- `limit` (optional, integer, default: 20) - Items per page
- `type` (optional, string: "STOCKS", "BONDS", "MUTUAL_FUNDS", "CRYPTO", "REAL_ESTATE", "OTHER") - Filter by investment type
- `sortBy` (optional, string: "date", "amount", "performance") - Sort field
- `sortOrder` (optional, string: "asc", "desc") - Sort order

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "investment_id_1",
      "userId": "user_id",
      "name": "Apple Inc.",
      "symbol": "AAPL",
      "type": "STOCKS",
      "quantity": 10.0,
      "purchasePrice": 150.00,
      "currentPrice": 200.00,
      "currentValue": 2000.00,
      "gainLoss": 500.00,
      "gainLossPercentage": 33.33,
      "purchaseDate": "2024-01-15T00:00:00Z",
      "broker": "Fidelity",
      "createdAt": "2024-01-15T10:30:00Z",
      "updatedAt": "2024-03-08T14:45:00Z"
    },
    {
      "id": "investment_id_2",
      "userId": "user_id",
      "name": "Vanguard Total Bond Market",
      "symbol": "BND",
      "type": "MUTUAL_FUNDS",
      "quantity": 50.0,
      "purchasePrice": 80.00,
      "currentPrice": 82.50,
      "currentValue": 4125.00,
      "gainLoss": 125.00,
      "gainLossPercentage": 3.13,
      "purchaseDate": "2024-02-01T00:00:00Z",
      "broker": "Vanguard",
      "createdAt": "2024-02-01T10:30:00Z",
      "updatedAt": "2024-03-08T14:45:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 12,
    "pages": 1
  }
}
```

---

### GET /api/investments/{id}
**Description:** Get a specific investment by ID

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Investment ID

**Response (200 OK):**
```json
{
  "id": "investment_id_1",
  "userId": "user_id",
  "name": "Apple Inc.",
  "symbol": "AAPL",
  "type": "STOCKS",
  "quantity": 10.0,
  "purchasePrice": 150.00,
  "currentPrice": 200.00,
  "currentValue": 2000.00,
  "gainLoss": 500.00,
  "gainLossPercentage": 33.33,
  "purchaseDate": "2024-01-15T00:00:00Z",
  "broker": "Fidelity",
  "notes": "Long-term investment",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-03-08T14:45:00Z"
}
```

**Error Responses:**
- 404 Not Found - Investment not found or unauthorized access

---

### POST /api/investments
**Description:** Create a new investment

**Authentication Required:** Yes

**Request Body:**
```json
{
  "name": "Apple Inc.",
  "symbol": "AAPL",
  "type": "STOCKS",
  "quantity": 10.0,
  "purchasePrice": 150.00,
  "currentPrice": 200.00,
  "purchaseDate": "2024-01-15T00:00:00Z",
  "broker": "Fidelity",
  "notes": "Long-term investment"
}
```

**Response (201 Created):**
```json
{
  "id": "investment_id_new",
  "userId": "user_id",
  "name": "Apple Inc.",
  "symbol": "AAPL",
  "type": "STOCKS",
  "quantity": 10.0,
  "purchasePrice": 150.00,
  "currentPrice": 200.00,
  "currentValue": 2000.00,
  "gainLoss": 500.00,
  "gainLossPercentage": 33.33,
  "purchaseDate": "2024-01-15T00:00:00Z",
  "broker": "Fidelity",
  "createdAt": "2024-03-08T10:30:00Z",
  "updatedAt": "2024-03-08T10:30:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body or missing required fields
- 401 Unauthorized - Invalid or missing token

---

### PUT /api/investments/{id}
**Description:** Update an existing investment

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Investment ID

**Request Body:**
```json
{
  "quantity": 12.0,
  "currentPrice": 210.00,
  "broker": "Charles Schwab"
}
```

**Response (200 OK):**
```json
{
  "id": "investment_id",
  "userId": "user_id",
  "name": "Apple Inc.",
  "symbol": "AAPL",
  "type": "STOCKS",
  "quantity": 12.0,
  "purchasePrice": 150.00,
  "currentPrice": 210.00,
  "currentValue": 2520.00,
  "gainLoss": 720.00,
  "gainLossPercentage": 40.0,
  "purchaseDate": "2024-01-15T00:00:00Z",
  "broker": "Charles Schwab",
  "updatedAt": "2024-03-08T11:00:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body
- 404 Not Found - Investment not found or unauthorized access

---

### DELETE /api/investments/{id}
**Description:** Delete an investment

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Investment ID

**Response (204 No Content)**

**Error Responses:**
- 404 Not Found - Investment not found or unauthorized access

---

## Salary

### GET /api/salary/history
**Description:** Get salary history with trends and comparisons

**Authentication Required:** Yes

**Query Parameters:**
- `months` (optional, integer, default: 12) - Number of months to include in history

**Response (200 OK):**
```json
{
  "currentSalary": 5000.00,
  "averageSalary": 4800.00,
  "totalEarnings": 57600.00,
  "history": [
    {
      "id": "salary_id_1",
      "amount": 4500.00,
      "currency": "USD",
      "startDate": "2023-01-01T00:00:00Z",
      "endDate": "2023-12-31T23:59:59Z",
      "frequency": "MONTHLY",
      "employer": "Tech Company Inc.",
      "jobTitle": "Software Engineer"
    },
    {
      "id": "salary_id_2",
      "amount": 5000.00,
      "currency": "USD",
      "startDate": "2024-01-01T00:00:00Z",
      "endDate": null,
      "frequency": "MONTHLY",
      "employer": "Tech Company Inc.",
      "jobTitle": "Senior Software Engineer"
    }
  ],
  "salaryGrowth": 11.11
}
```

---

### GET /api/salary
**Description:** List all salary records for the authenticated user

**Authentication Required:** Yes

**Query Parameters:**
- `page` (optional, integer, default: 1) - Pagination page number
- `limit` (optional, integer, default: 20) - Items per page
- `sortBy` (optional, string: "date", "amount") - Sort field
- `sortOrder` (optional, string: "asc", "desc") - Sort order

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "salary_id_1",
      "userId": "user_id",
      "amount": 5000.00,
      "currency": "USD",
      "startDate": "2024-01-01T00:00:00Z",
      "endDate": null,
      "frequency": "MONTHLY",
      "employer": "Tech Company Inc.",
      "jobTitle": "Senior Software Engineer",
      "department": "Engineering",
      "notes": "Base salary",
      "createdAt": "2024-01-01T10:30:00Z",
      "updatedAt": "2024-03-08T14:45:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 1,
    "pages": 1
  }
}
```

---

### GET /api/salary/{id}
**Description:** Get a specific salary record by ID

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Salary ID

**Response (200 OK):**
```json
{
  "id": "salary_id_1",
  "userId": "user_id",
  "amount": 5000.00,
  "currency": "USD",
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": null,
  "frequency": "MONTHLY",
  "employer": "Tech Company Inc.",
  "jobTitle": "Senior Software Engineer",
  "department": "Engineering",
  "notes": "Base salary",
  "createdAt": "2024-01-01T10:30:00Z",
  "updatedAt": "2024-03-08T14:45:00Z"
}
```

**Error Responses:**
- 404 Not Found - Salary record not found or unauthorized access

---

### POST /api/salary
**Description:** Create a new salary record

**Authentication Required:** Yes

**Request Body:**
```json
{
  "amount": 5000.00,
  "currency": "USD",
  "startDate": "2024-01-01T00:00:00Z",
  "frequency": "MONTHLY",
  "employer": "Tech Company Inc.",
  "jobTitle": "Senior Software Engineer",
  "department": "Engineering",
  "notes": "Base salary"
}
```

**Response (201 Created):**
```json
{
  "id": "salary_id_new",
  "userId": "user_id",
  "amount": 5000.00,
  "currency": "USD",
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": null,
  "frequency": "MONTHLY",
  "employer": "Tech Company Inc.",
  "jobTitle": "Senior Software Engineer",
  "department": "Engineering",
  "createdAt": "2024-03-08T10:30:00Z",
  "updatedAt": "2024-03-08T10:30:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body or missing required fields
- 401 Unauthorized - Invalid or missing token

---

### PUT /api/salary/{id}
**Description:** Update a salary record (e.g., end previous salary when updating to new one)

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Salary ID

**Request Body:**
```json
{
  "amount": 5500.00,
  "endDate": "2024-02-29T23:59:59Z"
}
```

**Response (200 OK):**
```json
{
  "id": "salary_id",
  "userId": "user_id",
  "amount": 5500.00,
  "currency": "USD",
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": "2024-02-29T23:59:59Z",
  "frequency": "MONTHLY",
  "employer": "Tech Company Inc.",
  "jobTitle": "Senior Software Engineer",
  "updatedAt": "2024-03-08T11:00:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body
- 404 Not Found - Salary record not found or unauthorized access

---

### DELETE /api/salary/{id}
**Description:** Delete a salary record

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Salary ID

**Response (204 No Content)**

**Error Responses:**
- 404 Not Found - Salary record not found or unauthorized access

---

## Debts

### GET /api/debts/schedule
**Description:** Get debt payoff schedule with projections

**Authentication Required:** Yes

**Query Parameters:**
- `includeProjection` (optional, boolean, default: true) - Include payoff projections

**Response (200 OK):**
```json
{
  "totalDebt": 15000.00,
  "totalMonthlyPayment": 500.00,
  "averageInterestRate": 5.5,
  "debts": [
    {
      "id": "debt_id_1",
      "name": "Car Loan",
      "balance": 10000.00,
      "interestRate": 4.5,
      "monthlyPayment": 300.00,
      "remainingMonths": 35,
      "payoffDate": "2027-02-08T00:00:00Z"
    },
    {
      "id": "debt_id_2",
      "name": "Credit Card",
      "balance": 5000.00,
      "interestRate": 18.0,
      "monthlyPayment": 200.00,
      "remainingMonths": 28,
      "payoffDate": "2026-07-08T00:00:00Z"
    }
  ]
}
```

---

### GET /api/debts
**Description:** List all debts for the authenticated user

**Authentication Required:** Yes

**Query Parameters:**
- `page` (optional, integer, default: 1) - Pagination page number
- `limit` (optional, integer, default: 20) - Items per page
- `status` (optional, string: "ACTIVE", "PAID_OFF", "CLOSED") - Filter by status
- `sortBy` (optional, string: "date", "balance", "interestRate") - Sort field
- `sortOrder` (optional, string: "asc", "desc") - Sort order

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "debt_id_1",
      "userId": "user_id",
      "name": "Car Loan",
      "debtType": "AUTO_LOAN",
      "balance": 10000.00,
      "originalAmount": 15000.00,
      "interestRate": 4.5,
      "monthlyPayment": 300.00,
      "dueDate": "2027-02-08T00:00:00Z",
      "creditor": "Bank of America",
      "status": "ACTIVE",
      "createdAt": "2022-02-08T10:30:00Z",
      "updatedAt": "2024-03-08T14:45:00Z"
    },
    {
      "id": "debt_id_2",
      "userId": "user_id",
      "name": "Credit Card",
      "debtType": "CREDIT_CARD",
      "balance": 5000.00,
      "originalAmount": 8000.00,
      "interestRate": 18.0,
      "monthlyPayment": 200.00,
      "dueDate": "2026-07-08T00:00:00Z",
      "creditor": "Chase",
      "status": "ACTIVE",
      "createdAt": "2024-01-01T10:30:00Z",
      "updatedAt": "2024-03-08T14:45:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 2,
    "pages": 1
  }
}
```

---

### GET /api/debts/{id}
**Description:** Get a specific debt by ID

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Debt ID

**Response (200 OK):**
```json
{
  "id": "debt_id_1",
  "userId": "user_id",
  "name": "Car Loan",
  "debtType": "AUTO_LOAN",
  "balance": 10000.00,
  "originalAmount": 15000.00,
  "interestRate": 4.5,
  "monthlyPayment": 300.00,
  "dueDate": "2027-02-08T00:00:00Z",
  "creditor": "Bank of America",
  "status": "ACTIVE",
  "notes": "Monthly payment due on 8th",
  "createdAt": "2022-02-08T10:30:00Z",
  "updatedAt": "2024-03-08T14:45:00Z"
}
```

**Error Responses:**
- 404 Not Found - Debt not found or unauthorized access

---

### POST /api/debts
**Description:** Create a new debt record

**Authentication Required:** Yes

**Request Body:**
```json
{
  "name": "Car Loan",
  "debtType": "AUTO_LOAN",
  "balance": 10000.00,
  "originalAmount": 15000.00,
  "interestRate": 4.5,
  "monthlyPayment": 300.00,
  "dueDate": "2027-02-08T00:00:00Z",
  "creditor": "Bank of America",
  "notes": "Monthly payment due on 8th"
}
```

**Response (201 Created):**
```json
{
  "id": "debt_id_new",
  "userId": "user_id",
  "name": "Car Loan",
  "debtType": "AUTO_LOAN",
  "balance": 10000.00,
  "originalAmount": 15000.00,
  "interestRate": 4.5,
  "monthlyPayment": 300.00,
  "dueDate": "2027-02-08T00:00:00Z",
  "creditor": "Bank of America",
  "status": "ACTIVE",
  "createdAt": "2024-03-08T10:30:00Z",
  "updatedAt": "2024-03-08T10:30:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body or missing required fields
- 401 Unauthorized - Invalid or missing token

---

### PUT /api/debts/{id}
**Description:** Update an existing debt

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Debt ID

**Request Body:**
```json
{
  "balance": 9500.00,
  "monthlyPayment": 310.00,
  "status": "ACTIVE"
}
```

**Response (200 OK):**
```json
{
  "id": "debt_id",
  "userId": "user_id",
  "name": "Car Loan",
  "debtType": "AUTO_LOAN",
  "balance": 9500.00,
  "originalAmount": 15000.00,
  "interestRate": 4.5,
  "monthlyPayment": 310.00,
  "dueDate": "2027-02-08T00:00:00Z",
  "creditor": "Bank of America",
  "status": "ACTIVE",
  "updatedAt": "2024-03-08T11:00:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body
- 404 Not Found - Debt not found or unauthorized access

---

### DELETE /api/debts/{id}
**Description:** Delete a debt

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Debt ID

**Response (204 No Content)**

**Error Responses:**
- 404 Not Found - Debt not found or unauthorized access

---

### GET /api/debts/{id}/payments
**Description:** Get payment history for a specific debt

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Debt ID

**Query Parameters:**
- `page` (optional, integer, default: 1) - Pagination page number
- `limit` (optional, integer, default: 20) - Items per page
- `sortBy` (optional, string: "date", "amount") - Sort field
- `sortOrder` (optional, string: "asc", "desc") - Sort order

**Response (200 OK):**
```json
{
  "debtId": "debt_id_1",
  "debtName": "Car Loan",
  "totalPaid": 2400.00,
  "paymentCount": 8,
  "data": [
    {
      "id": "payment_id_1",
      "debtId": "debt_id_1",
      "amount": 300.00,
      "date": "2024-03-08T10:30:00Z",
      "principalPaid": 275.00,
      "interestPaid": 25.00,
      "notes": "Regular monthly payment",
      "createdAt": "2024-03-08T10:30:00Z"
    },
    {
      "id": "payment_id_2",
      "debtId": "debt_id_1",
      "amount": 300.00,
      "date": "2024-02-08T10:30:00Z",
      "principalPaid": 274.50,
      "interestPaid": 25.50,
      "createdAt": "2024-02-08T10:30:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 8,
    "pages": 1
  }
}
```

---

### POST /api/debts/{id}/payments
**Description:** Add a payment to a debt

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Debt ID

**Request Body:**
```json
{
  "amount": 300.00,
  "date": "2024-03-08T10:30:00Z",
  "notes": "Regular monthly payment"
}
```

**Response (201 Created):**
```json
{
  "id": "payment_id_new",
  "debtId": "debt_id_1",
  "amount": 300.00,
  "date": "2024-03-08T10:30:00Z",
  "principalPaid": 275.00,
  "interestPaid": 25.00,
  "notes": "Regular monthly payment",
  "createdAt": "2024-03-08T10:30:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body or missing required fields
- 404 Not Found - Debt not found or unauthorized access

---

## Recurring Transactions

### GET /api/recurring
**Description:** List all recurring transactions for the authenticated user

**Authentication Required:** Yes

**Query Parameters:**
- `page` (optional, integer, default: 1) - Pagination page number
- `limit` (optional, integer, default: 20) - Items per page
- `status` (optional, string: "ACTIVE", "PAUSED", "COMPLETED") - Filter by status
- `type` (optional, string: "EXPENSE", "INCOME", "DEBT_PAYMENT") - Filter by type
- `sortBy` (optional, string: "date", "amount", "nextDate") - Sort field
- `sortOrder` (optional, string: "asc", "desc") - Sort order

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "recurring_id_1",
      "userId": "user_id",
      "name": "Netflix Subscription",
      "type": "EXPENSE",
      "amount": 15.99,
      "frequency": "MONTHLY",
      "category": "Entertainment",
      "nextDueDate": "2024-04-08T00:00:00Z",
      "lastProcessedDate": "2024-03-08T00:00:00Z",
      "status": "ACTIVE",
      "startDate": "2023-01-08T00:00:00Z",
      "endDate": null,
      "createdAt": "2023-01-08T10:30:00Z",
      "updatedAt": "2024-03-08T14:45:00Z"
    },
    {
      "id": "recurring_id_2",
      "userId": "user_id",
      "name": "Gym Membership",
      "type": "EXPENSE",
      "amount": 50.00,
      "frequency": "MONTHLY",
      "category": "Health & Fitness",
      "nextDueDate": "2024-03-15T00:00:00Z",
      "lastProcessedDate": "2024-02-15T00:00:00Z",
      "status": "ACTIVE",
      "startDate": "2023-06-15T00:00:00Z",
      "endDate": null,
      "createdAt": "2023-06-15T10:30:00Z",
      "updatedAt": "2024-03-08T14:45:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 2,
    "pages": 1
  }
}
```

---

### GET /api/recurring/{id}
**Description:** Get a specific recurring transaction by ID

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Recurring transaction ID

**Response (200 OK):**
```json
{
  "id": "recurring_id_1",
  "userId": "user_id",
  "name": "Netflix Subscription",
  "type": "EXPENSE",
  "amount": 15.99,
  "frequency": "MONTHLY",
  "category": "Entertainment",
  "description": "Monthly streaming service",
  "nextDueDate": "2024-04-08T00:00:00Z",
  "lastProcessedDate": "2024-03-08T00:00:00Z",
  "status": "ACTIVE",
  "startDate": "2023-01-08T00:00:00Z",
  "endDate": null,
  "notes": "Auto-renew enabled",
  "createdAt": "2023-01-08T10:30:00Z",
  "updatedAt": "2024-03-08T14:45:00Z"
}
```

**Error Responses:**
- 404 Not Found - Recurring transaction not found or unauthorized access

---

### POST /api/recurring
**Description:** Create a new recurring transaction

**Authentication Required:** Yes

**Request Body:**
```json
{
  "name": "Netflix Subscription",
  "type": "EXPENSE",
  "amount": 15.99,
  "frequency": "MONTHLY",
  "category": "Entertainment",
  "description": "Monthly streaming service",
  "startDate": "2023-01-08T00:00:00Z",
  "nextDueDate": "2024-04-08T00:00:00Z",
  "notes": "Auto-renew enabled"
}
```

**Response (201 Created):**
```json
{
  "id": "recurring_id_new",
  "userId": "user_id",
  "name": "Netflix Subscription",
  "type": "EXPENSE",
  "amount": 15.99,
  "frequency": "MONTHLY",
  "category": "Entertainment",
  "description": "Monthly streaming service",
  "nextDueDate": "2024-04-08T00:00:00Z",
  "lastProcessedDate": null,
  "status": "ACTIVE",
  "startDate": "2023-01-08T00:00:00Z",
  "endDate": null,
  "createdAt": "2024-03-08T10:30:00Z",
  "updatedAt": "2024-03-08T10:30:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body or missing required fields
- 401 Unauthorized - Invalid or missing token

---

### PUT /api/recurring/{id}
**Description:** Update a recurring transaction

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Recurring transaction ID

**Request Body:**
```json
{
  "amount": 16.99,
  "status": "ACTIVE",
  "endDate": "2024-12-31T23:59:59Z"
}
```

**Response (200 OK):**
```json
{
  "id": "recurring_id",
  "userId": "user_id",
  "name": "Netflix Subscription",
  "type": "EXPENSE",
  "amount": 16.99,
  "frequency": "MONTHLY",
  "category": "Entertainment",
  "nextDueDate": "2024-04-08T00:00:00Z",
  "lastProcessedDate": "2024-03-08T00:00:00Z",
  "status": "ACTIVE",
  "startDate": "2023-01-08T00:00:00Z",
  "endDate": "2024-12-31T23:59:59Z",
  "updatedAt": "2024-03-08T11:00:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body
- 404 Not Found - Recurring transaction not found or unauthorized access

---

### DELETE /api/recurring/{id}
**Description:** Delete a recurring transaction

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Recurring transaction ID

**Response (204 No Content)**

**Error Responses:**
- 404 Not Found - Recurring transaction not found or unauthorized access

---

### POST /api/recurring/process
**Description:** Process all due recurring transactions and create corresponding entries

**Authentication Required:** Yes

**Query Parameters:**
- `dryRun` (optional, boolean, default: false) - Simulate processing without creating entries

**Request Body:** Empty

**Response (200 OK):**
```json
{
  "processed": 3,
  "failed": 0,
  "processedTransactions": [
    {
      "recurringId": "recurring_id_1",
      "name": "Netflix Subscription",
      "type": "EXPENSE",
      "amount": 15.99,
      "createdExpenseId": "expense_id_123",
      "status": "SUCCESS"
    },
    {
      "recurringId": "recurring_id_2",
      "name": "Gym Membership",
      "type": "EXPENSE",
      "amount": 50.00,
      "createdExpenseId": "expense_id_124",
      "status": "SUCCESS"
    },
    {
      "recurringId": "recurring_id_3",
      "name": "Monthly Salary",
      "type": "INCOME",
      "amount": 5000.00,
      "createdSalaryId": "salary_id_456",
      "status": "SUCCESS"
    }
  ],
  "nextProcessDate": "2024-04-08T00:00:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid query parameters
- 401 Unauthorized - Invalid or missing token

---

## Budgets

### GET /api/budgets/status
**Description:** Get budget status and progress against all budgets

**Authentication Required:** Yes

**Query Parameters:**
- `month` (optional, ISO 8601 date format, default: current month) - Budget month to check

**Response (200 OK):**
```json
{
  "month": "2024-03",
  "budgets": [
    {
      "id": "budget_id_1",
      "category": "Food",
      "limit": 500.00,
      "spent": 350.75,
      "remaining": 149.25,
      "percentageUsed": 70.15,
      "status": "ON_TRACK",
      "transactions": 12
    },
    {
      "id": "budget_id_2",
      "category": "Entertainment",
      "limit": 200.00,
      "spent": 220.50,
      "remaining": -20.50,
      "percentageUsed": 110.25,
      "status": "EXCEEDED",
      "transactions": 5
    },
    {
      "id": "budget_id_3",
      "category": "Transportation",
      "limit": 300.00,
      "spent": 150.00,
      "remaining": 150.00,
      "percentageUsed": 50.0,
      "status": "ON_TRACK",
      "transactions": 3
    }
  ],
  "totalBudget": 1000.00,
  "totalSpent": 721.25,
  "totalRemaining": 278.75,
  "overallStatus": "ON_TRACK"
}
```

---

### GET /api/budgets
**Description:** List all budgets for the authenticated user

**Authentication Required:** Yes

**Query Parameters:**
- `page` (optional, integer, default: 1) - Pagination page number
- `limit` (optional, integer, default: 20) - Items per page
- `month` (optional, ISO 8601 date format) - Filter by month
- `sortBy` (optional, string: "category", "limit", "spent") - Sort field
- `sortOrder` (optional, string: "asc", "desc") - Sort order

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "budget_id_1",
      "userId": "user_id",
      "category": "Food",
      "limit": 500.00,
      "month": "2024-03",
      "createdAt": "2024-03-01T10:30:00Z",
      "updatedAt": "2024-03-08T14:45:00Z"
    },
    {
      "id": "budget_id_2",
      "userId": "user_id",
      "category": "Entertainment",
      "limit": 200.00,
      "month": "2024-03",
      "createdAt": "2024-03-01T10:30:00Z",
      "updatedAt": "2024-03-08T14:45:00Z"
    },
    {
      "id": "budget_id_3",
      "userId": "user_id",
      "category": "Transportation",
      "limit": 300.00,
      "month": "2024-03",
      "createdAt": "2024-03-01T10:30:00Z",
      "updatedAt": "2024-03-08T14:45:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 3,
    "pages": 1
  }
}
```

---

### POST /api/budgets
**Description:** Create or update (upsert) a budget for a specific category and month

**Authentication Required:** Yes

**Request Body:**
```json
{
  "category": "Food",
  "limit": 500.00,
  "month": "2024-03"
}
```

**Response (201 Created or 200 OK):**
```json
{
  "id": "budget_id_1",
  "userId": "user_id",
  "category": "Food",
  "limit": 500.00,
  "month": "2024-03",
  "createdAt": "2024-03-01T10:30:00Z",
  "updatedAt": "2024-03-08T14:45:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body or missing required fields
- 401 Unauthorized - Invalid or missing token

---

### DELETE /api/budgets/{id}
**Description:** Delete a budget

**Authentication Required:** Yes

**Path Parameters:**
- `id` (string, required) - Budget ID

**Response (204 No Content)**

**Error Responses:**
- 404 Not Found - Budget not found or unauthorized access

---

## Analytics

### GET /api/analytics/monthly
**Description:** Get monthly financial analytics including income, expenses, and net cashflow

**Authentication Required:** Yes

**Query Parameters:**
- `months` (optional, integer, default: 12) - Number of months to include
- `includeForecast` (optional, boolean, default: false) - Include forecast data

**Response (200 OK):**
```json
{
  "months": [
    {
      "month": "2024-03",
      "income": 5000.00,
      "expenses": 1500.00,
      "netCashflow": 3500.00,
      "expensesByCategory": {
        "Food": 350.00,
        "Transportation": 200.00,
        "Entertainment": 150.00,
        "Utilities": 100.00,
        "Other": 700.00
      },
      "investmentActivity": 1000.00,
      "debtPayments": 500.00
    },
    {
      "month": "2024-02",
      "income": 5000.00,
      "expenses": 1400.00,
      "netCashflow": 3600.00,
      "expensesByCategory": {
        "Food": 320.00,
        "Transportation": 200.00,
        "Entertainment": 100.00,
        "Utilities": 100.00,
        "Other": 680.00
      },
      "investmentActivity": 1200.00,
      "debtPayments": 500.00
    }
  ],
  "summary": {
    "averageIncome": 5000.00,
    "averageExpenses": 1450.00,
    "averageNetCashflow": 3550.00,
    "totalIncome": 60000.00,
    "totalExpenses": 17400.00,
    "savingsRate": 71.0
  }
}
```

---

### GET /api/analytics/yearly
**Description:** Get yearly financial analytics and trends

**Authentication Required:** Yes

**Query Parameters:**
- `years` (optional, integer, default: 3) - Number of years to include
- `includeForecast` (optional, boolean, default: false) - Include forecast data

**Response (200 OK):**
```json
{
  "years": [
    {
      "year": "2024",
      "income": 15000.00,
      "expenses": 4500.00,
      "netCashflow": 10500.00,
      "investmentActivity": 3500.00,
      "debtPayments": 1500.00,
      "topExpenseCategory": "Food",
      "topExpenseCategoryAmount": 1050.00
    },
    {
      "year": "2023",
      "income": 60000.00,
      "expenses": 18000.00,
      "netCashflow": 42000.00,
      "investmentActivity": 10000.00,
      "debtPayments": 6000.00,
      "topExpenseCategory": "Food",
      "topExpenseCategoryAmount": 4500.00
    },
    {
      "year": "2022",
      "income": 58000.00,
      "expenses": 17500.00,
      "netCashflow": 40500.00,
      "investmentActivity": 9000.00,
      "debtPayments": 5000.00,
      "topExpenseCategory": "Entertainment",
      "topExpenseCategoryAmount": 3500.00
    }
  ],
  "summary": {
    "averageYearlyIncome": 44333.33,
    "averageYearlyExpenses": 13333.33,
    "averageYearlyNetCashflow": 31000.00,
    "incomeGrowth": 25.0,
    "expenseGrowth": 2.86
  }
}
```

---

### GET /api/analytics/networth
**Description:** Get comprehensive net worth calculation and history

**Authentication Required:** Yes

**Query Parameters:**
- `includeHistory` (optional, boolean, default: true) - Include historical net worth data

**Response (200 OK):**
```json
{
  "currentNetWorth": 50000.00,
  "assets": {
    "investments": {
      "stocks": 20000.00,
      "bonds": 8000.00,
      "mutualFunds": 5000.00,
      "crypto": 2000.00,
      "realEstate": 0.00,
      "other": 0.00,
      "total": 35000.00
    },
    "bankAccounts": {
      "savings": 10000.00,
      "checking": 5000.00,
      "total": 15000.00
    },
    "totalAssets": 50000.00
  },
  "liabilities": {
    "debts": {
      "autoLoan": 10000.00,
      "creditCard": 5000.00,
      "studentLoan": 0.00,
      "personalLoan": 0.00,
      "mortgage": 0.00,
      "other": 0.00,
      "total": 15000.00
    },
    "totalLiabilities": 15000.00
  },
  "netWorth": 35000.00,
  "history": [
    {
      "date": "2024-03-08T00:00:00Z",
      "netWorth": 35000.00,
      "assets": 50000.00,
      "liabilities": 15000.00
    },
    {
      "date": "2024-02-08T00:00:00Z",
      "netWorth": 34500.00,
      "assets": 49500.00,
      "liabilities": 15000.00
    },
    {
      "date": "2024-01-08T00:00:00Z",
      "netWorth": 33000.00,
      "assets": 48000.00,
      "liabilities": 15000.00
    }
  ],
  "monthlyChange": 500.00,
  "monthlyChangePercentage": 1.45
}
```

---

## Export

### GET /api/export/expenses
**Description:** Export all expenses as CSV file

**Authentication Required:** Yes

**Query Parameters:**
- `startDate` (optional, ISO 8601 format) - Export from this date
- `endDate` (optional, ISO 8601 format) - Export until this date
- `category` (optional, string) - Filter by category before export

**Response (200 OK - CSV file)**
```
Content-Type: text/csv
Content-Disposition: attachment; filename="expenses.csv"

id,amount,category,description,date,paymentMethod,tags
expense_id_1,45.99,Food,Groceries,2024-03-08T10:30:00Z,CREDIT_CARD,"groceries,weekly"
expense_id_2,12.50,Transportation,Taxi ride,2024-03-07T18:15:00Z,DEBIT_CARD,"transport"
...
```

---

### GET /api/export/investments
**Description:** Export all investments as CSV file

**Authentication Required:** Yes

**Query Parameters:**
- `type` (optional, string) - Filter by investment type before export
- `includePerformance` (optional, boolean, default: true) - Include performance columns

**Response (200 OK - CSV file)**
```
Content-Type: text/csv
Content-Disposition: attachment; filename="investments.csv"

id,name,symbol,type,quantity,purchasePrice,currentPrice,currentValue,gainLoss,gainLossPercentage,purchaseDate,broker
investment_id_1,Apple Inc.,AAPL,STOCKS,10.0,150.00,200.00,2000.00,500.00,33.33,2024-01-15T00:00:00Z,Fidelity
...
```

---

### GET /api/export/salary
**Description:** Export all salary records as CSV file

**Authentication Required:** Yes

**Query Parameters:**
- `startDate` (optional, ISO 8601 format) - Export from this date
- `endDate` (optional, ISO 8601 format) - Export until this date

**Response (200 OK - CSV file)**
```
Content-Type: text/csv
Content-Disposition: attachment; filename="salary.csv"

id,amount,currency,startDate,endDate,frequency,employer,jobTitle,department
salary_id_1,5000.00,USD,2024-01-01T00:00:00Z,,MONTHLY,Tech Company Inc.,Senior Software Engineer,Engineering
...
```

---

### GET /api/export/debts
**Description:** Export all debts as CSV file

**Authentication Required:** Yes

**Query Parameters:**
- `status` (optional, string) - Filter by status before export
- `includePayments` (optional, boolean, default: false) - Include payment history

**Response (200 OK - CSV file)**
```
Content-Type: text/csv
Content-Disposition: attachment; filename="debts.csv"

id,name,debtType,balance,originalAmount,interestRate,monthlyPayment,dueDate,creditor,status
debt_id_1,Car Loan,AUTO_LOAN,10000.00,15000.00,4.5,300.00,2027-02-08T00:00:00Z,Bank of America,ACTIVE
...
```

---

## Preferences

### GET /api/preferences
**Description:** Get user preferences and settings

**Authentication Required:** Yes

**Response (200 OK):**
```json
{
  "userId": "user_id",
  "currency": "USD",
  "dateFormat": "YYYY-MM-DD",
  "theme": "LIGHT",
  "timezone": "America/New_York",
  "language": "en",
  "notifications": {
    "emailNotifications": true,
    "budgetAlerts": true,
    "recurringTransactionReminders": true,
    "investmentAlerts": true,
    "debtPaymentReminders": true,
    "weeklyDigest": true
  },
  "privacySettings": {
    "profileVisibility": "PRIVATE",
    "allowDataAnalytics": true,
    "dataRetention": "2_YEARS"
  },
  "displayPreferences": {
    "itemsPerPage": 20,
    "showDecimalPlaces": 2,
    "groupByCategory": true
  },
  "createdAt": "2024-01-01T10:30:00Z",
  "updatedAt": "2024-03-08T14:45:00Z"
}
```

**Error Responses:**
- 401 Unauthorized - Invalid or missing token

---

### PUT /api/preferences
**Description:** Update user preferences and settings

**Authentication Required:** Yes

**Request Body:**
```json
{
  "currency": "EUR",
  "theme": "DARK",
  "timezone": "Europe/London",
  "notifications": {
    "emailNotifications": false,
    "budgetAlerts": true,
    "recurringTransactionReminders": true
  },
  "displayPreferences": {
    "itemsPerPage": 25
  }
}
```

**Response (200 OK):**
```json
{
  "userId": "user_id",
  "currency": "EUR",
  "dateFormat": "YYYY-MM-DD",
  "theme": "DARK",
  "timezone": "Europe/London",
  "language": "en",
  "notifications": {
    "emailNotifications": false,
    "budgetAlerts": true,
    "recurringTransactionReminders": true,
    "investmentAlerts": true,
    "debtPaymentReminders": true,
    "weeklyDigest": true
  },
  "privacySettings": {
    "profileVisibility": "PRIVATE",
    "allowDataAnalytics": true,
    "dataRetention": "2_YEARS"
  },
  "displayPreferences": {
    "itemsPerPage": 25,
    "showDecimalPlaces": 2,
    "groupByCategory": true
  },
  "updatedAt": "2024-03-08T15:00:00Z"
}
```

**Error Responses:**
- 400 Bad Request - Invalid request body
- 401 Unauthorized - Invalid or missing token

---

## Admin

### GET /api/admin/users
**Description:** Get list of all users in the system

**Authentication Required:** Yes

**Admin Only:** Yes

**Query Parameters:**
- `page` (optional, integer, default: 1) - Pagination page number
- `limit` (optional, integer, default: 50) - Items per page
- `role` (optional, string: "USER", "ADMIN") - Filter by user role
- `sortBy` (optional, string: "createdAt", "email") - Sort field
- `sortOrder` (optional, string: "asc", "desc") - Sort order

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "user_id_1",
      "email": "user1@example.com",
      "name": "User One",
      "phoneNumber": "+1234567890",
      "role": "USER",
      "createdAt": "2024-01-01T10:30:00Z",
      "lastActive": "2024-03-08T14:45:00Z"
    },
    {
      "id": "user_id_2",
      "email": "admin@example.com",
      "name": "Admin User",
      "phoneNumber": "+1987654321",
      "role": "ADMIN",
      "createdAt": "2023-12-01T10:30:00Z",
      "lastActive": "2024-03-08T15:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "total": 150,
    "pages": 3
  }
}
```

**Error Responses:**
- 403 Forbidden - Insufficient permissions (user is not admin)
- 401 Unauthorized - Invalid or missing token

---

### GET /api/admin/logins
**Description:** Get login event history for all users

**Authentication Required:** Yes

**Admin Only:** Yes

**Query Parameters:**
- `page` (optional, integer, default: 1) - Pagination page number
- `limit` (optional, integer, default: 50) - Items per page
- `userId` (optional, string) - Filter by specific user
- `startDate` (optional, ISO 8601 format) - Filter from this date
- `endDate` (optional, ISO 8601 format) - Filter until this date
- `sortBy` (optional, string: "date", "userId") - Sort field
- `sortOrder` (optional, string: "asc", "desc") - Sort order

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "login_event_id_1",
      "userId": "user_id_1",
      "userEmail": "user1@example.com",
      "userName": "User One",
      "loginMethod": "GOOGLE_OAUTH",
      "ipAddress": "192.168.1.1",
      "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
      "timestamp": "2024-03-08T14:45:00Z",
      "status": "SUCCESS"
    },
    {
      "id": "login_event_id_2",
      "userId": "user_id_2",
      "userEmail": "admin@example.com",
      "userName": "Admin User",
      "loginMethod": "PHONE_AUTH",
      "ipAddress": "192.168.1.2",
      "userAgent": "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)...",
      "timestamp": "2024-03-08T15:00:00Z",
      "status": "SUCCESS"
    },
    {
      "id": "login_event_id_3",
      "userId": "user_id_3",
      "userEmail": "user3@example.com",
      "userName": "User Three",
      "loginMethod": "GOOGLE_OAUTH",
      "ipAddress": "192.168.1.3",
      "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)...",
      "timestamp": "2024-03-08T13:20:00Z",
      "status": "FAILED"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "total": 1250,
    "pages": 25
  }
}
```

**Error Responses:**
- 403 Forbidden - Insufficient permissions (user is not admin)
- 401 Unauthorized - Invalid or missing token

---

## Error Handling

The API returns standard HTTP status codes and error responses in JSON format.

### Common Error Responses

**400 Bad Request**
```json
{
  "error": "Invalid request",
  "message": "The request body is missing required fields: amount, category",
  "timestamp": "2024-03-08T14:45:00Z"
}
```

**401 Unauthorized**
```json
{
  "error": "Unauthorized",
  "message": "Invalid or missing Authorization header",
  "timestamp": "2024-03-08T14:45:00Z"
}
```

**403 Forbidden**
```json
{
  "error": "Forbidden",
  "message": "Admin access required",
  "timestamp": "2024-03-08T14:45:00Z"
}
```

**404 Not Found**
```json
{
  "error": "Not Found",
  "message": "Expense with ID 'expense_id_999' not found",
  "timestamp": "2024-03-08T14:45:00Z"
}
```

**500 Internal Server Error**
```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred while processing your request",
  "timestamp": "2024-03-08T14:45:00Z"
}
```

---

## Rate Limiting

The API implements rate limiting to ensure fair usage and prevent abuse. Users are limited to:
- **1000 requests per hour** for read operations
- **500 requests per hour** for write operations

Rate limit information is returned in response headers:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1709908200
```

---

## Pagination

Endpoints that return lists support pagination with the following query parameters:
- `page` (integer, default: 1) - The page number to retrieve
- `limit` (integer, default: 20) - The number of items per page (max: 100)

Paginated responses include metadata:
```json
{
  "data": [...],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 150,
    "pages": 8
  }
}
```

---

## Sorting

Endpoints that support sorting accept two parameters:
- `sortBy` (string) - The field to sort by
- `sortOrder` (string: "asc" or "desc") - The sort direction (default: "asc")

Example: `GET /api/expenses?sortBy=amount&sortOrder=desc`

---

## Date Format

All dates and timestamps in the API use ISO 8601 format (RFC 3339):
```
2024-03-08T14:45:00Z
```

When providing dates as query parameters, use the same ISO 8601 format.

---

## API Versioning

The current API version is 1.0.0. The API uses URL-based versioning and future versions will be accessible under `/api/v2`, `/api/v3`, etc.

---

## Support & Contact

For API support, documentation updates, or to report issues, please contact the development team or visit the project repository.
