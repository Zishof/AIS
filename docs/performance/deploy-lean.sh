#!/bin/bash
# =====================================================================
# Menyalin webapp AIS ke direktori deployment TANPA berkas yang tidak
# dibutuhkan runtime (Fase 8). Aman dijalankan berulang (idempoten).
#
# Pemakaian:
#   ./deploy-lean.sh <SUMBER_WEBAPP> <TUJUAN_DEPLOY>
# Contoh:
#   ./deploy-lean.sh /opt/ais/web /opt/tomcat/webapps/ecampus
#
# CATATAN: skrip ini hanya MENYALIN. Ia tidak menghapus apa pun di
# direktori sumber, sehingga tidak ada risiko kehilangan berkas.
# Jalankan lebih dulu dengan MODE_UJI=1 untuk melihat rencana salinnya.
# =====================================================================
set -euo pipefail

SUMBER="${1:-}"
TUJUAN="${2:-}"
DAFTAR_EXCLUDE="$(dirname "$0")/deploy-exclude.txt"

if [ -z "$SUMBER" ] || [ -z "$TUJUAN" ]; then
  echo "Pemakaian: $0 <SUMBER_WEBAPP> <TUJUAN_DEPLOY>" >&2
  exit 1
fi
if [ ! -d "$SUMBER" ]; then
  echo "ERROR: direktori sumber tidak ditemukan: $SUMBER" >&2
  exit 1
fi
if [ ! -f "$DAFTAR_EXCLUDE" ]; then
  echo "ERROR: daftar pengecualian tidak ditemukan: $DAFTAR_EXCLUDE" >&2
  exit 1
fi

OPSI_UJI=""
if [ "${MODE_UJI:-0}" = "1" ]; then
  OPSI_UJI="--dry-run"
  echo "== MODE UJI: tidak ada berkas yang benar-benar disalin =="
fi

echo "Sumber   : $SUMBER"
echo "Tujuan   : $TUJUAN"
echo "Exclude  : $DAFTAR_EXCLUDE"

UKURAN_SEBELUM=$(du -sm "$SUMBER" 2>/dev/null | cut -f1 || echo "?")

mkdir -p "$TUJUAN"
rsync -a --delete $OPSI_UJI \
  --exclude-from="$DAFTAR_EXCLUDE" \
  "$SUMBER"/ "$TUJUAN"/

if [ "${MODE_UJI:-0}" != "1" ]; then
  UKURAN_SESUDAH=$(du -sm "$TUJUAN" 2>/dev/null | cut -f1 || echo "?")
  echo "----------------------------------------"
  echo "Ukuran sumber   : ${UKURAN_SEBELUM} MB"
  echo "Ukuran deploy   : ${UKURAN_SESUDAH} MB"
  echo "----------------------------------------"
fi
echo "Selesai."
