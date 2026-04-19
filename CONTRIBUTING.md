# Contributing to Expense Tracker

Thank you for your interest in contributing! This guide will help you get started.

## Development Setup

### Prerequisites
- Java 17 JDK
- Maven 3.9+
- Node.js 18+
- npm 9+

### Quick Start

```bash
# Clone the repository
git clone https://github.com/your-org/expense-tracker.git
cd expense-tracker

# Backend setup
cd backend
cp .env.example .env    # Edit with your config
mvn clean package
java -jar target/backend-1.0.0.jar

# Frontend setup (in another terminal)
cd frontend
cp .env.example .env    # Edit with your config
npm install
npm run dev
```

The app will be available at http://localhost:5173 (frontend dev server) with API at http://localhost:7001.

### Using Docker

```bash
cp .env.example .env    # Edit root .env for Docker passwords
docker-compose up --build
```

App available at http://localhost:7000.

## Project Structure

```
expense-tracker/
  backend/          # Java/Javalin REST API
    src/main/java/  # Application source
    src/test/java/  # JUnit 5 tests
    pom.xml         # Maven config
  frontend/         # React/Vite SPA
    src/pages/      # Page components
    src/components/ # Shared components
    src/api/        # API client
  db/               # Database init scripts
  .github/          # CI/CD workflows
```

## Making Changes

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Make your changes
3. Run backend tests: `cd backend && mvn test`
4. Build frontend: `cd frontend && npm run build`
5. Commit with clear messages
6. Push and open a Pull Request

## Code Style

- **Java**: Follow standard Java conventions, use meaningful variable names
- **React**: Functional components with hooks, named exports
- **CSS**: Use CSS variables for theming, mobile-first responsive design

## Running Tests

```bash
# Backend
cd backend
mvn test

# Specific test class
mvn test -Dtest=ValidationUtilTest
```
