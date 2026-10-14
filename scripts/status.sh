#!/usr/bin/env bash
# Indique si le serveur répond (code retour 0 = actif, 1 = arrêté).
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CONFIGURED_PORT="$(sed -n 's/^-DJETTY_PORT=//p' "$PROJECT_DIR/.mvn/maven.config")"
JETTY_PORT="${JETTY_PORT:-$CONFIGURED_PORT}"
STATUS_CHECK_TIMEOUT_SECONDS=3
HTTP_OK=200
URL="http://localhost:${JETTY_PORT}/adresses/"

CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$STATUS_CHECK_TIMEOUT_SECONDS" "$URL")

if [[ "$CODE" == "$HTTP_OK" ]]; then
  echo "Le serveur tourne : $URL"
  exit 0
fi

echo "Le serveur est arrêté (ou ne répond pas correctement, code HTTP $CODE)."
exit 1
