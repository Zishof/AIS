#!/usr/bin/env sh
set -eu

# Backup konsisten Repository: database PostgreSQL + storage + checksum manifest.
# Wajib dijalankan operator server dengan direktori eksplisit; script tidak membaca rahasia source.
: "${REPOSITORY_BACKUP_DIR:?Tetapkan REPOSITORY_BACKUP_DIR}"
: "${AIS_REPOSITORY_STORAGE:?Tetapkan AIS_REPOSITORY_STORAGE}"
: "${DATABASE_URL:?Tetapkan DATABASE_URL PostgreSQL untuk pg_dump}"

umask 077
backup_root=$(cd -- "$REPOSITORY_BACKUP_DIR" 2>/dev/null && pwd -P) || {
  echo "Direktori backup tidak tersedia: $REPOSITORY_BACKUP_DIR" >&2
  exit 2
}
storage_root=$(cd -- "$AIS_REPOSITORY_STORAGE" 2>/dev/null && pwd -P) || {
  echo "Storage Repository tidak tersedia: $AIS_REPOSITORY_STORAGE" >&2
  exit 2
}
[ "$backup_root" != "/" ] && [ "$storage_root" != "/" ] || {
  echo "Root filesystem tidak boleh menjadi target backup/storage." >&2
  exit 2
}
case "$backup_root/" in "$storage_root/"*) echo "Direktori backup tidak boleh berada di dalam storage Repository." >&2; exit 2;; esac

stamp=$(date -u +%Y%m%dT%H%M%SZ)
work=$(mktemp -d "$backup_root/.repository-backup.XXXXXX")
cleanup(){ [ -n "${work:-}" ] && [ -d "$work" ] && rm -rf -- "$work"; }
trap cleanup EXIT HUP INT TERM

pg_dump --format=custom --no-owner --no-privileges --file="$work/repository-db.dump" "$DATABASE_URL"
tar -C "$storage_root" -czf "$work/repository-storage.tar.gz" .
{
  echo "created_utc=$stamp"
  echo "storage_root=$storage_root"
  echo "database_format=postgresql-custom"
  echo "application=Repository AIS"
} > "$work/manifest.txt"
(cd "$work" && sha256sum repository-db.dump repository-storage.tar.gz manifest.txt > SHA256SUMS)

final="$backup_root/repository-$stamp"
[ ! -e "$final" ] || { echo "Target backup sudah ada: $final" >&2; exit 3; }
mv -- "$work" "$final"
work=""
trap - EXIT HUP INT TERM
echo "Backup Repository selesai: $final"
echo "Lanjutkan dengan repository-restore-verify.sh sebelum menyalin backup ke media sekunder."
