#!/usr/bin/env bash
set -euo pipefail

# ============================================
# Kanaku Book - Database Backup Script
# ============================================
# Manual:  bash deploy/backup.sh
# Cron:    0 2 * * * /opt/kanakubook/deploy/backup.sh >> /var/log/kanakubook-backup.log 2>&1
# ============================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

source .env

BACKUP_DIR="${PROJECT_DIR}/backups"
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/kanakubook_${TIMESTAMP}.sql.gz"

echo "[$(date)] Starting database backup..."

# Dump and compress
docker exec expense-tracker-db mysqldump \
    -u "${DB_USER}" \
    -p"${DB_PASSWORD}" \
    --single-transaction \
    --routines \
    --triggers \
    expensetracker 2>/dev/null | gzip > "$BACKUP_FILE"

FILESIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "[$(date)] Backup saved: ${BACKUP_FILE} (${FILESIZE})"

# Keep only last 30 days of backups
find "$BACKUP_DIR" -name "kanakubook_*.sql.gz" -mtime +30 -delete
REMAINING=$(ls -1 "$BACKUP_DIR"/kanakubook_*.sql.gz 2>/dev/null | wc -l)
echo "[$(date)] Backups retained: ${REMAINING} (30-day retention)"

echo "[$(date)] Backup complete."
