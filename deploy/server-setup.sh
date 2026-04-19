#!/usr/bin/env bash
set -euo pipefail

# ============================================
# Kanaku Book - Server Initial Setup
# ============================================
# Run this ONCE on a fresh Ubuntu 22.04/24.04 server:
#   curl -sSL https://raw.githubusercontent.com/yavij/expense-tracker/main/deploy/server-setup.sh | bash
#   OR: scp this file to server and run: bash server-setup.sh
# ============================================

echo "============================================"
echo " Kanaku Book - Server Setup"
echo "============================================"

# Update system
echo "[1/6] Updating system packages..."
apt-get update -qq && apt-get upgrade -y -qq

# Install Docker
echo "[2/6] Installing Docker..."
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
    echo "  Docker installed: $(docker --version)"
else
    echo "  Docker already installed: $(docker --version)"
fi

# Install Docker Compose plugin
echo "[3/6] Verifying Docker Compose..."
if docker compose version &> /dev/null; then
    echo "  Docker Compose available: $(docker compose version)"
else
    apt-get install -y -qq docker-compose-plugin
    echo "  Docker Compose installed: $(docker compose version)"
fi

# Install Caddy (reverse proxy with auto-SSL)
echo "[4/6] Installing Caddy..."
if ! command -v caddy &> /dev/null; then
    apt-get install -y -qq debian-keyring debian-archive-keyring apt-transport-https curl
    curl -1sLf 'https://dl.cloudflare.com/deb/caddy/gpg' | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg 2>/dev/null
    echo "deb [signed-by=/usr/share/keyrings/caddy-stable-archive-keyring.gpg] https://dl.cloudflare.com/caddy stable main" > /etc/apt/sources.list.d/caddy-stable.list
    apt-get update -qq && apt-get install -y -qq caddy
    echo "  Caddy installed: $(caddy version)"
else
    echo "  Caddy already installed: $(caddy version)"
fi

# Configure firewall
echo "[5/6] Configuring firewall..."
apt-get install -y -qq ufw
ufw --force reset > /dev/null 2>&1
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable
echo "  Firewall enabled: SSH, HTTP, HTTPS allowed"

# Create app directory
echo "[6/6] Creating app directory..."
mkdir -p /opt/kanakubook
mkdir -p /opt/kanakubook/backups

echo ""
echo "============================================"
echo " Server setup complete!"
echo "============================================"
echo ""
echo " Next steps:"
echo "   1. Clone your repo:  cd /opt/kanakubook && git clone https://github.com/yavij/expense-tracker.git ."
echo "   2. Configure:        cp .env.production .env && nano .env"
echo "   3. Deploy:           bash deploy/deploy.sh"
echo ""
