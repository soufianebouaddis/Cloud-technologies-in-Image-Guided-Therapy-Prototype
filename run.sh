#!/usr/bin/env bash
#
# End-to-end launcher for the Cloud Technologies for Image-Guided Therapy platform.
#
#   ./run.sh              build everything, start infra + services + frontend
#   ./run.sh --no-build   skip the Maven build (reuse existing jars)
#
# Ctrl-C stops the frontend and all backend services started by this script.
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

LOG_DIR="$ROOT/.run-logs"
PID_FILE="$LOG_DIR/pids"
mkdir -p "$LOG_DIR"
: > "$PID_FILE"

# Infra compose file (Kafka + MongoDB). Override with COMPOSE_FILE=... ./run.sh
COMPOSE_FILE="${COMPOSE_FILE:-/Users/snof/Desktop/docker/docker-compose.yaml}"

NO_BUILD=false
[[ "${1:-}" == "--no-build" ]] && NO_BUILD=true

c_info() { printf "\033[1;36m[run]\033[0m %s\n" "$*"; }
c_warn() { printf "\033[1;33m[run]\033[0m %s\n" "$*"; }
c_err()  { printf "\033[1;31m[run]\033[0m %s\n" "$*"; }

cleanup() {
  echo
  c_info "shutting down backend services..."
  if [[ -f "$PID_FILE" ]]; then
    while read -r pid; do [[ -n "$pid" ]] && kill "$pid" 2>/dev/null || true; done < "$PID_FILE"
  fi
  c_info "backend stopped (Kafka/MongoDB containers left running)."
}
trap cleanup EXIT INT TERM

wait_for_port() { # port name timeout_seconds
  local port="$1" name="$2" timeout="${3:-90}"
  c_info "waiting for $name on :$port ..."
  for ((i = 0; i < timeout; i++)); do
    if nc -z localhost "$port" 2>/dev/null; then c_info "$name is up"; return 0; fi
    sleep 1
  done
  c_err "timed out waiting for $name on :$port"
  return 1
}

run_bg() { # name command...
  local name="$1"; shift
  "$@" > "$LOG_DIR/$name.log" 2>&1 &
  local pid=$!
  echo "$pid" >> "$PID_FILE"
  c_info "started $name (pid $pid) -> $LOG_DIR/$name.log"
}

# ---------------------------------------------------------------------------
# 1. Infrastructure: Kafka + MongoDB (Docker)
# ---------------------------------------------------------------------------
if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -qx kafka; then
  c_info "starting Kafka + MongoDB via docker-compose ($COMPOSE_FILE)"
  docker-compose -f "$COMPOSE_FILE" up -d
fi
wait_for_port 9092 kafka 120
wait_for_port 27017 mongodb 60

# Ollama (native) sanity check
if curl -s localhost:11434/api/tags >/dev/null 2>&1; then
  c_info "Ollama reachable on :11434"
else
  c_warn "Ollama not reachable on :11434 — start it and pull the medgemma model, or ai-service will fail."
fi

# ---------------------------------------------------------------------------
# 2. Build
# ---------------------------------------------------------------------------
if ! $NO_BUILD; then
  c_info "building Java services (this can take a minute)..."
  for svc in discovery-service imaging-service ai-service metadata-service simulator; do
    c_info "  mvn package $svc"
    ( cd "$svc" && ../mvnw -q clean package -DskipTests )
  done
fi

if [[ ! -x cpp-service/build/medical_kernel ]]; then
  c_info "building cpp-service (gRPC denoise kernel)..."
  cmake -S cpp-service -B cpp-service/build -DCMAKE_BUILD_TYPE=Release
  cmake --build cpp-service/build
fi

# ---------------------------------------------------------------------------
# 3. Backend services (order matters)
#    config-service + apigateway are intentionally skipped: config-service needs
#    GIT_USERNAME/GIT_TOKEN and every service imports config as `optional`.
# ---------------------------------------------------------------------------
run_bg discovery-service java -jar discovery-service/target/discovery-service-0.0.1-SNAPSHOT.jar
wait_for_port 8761 eureka 90

run_bg cpp-service ./cpp-service/build/medical_kernel
run_bg imaging-service java -jar imaging-service/target/imaging-service-0.0.1-SNAPSHOT.jar
run_bg ai-service java -jar ai-service/target/ai-service.jar
run_bg metadata-service java -jar metadata-service/target/metadata-service.jar
wait_for_port 8082 imaging-service 90

run_bg simulator java -jar simulator/target/simulator-0.0.1-SNAPSHOT.jar
wait_for_port 8085 simulator 90

# ---------------------------------------------------------------------------
# 4. Frontend (foreground — Ctrl-C here tears everything down)
# ---------------------------------------------------------------------------
c_info "backend is up. Starting the React front-end gateway..."
cd frontend
if [[ ! -d node_modules ]]; then
  c_info "installing frontend dependencies (first run)..."
  npm install
fi
c_info "open http://localhost:5173 and upload a .dcm from /Users/snof/Desktop/Medic-dcm"
npm run dev
