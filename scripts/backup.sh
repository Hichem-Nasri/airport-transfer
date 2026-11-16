#!/usr/bin/env bash
# Daily PostgreSQL dump, 14 days kept. Cron example: 0 3 * * * /opt/hms/scripts/backup.sh
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; . ./.env; set +a
mkdir -p backups
docker compose -f docker-compose.prod.yml --env-file .env exec -T postgres \
  pg_dump -U "${DB_USER:-airport}" "${DB_NAME:-airport_transfer}" | gzip > "backups/db-$(date +%F).sql.gz"
find backups -name 'db-*.sql.gz' -mtime +14 -delete
