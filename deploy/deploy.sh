#!/usr/bin/env bash
set -euo pipefail

# ============================================
# Kanaku Book - Deploy / Update Script
# ============================================
# Run from the project root:
#   bash deploy/deploy.sh
# ============================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

echo "============================================"
echo " Kanaku Book - Deploying"
echo "============================================"

# Check .env exists
if [ ! -f .env ]; then
    echo "ERROR: .env file not found!"
    echo "  Run: cp .env.production .env && nano .env"
    exit 1
fi

# Load domain from .env
source .env
DOMAIN="${DOMAIN:-}"

if [ -z "$DOMAIN" ] || [ "$DOMAIN" = "YOUR_DOMAIN_HERE" ]; then
    echo "ERROR: DOMAIN not configured in .env"
    echo "  Set DOMAIN=your-domain.com in .env"
    exit 1
fi

echo "[1/5] Pulling latest code..."
if git rev-parse --git-dir > /dev/null 2>&1; then
    git pull origin main 2>/dev/null || git pull origin master 2>/dev/null || echo "  Skipped git pull (not on main/master)"
fi

echo "[2/5] Building Docker images..."
docker compose build --no-cache

echo "[3/5] Starting services..."
docker compose up -d

echo "[4/5] Waiting for services to be healthy..."
sleep 5

# Check health
MAX_RETRIES=30
RETRY=0
until curl -sf http://localhost:7000/health > /dev/null 2>&1; do
    RETRY=$((RETRY + 1))
    if [ $RETRY -ge $MAX_RETRIES ]; then
        echo "  ERROR: App failed to start after ${MAX_RETRIES}s"
        echo "  Check logs: docker compose logs app"
        exit 1
    fi
    sleep 1
done
echo "  App is healthy!"

echo "[5/5] Configuring Caddy for HTTPS..."
cat > /etc/caddy/Caddyfile << EOF
${DOMAIN} {
    reverse_proxy localhost:7000
    encode gzip

    header {
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        Strict-Transport-Security "max-age=31536000; includeSubDomains"
        -Server
    }
}

www.${DOMAIN} {
    redir https://${DOMAIN}{uri} permanent
}
EOF

systemctl reload caddy 2>/dev/null || systemctl restart caddy
echo "  Caddy configured for https://${DOMAIN}"

echo ""
echo "============================================"
echo " Deployment complete!"
echo "============================================"
echo ""
echo " Your app is live at: https://${DOMAIN}"
echo ""
echo " Useful commands:"
echo "   docker compose ps              - Check service status"
echo "   docker compose logs -f app     - View app logs"
echo "   docker compose logs -f mysql   - View database logs"
echo "   docker compose restart app     - Restart app"
echo "   bash deploy/backup.sh          - Run manual backup"
echo ""
