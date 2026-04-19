package expensetracker;

import expensetracker.dao.UserDao;
import expensetracker.handler.AdminHandler;
import expensetracker.handler.AnalyticsHandler;
import expensetracker.handler.AuthHandler;
import expensetracker.handler.BackupHandler;
import expensetracker.handler.BudgetHandler;
import expensetracker.handler.DebtHandler;
import expensetracker.handler.ExpenseHandler;
import expensetracker.handler.InvestmentHandler;
import expensetracker.handler.ExportHandler;
import expensetracker.handler.MarketRatesHandler;
import expensetracker.handler.PaymentHandler;
import expensetracker.handler.PreferenceHandler;
import expensetracker.handler.RecurringHandler;
import expensetracker.handler.SalaryHandler;
import expensetracker.model.User;
import expensetracker.util.Database;
import expensetracker.util.JwtUtil;
import expensetracker.util.RateLimiter;
import expensetracker.util.NotificationScheduler;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Database.init();
        log.info("Database initialization complete");

        String jwtSecret = System.getenv("JWT_SECRET");
        if (jwtSecret == null || jwtSecret.length() < 32) {
            log.warn("JWT_SECRET is not set or is shorter than 32 characters. Using insecure default. Set a strong JWT_SECRET for production!");
        }

        AuthHandler authHandler = new AuthHandler();
        ExpenseHandler expenseHandler = new ExpenseHandler();
        InvestmentHandler investmentHandler = new InvestmentHandler();
        SalaryHandler salaryHandler = new SalaryHandler();
        DebtHandler debtHandler = new DebtHandler();
        BudgetHandler budgetHandler = new BudgetHandler();
        RecurringHandler recurringHandler = new RecurringHandler();
        AnalyticsHandler analyticsHandler = new AnalyticsHandler();
        ExportHandler exportHandler = new ExportHandler();
        BackupHandler backupHandler = new BackupHandler();
        PreferenceHandler preferenceHandler = new PreferenceHandler();
        AdminHandler adminHandler = new AdminHandler();
        PaymentHandler paymentHandler = new PaymentHandler();
        MarketRatesHandler marketRatesHandler = new MarketRatesHandler();

        // Rate limiters
        RateLimiter authRateLimiter = new RateLimiter(10, 60000); // 10 requests per minute for auth
        RateLimiter apiRateLimiter = new RateLimiter(60, 60000);  // 60 requests per minute for general API

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
        });

        // Request logging middleware
        app.before("/api/*", ctx -> {
            ctx.attribute("startTime", System.currentTimeMillis());
        });

        app.after("/api/*", ctx -> {
            long startTime = ctx.attribute("startTime") != null ? (Long) ctx.attribute("startTime") : System.currentTimeMillis();
            long duration = System.currentTimeMillis() - startTime;
            log.info("{} {} - Status: {} - Duration: {}ms", ctx.method(), ctx.path(), ctx.status(), duration);
        });

        // Security headers middleware – add before all routes
        app.before("/api/*", ctx -> {
            ctx.header("X-Content-Type-Options", "nosniff");
            ctx.header("X-Frame-Options", "DENY");
            ctx.header("X-XSS-Protection", "1; mode=block");
            ctx.header("Content-Security-Policy", "default-src 'self'");
            ctx.header("Strict-Transport-Security", "max-age=31536000");
        });

        // Rate limiting middleware for auth routes (stricter: 10/minute)
        app.before("/api/auth/*", ctx -> {
            String clientIp = getClientIp(ctx);
            if (!authRateLimiter.isAllowed(clientIp)) {
                ctx.status(429).json(Map.of("error", "Too many requests. Please try again later."));
            }
        });

        // Rate limiting middleware for all other API routes (standard: 60/minute)
        app.before("/api/*", ctx -> {
            if (ctx.path().startsWith("/api/auth")) return; // skip, already rate limited above
            String clientIp = getClientIp(ctx);
            if (!apiRateLimiter.isAllowed(clientIp)) {
                ctx.status(429).json(Map.of("error", "Too many requests. Please try again later."));
            }
        });

        // Global exception handler for validation errors
        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        });

        // Auth middleware – throw UnauthorizedResponse to halt the request chain
        app.before("/api/*", ctx -> {
            if (ctx.path().equals("/api/auth/google") || ctx.path().equals("/api/auth/phone") || ctx.path().equals("/api/docs")) return;
            String auth = ctx.header("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                throw new UnauthorizedResponse("Missing or invalid Authorization");
            }
            String token = auth.substring(7);
            try {
                Claims claims = JwtUtil.parseToken(token);
                String userId = JwtUtil.getUserId(claims);
                User user = new UserDao().findById(userId).orElse(null);
                if (user == null) {
                    throw new UnauthorizedResponse("User not found");
                }
                ctx.attribute("user", user);
            } catch (UnauthorizedResponse e) {
                throw e;
            } catch (Exception e) {
                throw new UnauthorizedResponse("Invalid token");
            }
        });

        // Admin middleware – check role for /api/admin/* routes
        app.before("/api/admin/*", ctx -> {
            User user = ctx.attribute("user");
            if (user == null || !user.getRole().equals("ADMIN")) {
                throw new io.javalin.http.ForbiddenResponse("Admin access required");
            }
        });

        app.post("/api/auth/google", authHandler.googleLogin);
        app.post("/api/auth/phone", authHandler.phoneLogin);
        app.get("/api/me", authHandler.me);
        app.put("/api/me", authHandler.updateProfile);

        app.get("/api/expenses/summary", expenseHandler::summary);
        app.get("/api/expenses/search", expenseHandler::search);
        app.get("/api/expenses", expenseHandler::list);
        app.get("/api/expenses/{id}", expenseHandler::getOne);
        app.post("/api/expenses", expenseHandler::create);
        app.put("/api/expenses/{id}", expenseHandler::update);
        app.delete("/api/expenses/{id}", expenseHandler::delete);

        // Investment routes (static before parameterized)
        app.get("/api/investments/portfolio", investmentHandler::portfolio);
        app.get("/api/investments", investmentHandler::list);
        app.get("/api/investments/{id}", investmentHandler::getOne);
        app.post("/api/investments", investmentHandler::create);
        app.put("/api/investments/{id}", investmentHandler::update);
        app.delete("/api/investments/{id}", investmentHandler::delete);

        // Salary routes
        app.get("/api/salary/history", salaryHandler::history);
        app.get("/api/salary", salaryHandler::list);
        app.get("/api/salary/{id}", salaryHandler::getOne);
        app.post("/api/salary", salaryHandler::create);
        app.put("/api/salary/{id}", salaryHandler::update);
        app.delete("/api/salary/{id}", salaryHandler::delete);

        // Debt routes (static before parameterized)
        app.get("/api/debts/schedule", debtHandler::payoffSchedule);
        app.get("/api/debts", debtHandler::list);
        app.get("/api/debts/{id}", debtHandler::getOne);
        app.post("/api/debts", debtHandler::create);
        app.put("/api/debts/{id}", debtHandler::update);
        app.delete("/api/debts/{id}", debtHandler::delete);
        app.get("/api/debts/{id}/payments", debtHandler::listPayments);
        app.post("/api/debts/{id}/payments", debtHandler::addPayment);

        // Budget routes (static before parameterized)
        app.get("/api/budgets/status", budgetHandler::status);
        app.get("/api/budgets", budgetHandler::list);
        app.post("/api/budgets", budgetHandler::upsert);
        app.delete("/api/budgets/{id}", budgetHandler::delete);

        // Recurring transaction routes (static before parameterized)
        app.post("/api/recurring/process", recurringHandler::processDue);
        app.get("/api/recurring", recurringHandler::list);
        app.get("/api/recurring/{id}", recurringHandler::getOne);
        app.post("/api/recurring", recurringHandler::create);
        app.put("/api/recurring/{id}", recurringHandler::update);
        app.delete("/api/recurring/{id}", recurringHandler::delete);

        // Analytics routes
        app.get("/api/analytics/monthly", analyticsHandler::monthly);
        app.get("/api/analytics/yearly", analyticsHandler::yearly);
        app.get("/api/analytics/networth", analyticsHandler::networth);

        // Export routes
        app.get("/api/export/expenses", exportHandler::exportExpenses);
        app.get("/api/export/investments", exportHandler::exportInvestments);
        app.get("/api/export/salary", exportHandler::exportSalary);
        app.get("/api/export/debts", exportHandler::exportDebts);

        // Backup/Restore routes
        app.get("/api/backup", backupHandler::backup);
        app.post("/api/backup/restore", backupHandler::restore);

        // Preferences routes
        app.get("/api/preferences", preferenceHandler::get);
        app.put("/api/preferences", preferenceHandler::update);

        // Payment routes
        app.post("/api/payment/create-order", paymentHandler::createOrder);
        app.post("/api/payment/verify", paymentHandler::verifyPayment);
        app.get("/api/payment/status", paymentHandler::status);

        // Market rates (gold & silver)
        app.get("/api/market-rates", marketRatesHandler::getRates);

        app.get("/api/admin/users", adminHandler::users);
        app.get("/api/admin/logins", adminHandler::logins);

        // Health check endpoint
        app.get("/health", ctx -> ctx.json(Map.of("status", "ok", "timestamp", System.currentTimeMillis())));

        // API documentation endpoint
        app.get("/api/docs", ctx -> {
            ctx.contentType("application/json");
            ctx.json(Map.of(
                "openapi", "3.0.3",
                "info", Map.of(
                    "title", "Kanaku Book API",
                    "version", "1.0.0",
                    "description", "Expense Tracker REST API"
                ),
                "paths", Map.ofEntries(
                    Map.entry("/api/auth/google", Map.of("post", Map.of("summary", "Google OAuth login", "tags", List.of("Auth")))),
                    Map.entry("/api/auth/phone", Map.of("post", Map.of("summary", "Phone OTP login", "tags", List.of("Auth")))),
                    Map.entry("/api/me", Map.of(
                        "get", Map.of("summary", "Get current user", "tags", List.of("Auth")),
                        "put", Map.of("summary", "Update profile", "tags", List.of("Auth"))
                    )),
                    Map.entry("/api/expenses", Map.of(
                        "get", Map.of("summary", "List expenses (supports limit/offset pagination)", "tags", List.of("Expenses")),
                        "post", Map.of("summary", "Create expense", "tags", List.of("Expenses"))
                    )),
                    Map.entry("/api/expenses/{id}", Map.of(
                        "get", Map.of("summary", "Get expense by ID", "tags", List.of("Expenses")),
                        "put", Map.of("summary", "Update expense", "tags", List.of("Expenses")),
                        "delete", Map.of("summary", "Delete expense", "tags", List.of("Expenses"))
                    )),
                    Map.entry("/api/expenses/summary", Map.of("get", Map.of("summary", "Monthly expense summary", "tags", List.of("Expenses")))),
                    Map.entry("/api/expenses/search", Map.of("get", Map.of("summary", "Search expenses by text", "tags", List.of("Expenses")))),
                    Map.entry("/api/investments", Map.of(
                        "get", Map.of("summary", "List investments", "tags", List.of("Investments")),
                        "post", Map.of("summary", "Create investment", "tags", List.of("Investments"))
                    )),
                    Map.entry("/api/investments/portfolio", Map.of("get", Map.of("summary", "Portfolio summary", "tags", List.of("Investments")))),
                    Map.entry("/api/salary", Map.of(
                        "get", Map.of("summary", "List salary entries", "tags", List.of("Salary")),
                        "post", Map.of("summary", "Create salary entry", "tags", List.of("Salary"))
                    )),
                    Map.entry("/api/salary/history", Map.of("get", Map.of("summary", "Salary history with yearly totals", "tags", List.of("Salary")))),
                    Map.entry("/api/debts", Map.of(
                        "get", Map.of("summary", "List debts", "tags", List.of("Debts")),
                        "post", Map.of("summary", "Create debt", "tags", List.of("Debts"))
                    )),
                    Map.entry("/api/debts/schedule", Map.of("get", Map.of("summary", "Debt payoff schedule (snowball/avalanche)", "tags", List.of("Debts")))),
                    Map.entry("/api/budgets", Map.of(
                        "get", Map.of("summary", "List budgets", "tags", List.of("Budgets")),
                        "post", Map.of("summary", "Create/update budget", "tags", List.of("Budgets"))
                    )),
                    Map.entry("/api/budgets/status", Map.of("get", Map.of("summary", "Budget status with spend vs limit", "tags", List.of("Budgets")))),
                    Map.entry("/api/recurring", Map.of(
                        "get", Map.of("summary", "List recurring transactions", "tags", List.of("Recurring")),
                        "post", Map.of("summary", "Create recurring transaction", "tags", List.of("Recurring"))
                    )),
                    Map.entry("/api/recurring/process", Map.of("post", Map.of("summary", "Process due recurring transactions", "tags", List.of("Recurring")))),
                    Map.entry("/api/analytics/monthly", Map.of("get", Map.of("summary", "Monthly analytics breakdown", "tags", List.of("Analytics")))),
                    Map.entry("/api/analytics/yearly", Map.of("get", Map.of("summary", "Yearly analytics trends", "tags", List.of("Analytics")))),
                    Map.entry("/api/analytics/networth", Map.of("get", Map.of("summary", "Net worth calculation", "tags", List.of("Analytics")))),
                    Map.entry("/api/export/expenses", Map.of("get", Map.of("summary", "Export expenses as CSV", "tags", List.of("Export")))),
                    Map.entry("/api/export/investments", Map.of("get", Map.of("summary", "Export investments as CSV", "tags", List.of("Export")))),
                    Map.entry("/api/export/salary", Map.of("get", Map.of("summary", "Export salary as CSV", "tags", List.of("Export")))),
                    Map.entry("/api/export/debts", Map.of("get", Map.of("summary", "Export debts as CSV", "tags", List.of("Export")))),
                    Map.entry("/api/backup", Map.of("get", Map.of("summary", "Full JSON backup", "tags", List.of("Backup")))),
                    Map.entry("/api/backup/restore", Map.of("post", Map.of("summary", "Restore from JSON backup", "tags", List.of("Backup")))),
                    Map.entry("/api/preferences", Map.of(
                        "get", Map.of("summary", "Get user preferences", "tags", List.of("Preferences")),
                        "put", Map.of("summary", "Update user preferences", "tags", List.of("Preferences"))
                    )),
                    Map.entry("/api/payment/create-order", Map.of("post", Map.of("summary", "Create Razorpay payment order", "tags", List.of("Payment")))),
                    Map.entry("/api/payment/verify", Map.of("post", Map.of("summary", "Verify payment signature", "tags", List.of("Payment")))),
                    Map.entry("/api/payment/status", Map.of("get", Map.of("summary", "Get subscription status", "tags", List.of("Payment")))),
                    Map.entry("/api/market-rates", Map.of("get", Map.of("summary", "Live gold & silver market rates", "tags", List.of("Market Rates")))),
                    Map.entry("/api/admin/users", Map.of("get", Map.of("summary", "List all users (admin)", "tags", List.of("Admin")))),
                    Map.entry("/api/admin/logins", Map.of("get", Map.of("summary", "Login audit trail (admin)", "tags", List.of("Admin"))))
                )
            ));
        });

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7000"));
        log.info("Starting Expense Tracker API on port {}", port);
        app.start(port);
        log.info("Expense Tracker API running on http://localhost:{}", port);

        // Initialize and start notification scheduler
        NotificationScheduler notificationScheduler = new NotificationScheduler();
        notificationScheduler.start();

        // Add shutdown hook to gracefully stop notification scheduler
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received, stopping notification scheduler");
            notificationScheduler.stop();
        }));
    }

    /**
     * Extracts the client IP address from the request context.
     * Considers X-Forwarded-For header for proxied requests.
     */
    private static String getClientIp(Context ctx) {
        String xForwardedFor = ctx.header("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, get the first one
            return xForwardedFor.split(",")[0].trim();
        }
        return ctx.ip();
    }
}
