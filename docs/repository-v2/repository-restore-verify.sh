#!/usr/bin/env sh
set -eu

# Verifikasi non-destruktif. Script ini tidak menjalankan pg_restore dan tidak menulis storage.
: "${1:?Pemakaian: repository-restore-verify.sh /path/repository-YYYYmmddTHHMMSSZ}"
backup=$(cd -- "$1" 2>/dev/null && pwd -P) || { echo "Backup tidak ditemukan: $1" >&2; exit 2; }
[ "$backup" != "/" ] || { echo "Root filesystem bukan direktori backup." >&2; exit 2; }
for required in repository-db.dump repository-storage.tar.gz manifest.txt SHA256SUMS; do
  [ -f "$backup/$required" ] || { echo "Berkas wajib tidak ada: $required" >&2; exit 3; }
done
(cd "$backup" && sha256sum -c SHA256SUMS)
pg_restore --list "$backup/repository-db.dump" >/dev/null
tar -tzf "$backup/repository-storage.tar.gz" >/dev/null
echo "Backup valid secara struktur dan checksum: $backup"
echo "Restore tetap harus dilakukan ke database kosong dan storage karantina sesuai BACKUP_RESTORE.md."
