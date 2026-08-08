#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME_DIR="${SCRIPT_DIR}/runtime"
TOMCAT_VERSION="${TOMCAT_VERSION:-9.0.120}"
TOMCAT_BASE_URL="${TOMCAT_BASE_URL:-https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin}"
ADOPTIUM_API="${ADOPTIUM_API:-https://api.adoptium.net/v3}"
ARCH="x64"
WAR_FILE=""
OUTPUT_FILE=""

usage() {
    cat <<'USAGE'
Pemakaian:
  ./build-installer.sh --war /path/ais.war [--arch x64|aarch64] [--output file.run]

Environment opsional:
  TOMCAT_VERSION   Versi Tomcat 9 (default 9.0.120)
  TOMCAT_BASE_URL  Direktori URL artefak Tomcat
  ADOPTIUM_API     Base URL API Adoptium
  JRE_URL          URL JRE .tar.gz yang dipin manual
  JRE_SHA256       SHA-256 wajib bila JRE_URL digunakan

Builder memerlukan bash, curl, tar, gzip, awk, sed, find, sort, xargs, dan sha256sum/sha512sum.
USAGE
}

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || die "Perintah '$1' tidak tersedia."; }

while (($#)); do
    case "$1" in
        --war) [[ $# -ge 2 ]] || die "--war memerlukan nilai"; WAR_FILE="$2"; shift 2 ;;
        --arch) [[ $# -ge 2 ]] || die "--arch memerlukan nilai"; ARCH="$2"; shift 2 ;;
        --output) [[ $# -ge 2 ]] || die "--output memerlukan nilai"; OUTPUT_FILE="$2"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *) die "Argumen tidak dikenal: $1" ;;
    esac
done

[[ -n "$WAR_FILE" ]] || { usage; die "--war wajib diisi"; }
[[ -f "$WAR_FILE" && -r "$WAR_FILE" ]] || die "WAR tidak dapat dibaca: $WAR_FILE"
[[ "$ARCH" == "x64" || "$ARCH" == "aarch64" ]] || die "Arsitektur harus x64 atau aarch64"
[[ -d "$RUNTIME_DIR" ]] || die "Folder runtime tidak ditemukan: $RUNTIME_DIR"

for cmd in curl tar gzip awk sed find sort xargs sha256sum sha512sum mktemp; do need "$cmd"; done

if [[ -z "$OUTPUT_FILE" ]]; then
    OUTPUT_FILE="${SCRIPT_DIR}/dist/ais-kantin-linux-${ARCH}.run"
fi
mkdir -p "$(dirname -- "$OUTPUT_FILE")"

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/ais-kantin-builder.XXXXXX")"
trap 'rm -rf -- "$WORK_DIR"' EXIT
PAYLOAD_DIR="${WORK_DIR}/payload"
mkdir -p "${PAYLOAD_DIR}/app"

printf 'Menyiapkan Eclipse Temurin JRE 8 (%s)...\n' "$ARCH"
JRE_ARCH="$ARCH"
if [[ -n "${JRE_URL:-}" ]]; then
    [[ -n "${JRE_SHA256:-}" ]] || die "JRE_SHA256 wajib bila JRE_URL diisi"
    jre_url="$JRE_URL"
    jre_sha="$JRE_SHA256"
else
    metadata_url="${ADOPTIUM_API}/assets/latest/8/hotspot?architecture=${JRE_ARCH}&heap_size=normal&image_type=jre&jvm_impl=hotspot&os=linux&project=jdk&vendor=eclipse"
    curl -fsSL --retry 3 "$metadata_url" -o "${WORK_DIR}/adoptium.json"
    jre_url="$(grep -m1 -oE '"link"[[:space:]]*:[[:space:]]*"[^"]+\.tar\.gz"' "${WORK_DIR}/adoptium.json" | sed -E 's/^.*"([^"]+)"$/\1/')"
    jre_sha="$(grep -m1 -oE '"checksum"[[:space:]]*:[[:space:]]*"[0-9a-fA-F]{64}"' "${WORK_DIR}/adoptium.json" | sed -E 's/^.*"([0-9a-fA-F]{64})"$/\1/')"
    [[ -n "$jre_url" && -n "$jre_sha" ]] || die "Metadata JRE dari Adoptium tidak dapat dibaca"
fi
curl -fL --retry 3 "$jre_url" -o "${WORK_DIR}/jre.tar.gz"
printf '%s  %s\n' "$jre_sha" "${WORK_DIR}/jre.tar.gz" | sha256sum -c -
mkdir -p "${PAYLOAD_DIR}/jre"
tar -xzf "${WORK_DIR}/jre.tar.gz" -C "${PAYLOAD_DIR}/jre" --strip-components=1
[[ -f "${PAYLOAD_DIR}/jre/bin/java" ]] || die "bin/java tidak ditemukan pada paket JRE"
find "${PAYLOAD_DIR}/jre/bin" -type f -exec chmod 0755 {} +
find "${PAYLOAD_DIR}/jre/lib" -type f -name '*.so' -exec chmod 0755 {} +

printf 'Menyiapkan Apache Tomcat %s...\n' "$TOMCAT_VERSION"
tomcat_name="apache-tomcat-${TOMCAT_VERSION}.tar.gz"
tomcat_url="${TOMCAT_BASE_URL}/${tomcat_name}"
curl -fL --retry 3 "$tomcat_url" -o "${WORK_DIR}/${tomcat_name}"
curl -fsSL --retry 3 "${tomcat_url}.sha512" -o "${WORK_DIR}/${tomcat_name}.sha512"
tomcat_sha="$(awk '{print $1; exit}' "${WORK_DIR}/${tomcat_name}.sha512")"
[[ "$tomcat_sha" =~ ^[0-9a-fA-F]{128}$ ]] || die "Checksum Tomcat tidak valid"
printf '%s  %s\n' "$tomcat_sha" "${WORK_DIR}/${tomcat_name}" | sha512sum -c -
mkdir -p "${PAYLOAD_DIR}/tomcat"
tar -xzf "${WORK_DIR}/${tomcat_name}" -C "${PAYLOAD_DIR}/tomcat" --strip-components=1
chmod 0755 "${PAYLOAD_DIR}/tomcat/bin/"*.sh
rm -rf -- "${PAYLOAD_DIR}/tomcat/webapps"/*
mkdir -p "${PAYLOAD_DIR}/tomcat/webapps"

cp -- "$WAR_FILE" "${PAYLOAD_DIR}/app/kantin.war"
cp -- "${RUNTIME_DIR}/install.sh" "${PAYLOAD_DIR}/install.sh"
cp -- "${RUNTIME_DIR}/configure.sh" "${PAYLOAD_DIR}/configure.sh"
chmod 0755 "${PAYLOAD_DIR}/install.sh" "${PAYLOAD_DIR}/configure.sh"

(
    cd "$PAYLOAD_DIR"
    find . -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS
)

tar -czf "${WORK_DIR}/payload.tar.gz" -C "$PAYLOAD_DIR" .
payload_sha="$(sha256sum "${WORK_DIR}/payload.tar.gz" | awk '{print $1}')"
sed "s/__PAYLOAD_SHA256__/${payload_sha}/g" "${RUNTIME_DIR}/launcher.sh" > "$OUTPUT_FILE"
cat "${WORK_DIR}/payload.tar.gz" >> "$OUTPUT_FILE"
chmod 0755 "$OUTPUT_FILE"

printf '\nInstaller berhasil dibuat:\n  %s\n' "$OUTPUT_FILE"
printf 'SHA-256 installer:\n  %s\n' "$(sha256sum "$OUTPUT_FILE" | awk '{print $1}')"
