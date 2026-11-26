#!/usr/bin/env bash
set -euo pipefail

# ─── Colours ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; BOLD='\033[1m'; NC='\033[0m'

info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ─── Config (override with environment variables) ─────────────────────────────
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-routesync_db}"
DB_USERNAME="${DB_USERNAME:-root}"
DB_PASSWORD="${DB_PASSWORD:-root}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"
LOAD_SAMPLE="${LOAD_SAMPLE:-}"          # Set to "yes" to auto-load sample data

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"
FRONTEND_DIR="$SCRIPT_DIR/frontend"
DATABASE_DIR="$SCRIPT_DIR/database"

BACKEND_LOG="$SCRIPT_DIR/backend.log"
FRONTEND_LOG="$SCRIPT_DIR/frontend.log"
PIDS_FILE="$SCRIPT_DIR/.app.pids"

# ─── Cleanup on exit / Ctrl-C ─────────────────────────────────────────────────
cleanup() {
  echo ""
  info "Shutting down…"
  if [[ -f "$PIDS_FILE" ]]; then
    while IFS= read -r pid; do
      if kill -0 "$pid" 2>/dev/null; then
        kill "$pid" 2>/dev/null && info "Stopped PID $pid"
      fi
    done < "$PIDS_FILE"
    rm -f "$PIDS_FILE"
  fi
  success "All processes stopped."
}
trap cleanup INT TERM EXIT

# ─── Prerequisite checks ──────────────────────────────────────────────────────
check_prereqs() {
  echo -e "\n${BOLD}Checking prerequisites…${NC}"

  command -v java  >/dev/null 2>&1 || error "Java not found. Install Java 17+."
  local jver; jver=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
  [[ "$jver" -ge 17 ]] 2>/dev/null || error "Java 17+ required (found $jver)."
  success "Java $jver"

  command -v mvn >/dev/null 2>&1 || error "Maven not found. Install Maven 3.8+."
  success "Maven $(mvn -q --version 2>&1 | head -1 | awk '{print $3}')"

  command -v node >/dev/null 2>&1 || error "Node.js not found. Install Node 18+."
  success "Node $(node --version)"

  command -v npm >/dev/null 2>&1 || error "npm not found."
  success "npm $(npm --version)"

  # MySQL connectivity check
  if command -v mysql >/dev/null 2>&1; then
    if mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" -h"$DB_HOST" -P"$DB_PORT" \
             -e "SELECT 1;" >/dev/null 2>&1; then
      success "MySQL reachable at $DB_HOST:$DB_PORT"
    else
      error "Cannot connect to MySQL at $DB_HOST:$DB_PORT as user '$DB_USERNAME'.\n       Check DB_USERNAME / DB_PASSWORD env vars or start MySQL."
    fi
  else
    warn "mysql client not found — skipping connection check."
  fi
}

# ─── Sample data loader ───────────────────────────────────────────────────────
prompt_sample_data() {
  if [[ "$LOAD_SAMPLE" == "yes" ]]; then
    load_sample_data
    return
  fi

  echo ""
  read -rp "$(echo -e "${YELLOW}Load sample data (stations/buses/routes/trips)?${NC} [y/N] ")" ans
  [[ "$ans" == "y" || "$ans" == "Y" ]] && load_sample_data || info "Skipping sample data."
}

load_sample_data() {
  if ! command -v mysql >/dev/null 2>&1; then
    warn "mysql client not found — cannot load sample data."
    return
  fi
  info "Loading sample data…"
  mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" -h"$DB_HOST" -P"$DB_PORT" \
        "$DB_NAME" < "$DATABASE_DIR/sample-data.sql" 2>/dev/null \
    && success "Sample data loaded." \
    || warn "Sample data may already be loaded (duplicate key ignored)."
}

# ─── Backend ──────────────────────────────────────────────────────────────────
start_backend() {
  echo -e "\n${BOLD}Starting backend…${NC}"
  info "Logs → $BACKEND_LOG"

  export DB_USERNAME DB_PASSWORD DB_HOST DB_PORT DB_NAME BACKEND_PORT

  (
    cd "$BACKEND_DIR"
    mvn spring-boot:run \
      -Dspring-boot.run.jvmArguments="-Xms256m -Xmx512m" \
      -Dserver.port="$BACKEND_PORT" \
      -Dspring.datasource.url="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
      -Dspring.datasource.username="$DB_USERNAME" \
      -Dspring.datasource.password="$DB_PASSWORD" \
      > "$BACKEND_LOG" 2>&1
  ) &
  local pid=$!
  echo "$pid" >> "$PIDS_FILE"

  info "Waiting for backend to come up on port ${BACKEND_PORT}..."
  local attempts=0
  until curl -sf "http://localhost:${BACKEND_PORT}/api/auth/me" >/dev/null 2>&1 || \
        grep -q "Started KtRouteApplication" "$BACKEND_LOG" 2>/dev/null; do
    sleep 2
    (( attempts++ ))
    if (( attempts > 60 )); then
      echo ""
      error "Backend did not start within 120 s.\n       Check $BACKEND_LOG for details."
    fi
    printf "."
  done
  echo ""
  success "Backend running at http://localhost:${BACKEND_PORT}"
}

# ─── Frontend ─────────────────────────────────────────────────────────────────
start_frontend() {
  echo -e "\n${BOLD}Starting frontend…${NC}"

  if [[ ! -d "$FRONTEND_DIR/node_modules" ]]; then
    info "node_modules not found — running npm install…"
    (cd "$FRONTEND_DIR" && npm install --silent)
    success "npm install complete."
  fi

  info "Logs → $FRONTEND_LOG"
  (
    cd "$FRONTEND_DIR"
    npm run dev -- --port "$FRONTEND_PORT" --host 0.0.0.0 \
      > "$FRONTEND_LOG" 2>&1
  ) &
  local pid=$!
  echo "$pid" >> "$PIDS_FILE"

  # Wait for Vite to print its URL
  local attempts=0
  until grep -q "Local:" "$FRONTEND_LOG" 2>/dev/null; do
    sleep 1
    (( attempts++ ))
    (( attempts > 30 )) && break
    printf "."
  done
  echo ""
  success "Frontend running at http://localhost:${FRONTEND_PORT}"
}

# ─── Main ─────────────────────────────────────────────────────────────────────
main() {
  echo -e "${BOLD}${BLUE}"
  echo "╔══════════════════════════════════════════════╗"
  echo "║   RouteSync — Startup    ║"
  echo "╚══════════════════════════════════════════════╝"
  echo -e "${NC}"

  # Clean up any previous PID file
  rm -f "$PIDS_FILE"
  touch "$PIDS_FILE"

  check_prereqs
  start_backend
  prompt_sample_data
  start_frontend

  echo ""
  echo -e "${BOLD}${GREEN}═══════════════════════════════════════════${NC}"
  echo -e "${BOLD}  Application is ready!${NC}"
  echo -e "  Frontend  →  ${BLUE}http://localhost:${FRONTEND_PORT}${NC}"
  echo -e "  Backend   →  ${BLUE}http://localhost:${BACKEND_PORT}${NC}"
  echo ""
  echo -e "  Admin login:  ${YELLOW}admin@ktroute.com${NC} / ${YELLOW}Admin@1234${NC}"
  echo -e "${BOLD}${GREEN}═══════════════════════════════════════════${NC}"
  echo ""
  echo -e "  Press ${BOLD}Ctrl+C${NC} to stop all services."
  echo ""

  # Keep script alive; the trap handles cleanup on Ctrl-C
  wait
}

main "$@"
