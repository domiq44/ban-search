#!/usr/bin/env bash
# Ouvre l'application dans le navigateur par défaut.
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CONFIGURED_PORT="$(sed -n 's/^-DJETTY_PORT=//p' "$PROJECT_DIR/.mvn/maven.config")"
JETTY_PORT="${JETTY_PORT:-$CONFIGURED_PORT}"
URL="http://localhost:${JETTY_PORT}/adresses/"

if ! curl -s -o /dev/null "$URL"; then
  echo "Le serveur ne répond pas, lancez d'abord ./scripts/start.sh"
  exit 1
fi

xdg-open "$URL" >/dev/null 2>&1 &
echo "Ouverture de $URL"
