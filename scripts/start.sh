#!/usr/bin/env bash
# Démarre le serveur Jetty en arrière-plan.
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR" || exit 1
CONFIGURED_PORT="$(sed -n 's/^-DJETTY_PORT=//p' "$PROJECT_DIR/.mvn/maven.config")"
JETTY_PORT="${JETTY_PORT:-$CONFIGURED_PORT}"
MIN_JETTY_PORT=1
MAX_JETTY_PORT=65535

if [[ ! "$JETTY_PORT" =~ ^[0-9]+$ ]] \
  || (( JETTY_PORT < MIN_JETTY_PORT || JETTY_PORT > MAX_JETTY_PORT )); then
  echo "Port Jetty invalide : $JETTY_PORT"
  exit 1
fi

PID_FILE="logs/.server.pid"
LOG_FILE="logs/server.log"
STARTUP_ATTEMPTS=60
STARTUP_CHECK_TIMEOUT_SECONDS=2
STARTUP_RETRY_DELAY_SECONDS=1
JETTY_QUIET_LOGS="${JETTY_QUIET_LOGS:-1}"
BAN_API_LOG_JSON_PRETTY="${BAN_API_LOG_JSON_PRETTY:-true}"
export BAN_API_LOG_JSON_PRETTY
MAVEN_QUIET_ARGS=()

if [[ "$JETTY_QUIET_LOGS" == "1" ]]; then
  MAVEN_QUIET_ARGS=(-q)
fi

server_process_matches_project() {
  local process_id="$1"
  local process_args
  process_args="$(ps -p "$process_id" -o args= 2>/dev/null)" || return 1
  [[ "$process_args" == *"$PROJECT_DIR"* && "$process_args" == *"jetty:run"* ]]
}

if [[ -f "$PID_FILE" ]]; then
  EXISTING_PID="$(cat "$PID_FILE")"
  if [[ "$EXISTING_PID" =~ ^[0-9]+$ ]] && kill -0 "$EXISTING_PID" 2>/dev/null \
    && server_process_matches_project "$EXISTING_PID"; then
    echo "Le serveur tourne déjà (PID $EXISTING_PID)."
    exit 0
  fi
  rm -f "$PID_FILE"
fi

if (exec 3<>"/dev/tcp/127.0.0.1/${JETTY_PORT}") 2>/dev/null; then
  echo "Impossible de démarrer Jetty : le port ${JETTY_PORT} est déjà utilisé."
  exit 1
fi

mkdir -p "$PROJECT_DIR/logs"
# setsid : le PID devient chef de groupe, ce qui permet de tout arrêter proprement
setsid nohup mvn "${MAVEN_QUIET_ARGS[@]}" "-DJETTY_PORT=${JETTY_PORT}" jetty:run > "$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"

echo "Démarrage en cours (PID $!)... logs : $LOG_FILE"
for _ in $(seq 1 "$STARTUP_ATTEMPTS"); do
  if ! kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "Échec du démarrage, voir $LOG_FILE"
    rm -f "$PID_FILE"
    exit 1
  fi
  if curl -fs -o /dev/null --max-time "$STARTUP_CHECK_TIMEOUT_SECONDS" \
    "http://localhost:${JETTY_PORT}/adresses/"; then
    echo "Serveur prêt : http://localhost:${JETTY_PORT}/adresses/"
    exit 0
  fi
  sleep "$STARTUP_RETRY_DELAY_SECONDS"
done
echo "Le serveur n'a pas répondu sur le port ${JETTY_PORT}; arrêt du démarrage. Logs : $LOG_FILE"
kill -- "-$(cat "$PID_FILE")" 2>/dev/null || true
rm -f "$PID_FILE"
exit 1
