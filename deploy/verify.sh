#!/usr/bin/env bash
set -euo pipefail

# ============================================
# Kanaku Book - Post-Deploy Verification
# ============================================
# Run after deployment to verify everything works:
#   bash deploy/verify.sh
# ============================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

source .env
DOMAIN="${DOMAIN:-localhost}"
PASSED=0
FAILED=0

check() {
    local label="$1"
    local result="$2"
    if [ "$result" = "ok" ]; then
        echo "  [PASS] $label"
        PASSED=$((PASSED + 1))
    else
        echo "  [FAIL] $label - $result"
        FAILED=$((FAILED + 1))
    fi
}

echo "============================================"
echo " Kanaku Book - Post-Deploy Verification"
echo "============================================"
echo ""

# 1. Docker services
echo "Docker Services:"
APP_STATUS=$(docker inspect -f '{{.State.Status}}' expense-tracker-app 2>/dev/null || echo "missing")
DB_STATUS=$(docker inspect -f '{{.State.Status}}' expense-tracker-db 2>/dev/null || echo "missing")
check "App container running" "$([ "$APP_STATUS" = "running" ] && echo ok || echo "$APP_STATUS")"
check "DB container running" "$([ "$DB_STATUS" = "running" ] && echo ok || echo "$DB_STATUS")"

echo ""
echo "API Endpoints:"

# 2. Health check
HEALTH=$(curl -sf http://localhost:7000/health 2>/dev/null | grep -c '"ok"' || echo "0")
check "Health endpoint" "$([ "$HEALTH" = "1" ] && echo ok || echo "not responding")"

# 3. Frontend serves
FRONTEND=$(curl -sf http://localhost:7000/ 2>/dev/null | grep -c "Kanaku Book" || echo "0")
check "Frontend served" "$([ "$FRONTEND" -ge "1" ] && echo ok || echo "not serving HTML")"

# 4. API docs
DOCS=$(curl -sf http://localhost:7000/api/docs 2>/dev/null | grep -c "openapi" || echo "0")
check "API docs endpoint" "$([ "$DOCS" -ge "1" ] && echo ok || echo "not accessible")"

# 5. PWA manifest
MANIFEST=$(curl -sf http://localhost:7000/manifest.json 2>/dev/null | grep -c "Kanaku Book" || echo "0")
check "PWA manifest" "$([ "$MANIFEST" -ge "1" ] && echo ok || echo "not found")"

echo ""
echo "Security:"

# 6. Security headers
HEADERS=$(curl -sI http://localhost:7000/health 2>/dev/null)
check "X-Content-Type-Options" "$(echo "$HEADERS" | grep -qi "nosniff" && echo ok || echo "missing")"

# 7. JWT Secret strength
JWT_LEN=${#JWT_SECRET}
check "JWT_SECRET length (>=32)" "$([ "$JWT_LEN" -ge 32 ] && echo ok || echo "only ${JWT_LEN} chars")"

# 8. DEV_PHONE_OTP not set
check "DEV_PHONE_OTP not set" "$([ -z "${DEV_PHONE_OTP:-}" ] && echo ok || echo "REMOVE THIS IN PRODUCTION!")"

echo ""
echo "Database:"

# 9. MySQL connection
DB_PING=$(docker exec expense-tracker-db mysqladmin ping -u"${DB_USER}" -p"${DB_PASSWORD}" 2>/dev/null | grep -c "alive" || echo "0")
check "MySQL responding" "$([ "$DB_PING" = "1" ] && echo ok || echo "not responding")"

# 10. Tables exist
TABLE_COUNT=$(docker exec expense-tracker-db mysql -u"${DB_USER}" -p"${DB_PASSWORD}" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='expensetracker'" 2>/dev/null || echo "0")
check "Database tables created" "$([ "$TABLE_COUNT" -ge "10" ] && echo ok || echo "only ${TABLE_COUNT} tables")"

if [ "$DOMAIN" != "localhost" ] && [ "$DOMAIN" != "YOUR_DOMAIN_HERE" ]; then
    echo ""
    echo "HTTPS (${DOMAIN}):"
    HTTPS_STATUS=$(curl -sf -o /dev/null -w "%{http_code}" "https://${DOMAIN}/health" 2>/dev/null || echo "000")
    check "HTTPS accessible" "$([ "$HTTPS_STATUS" = "200" ] && echo ok || echo "HTTP ${HTTPS_STATUS}")"

    SSL_EXPIRY=$(echo | openssl s_client -servername "$DOMAIN" -connect "${DOMAIN}:443" 2>/dev/null | openssl x509 -noout -enddate 2>/dev/null | cut -d= -f2 || echo "")
    check "SSL certificate" "$([ -n "$SSL_EXPIRY" ] && echo "ok (expires: $SSL_EXPIRY)" || echo "cannot verify")"
fi

echo ""
echo "============================================"
echo " Results: ${PASSED} passed, ${FAILED} failed"
echo "============================================"

if [ "$FAILED" -gt 0 ]; then
    echo ""
    echo " Fix the failed checks before going live!"
    exit 1
else
    echo ""
    echo " All checks passed! Ready for launch."
fi
