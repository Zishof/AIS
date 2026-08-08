#!/usr/bin/env bash
set -Eeuo pipefail
umask 027

SERVICE_NAME="ais-kantin"
SERVICE_USER="${AIS_SERVICE_USER:-ais-kantin}"
INSTALL_ROOT="${AIS_INSTALL_ROOT:-/opt/ais-kantin}"
CONFIG_DIR="/opt/.g/.h"
CONFIG_FILE="${CONFIG_DIR}/kantin.txt"

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
[[ "${EUID}" -eq 0 ]] || die "Jalankan sebagai root (sudo)."
[[ -t 0 ]] || die "Konfigurasi interaktif memerlukan terminal."
[[ -L "${INSTALL_ROOT}/current" ]] || die "Instalasi AIS e-Kantin tidak ditemukan."

prompt_value() {
    local label="$1" default_value="$2" value
    read -r -p "${label} [${default_value}]: " value
    printf '%s' "${value:-$default_value}"
}
prompt_required() {
    local label="$1" value=""
    while [[ -z "$value" ]]; do read -r -p "${label}: " value; done
    printf '%s' "$value"
}
prompt_password() {
    local label="$1" value=""
    while [[ -z "$value" ]]; do read -r -s -p "${label}: " value; printf '\n' >&2; done
    printf '%s' "$value"
}
validate_host() { [[ "$1" =~ ^[A-Za-z0-9._:\[\]-]+$ ]] || die "Host database tidak valid."; }
validate_port() { [[ "$1" =~ ^[0-9]+$ ]] && (($1 >= 1 && $1 <= 65535)) || die "Port database tidak valid."; }
escape_property() {
    local value="$1"
    value="${value//\\/\\\\}"; value="${value//:/\\:}"; value="${value//=/\\=}"
    value="${value// /\\ }"
    value="${value//#/\\#}"; value="${value//!/\\!}"
    printf '%s' "$value"
}

printf 'Konfigurasi ulang database AIS e-Kantin\n'
MAIN_HOST="$(prompt_value 'Host database utama' '127.0.0.1')"
MAIN_PORT="$(prompt_value 'Port database utama' '5432')"
MAIN_USER="$(prompt_required 'Username database utama')"
MAIN_PASSWORD="$(prompt_password 'Password database utama')"
STREAM_HOST="$(prompt_value 'Host database streaming' "$MAIN_HOST")"
STREAM_PORT="$(prompt_value 'Port database streaming' '5432')"
STREAM_USER="$(prompt_value 'Username database streaming' "$MAIN_USER")"
STREAM_PASSWORD="$(prompt_password 'Password database streaming')"
validate_host "$MAIN_HOST"; validate_host "$STREAM_HOST"
validate_port "$MAIN_PORT"; validate_port "$STREAM_PORT"

mkdir -p "$CONFIG_DIR"
config_tmp="$(mktemp "${CONFIG_DIR}/kantin.txt.XXXXXX")"
cat > "$config_tmp" <<EOF
dinamic_local_database=true
host_database=$(escape_property "$MAIN_HOST")
port_database=$(escape_property "$MAIN_PORT")
username=$(escape_property "$MAIN_USER")
password=$(escape_property "$MAIN_PASSWORD")
host_streaming_database=$(escape_property "$STREAM_HOST")
port_streaming_database=$(escape_property "$STREAM_PORT")
username_streaming=$(escape_property "$STREAM_USER")
password_streaming=$(escape_property "$STREAM_PASSWORD")
hbm2ddl=auto
EOF
chown root:"$SERVICE_USER" "$config_tmp"
chmod 0640 "$config_tmp"
mv -f "$config_tmp" "$CONFIG_FILE"

systemctl stop "$SERVICE_NAME"
# Direktori ini hanyalah hasil ekstraksi Tomcat dari WAR. Menghapusnya memaksa deploy ulang
# sehingga placeholder ${url} dan ${url_streaming} kembali bersih sebelum startup berikutnya.
rm -rf -- "${INSTALL_ROOT}/current/tomcat/webapps/kantin"
systemctl start "$SERVICE_NAME"
printf 'Konfigurasi tersimpan dan service %s sudah dinyalakan kembali.\n' "$SERVICE_NAME"
