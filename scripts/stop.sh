#!/usr/bin/env bash
# Arrête le serveur Jetty lancé par scripts/start.sh.
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR" || exit 1

PID_FILE="logs/.server.pid"

server_process_matches_project() {
  local process_id="$1"
  local process_args
  process_args="$(ps -p "$process_id" -o args= 2>/dev/null)" || return 1
  [[ "$process_args" == *"$PROJECT_DIR"* && "$process_args" == *"jetty:run"* ]]
}

if [[ ! -f "$PID_FILE" ]]; then
  echo "Aucun serveur lancé par start.sh n'est enregistré."
  exit 0
fi

SERVER_PID="$(cat "$PID_FILE")"
if [[ "$SERVER_PID" =~ ^[0-9]+$ ]] && kill -0 "$SERVER_PID" 2>/dev/null \
  && server_process_matches_project "$SERVER_PID"; then
  if kill -- "-$SERVER_PID"; then
    echo "Serveur arrêté."
  else
    echo "Impossible d'arrêter le serveur (PID $SERVER_PID)."
    exit 1
  fi
else
  echo "Le fichier PID est périmé ou ne correspond pas au Jetty de ce projet."
fi
rm -f "$PID_FILE"
