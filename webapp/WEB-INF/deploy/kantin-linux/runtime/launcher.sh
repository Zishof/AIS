#!/usr/bin/env bash
set -Eeuo pipefail

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
command -v sha256sum >/dev/null 2>&1 || die "sha256sum tidak tersedia"
command -v tar >/dev/null 2>&1 || die "tar tidak tersedia"

SELF="$(readlink -f -- "$0" 2>/dev/null || printf '%s' "$0")"
ARCHIVE_LINE="$(awk '/^__AIS_KANTIN_ARCHIVE_BELOW__$/ { print NR + 1; exit }' "$SELF")"
[[ -n "$ARCHIVE_LINE" ]] || die "Penanda payload installer tidak ditemukan"

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/ais-kantin-installer.XXXXXX")"
trap 'rm -rf -- "$TMP_DIR"' EXIT
tail -n +"$ARCHIVE_LINE" "$SELF" > "${TMP_DIR}/payload.tar.gz"
actual_sha="$(sha256sum "${TMP_DIR}/payload.tar.gz" | awk '{print $1}')"
expected_sha="__PAYLOAD_SHA256__"
[[ "$actual_sha" == "$expected_sha" ]] || die "Checksum payload tidak cocok; installer rusak atau berubah"

mkdir -p "${TMP_DIR}/payload"
tar -xzf "${TMP_DIR}/payload.tar.gz" -C "${TMP_DIR}/payload"
(
    cd "${TMP_DIR}/payload"
    sha256sum -c --quiet SHA256SUMS
)
printf 'Payload installer terverifikasi.\n'
"${TMP_DIR}/payload/install.sh" "${TMP_DIR}/payload"
exit 0
__AIS_KANTIN_ARCHIVE_BELOW__
