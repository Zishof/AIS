#!/bin/bash
set -euo pipefail

AIS_DIR="/opt/ais"
SRC_WEB="$AIS_DIR/web"
SVN_PASSWORD="fauzi123"

MODE="${1:-all}"

# Lokasi utama yang akan discan.
# Bisa ditambah kalau Tomcat Bapak ada di lokasi lain.
SCAN_ROOTS=(
  "/opt"
  "/backup2"
  "/backup4"
  "/var/lib"
  "/usr/share"
)

echo "=================================================="
echo " UPDATE WEB RESOURCE ECAMPUS - AUTO DETECT TOMCAT"
echo "=================================================="
echo "Mode       : $MODE"
echo "AIS Dir    : $AIS_DIR"
echo "Source Web : $SRC_WEB"
echo "=================================================="

if [ ! -d "$AIS_DIR" ]; then
  echo "ERROR: Folder AIS tidak ditemukan: $AIS_DIR"
  exit 1
fi

if [ ! -d "$SRC_WEB" ]; then
  echo "ERROR: Folder web source tidak ditemukan: $SRC_WEB"
  exit 1
fi

if ! command -v rsync >/dev/null 2>&1; then
  echo "ERROR: rsync belum terinstall."
  echo "Install dulu dengan:"
  echo "  apt install rsync -y"
  exit 1
fi

cd "$AIS_DIR"

echo ""
echo "== BERSIHKAN BUILD DAN TEMPORARY =="
rm -rf build/*
rm -rf temporary/*

echo ""
echo "== SVN UPDATE =="
svn update --password "$SVN_PASSWORD"

echo ""
echo "== DETEKSI SEMUA WEBAPP TOMCAT YANG MEMILIKI WEB-INF =="

EXISTING_ROOTS=()
for ROOT in "${SCAN_ROOTS[@]}"; do
  if [ -d "$ROOT" ]; then
    EXISTING_ROOTS+=("$ROOT")
  fi
done

if [ ${#EXISTING_ROOTS[@]} -eq 0 ]; then
  echo "ERROR: Tidak ada scan root yang ditemukan."
  exit 1
fi

mapfile -t WEBINF_DIRS < <(
  find "${EXISTING_ROOTS[@]}" \
    -path "$AIS_DIR" -prune -o \
    -type d \
    -name "WEB-INF" \
    -path "*/webapps/*/WEB-INF" \
    -print 2>/dev/null | sort -u
)

if [ ${#WEBINF_DIRS[@]} -eq 0 ]; then
  echo "ERROR: Tidak ditemukan webapp Tomcat dengan folder WEB-INF."
  echo "Pastikan Tomcat berada di salah satu lokasi ini:"
  printf '  - %s\n' "${SCAN_ROOTS[@]}"
  exit 1
fi

echo "Ditemukan ${#WEBINF_DIRS[@]} WEB-INF:"
for WEBINF in "${WEBINF_DIRS[@]}"; do
  echo "  - $WEBINF"
done

update_zul_only() {
  local WEBINF="$1"

  echo ""
  echo "== UPDATE ZUL SAJA =="
  echo "Target: $WEBINF"

  rsync -av \
    --include='*/' \
    --include='*.zul' \
    --exclude='*' \
    "$SRC_WEB/WEB-INF/" "$WEBINF/"
}

update_baru() {
  local WEBINF="$1"

  echo ""
  echo "== UPDATE WEB-INF/baru =="
  echo "Target: $WEBINF/baru"

  if [ ! -d "$SRC_WEB/WEB-INF/baru" ]; then
    echo "SKIP: Source tidak ditemukan: $SRC_WEB/WEB-INF/baru"
    return
  fi

  mkdir -p "$WEBINF/baru"

  rsync -av --delete \
    "$SRC_WEB/WEB-INF/baru/" "$WEBINF/baru/"
}

update_css() {
  local WEBINF="$1"
  local WEBAPP_DIR
  WEBAPP_DIR="$(dirname "$WEBINF")"

  echo ""
  echo "== UPDATE CSS =="
  echo "Target: $WEBAPP_DIR/css"

  if [ ! -d "$SRC_WEB/css" ]; then
    echo "SKIP: Source tidak ditemukan: $SRC_WEB/css"
    return
  fi

  mkdir -p "$WEBAPP_DIR/css"

  rsync -av --delete \
    "$SRC_WEB/css/" "$WEBAPP_DIR/css/"
}

update_jsp_only() {
  local WEBINF="$1"

  echo ""
  echo "== UPDATE JSP SAJA DI WEB-INF =="
  echo "Target: $WEBINF"

  rsync -av \
    --include='*/' \
    --include='*.jsp' \
    --exclude='*' \
    "$SRC_WEB/WEB-INF/" "$WEBINF/"
}

for WEBINF in "${WEBINF_DIRS[@]}"; do
  case "$MODE" in
    scan)
      # Hanya menampilkan hasil deteksi.
      ;;

    zul)
      update_zul_only "$WEBINF"
      ;;

    jsp)
      update_jsp_only "$WEBINF"
      ;;

    baru)
      update_baru "$WEBINF"
      ;;

    css)
      update_css "$WEBINF"
      ;;

    all)
      update_baru "$WEBINF"
      update_css "$WEBINF"
      update_zul_only "$WEBINF"
      ;;

    *)
      echo ""
      echo "ERROR: Mode tidak dikenal: $MODE"
      echo ""
      echo "Gunakan salah satu:"
      echo "  $0 scan   # hanya cek target yang terdeteksi"
      echo "  $0 zul    # update file .zul saja"
      echo "  $0 jsp    # update file .jsp saja di WEB-INF"
      echo "  $0 baru   # update WEB-INF/baru saja"
      echo "  $0 css    # update css saja"
      echo "  $0 all    # update baru + css + zul"
      exit 1
      ;;
  esac
done

echo ""
echo "=================================================="
echo " UPDATE SELESAI"
echo "=================================================="

echo ""
echo "== DETEKSI LOG CATALINA.OUT =="

mapfile -t LOG_FILES < <(
  for WEBINF in "${WEBINF_DIRS[@]}"; do
    TOMCAT_DIR="${WEBINF%%/webapps/*}"
    LOG_FILE="$TOMCAT_DIR/logs/catalina.out"
    if [ -f "$LOG_FILE" ]; then
      echo "$LOG_FILE"
    fi
  done | sort -u
)

if [ ${#LOG_FILES[@]} -gt 0 ]; then
  echo "Log ditemukan:"
  for LOG in "${LOG_FILES[@]}"; do
    echo "  - $LOG"
  done

  echo ""
  echo "Menampilkan log pertama:"
  echo "${LOG_FILES[0]}"
  tail -3f "${LOG_FILES[0]}"
else
  echo "Tidak ditemukan catalina.out dari target yang terdeteksi."
fi