#!/usr/bin/env bash
set -Eeuo pipefail
umask 027

PAYLOAD_DIR="${1:-}"
INSTALL_ROOT="${AIS_INSTALL_ROOT:-/opt/ais-kantin}"
SERVICE_NAME="ais-kantin"
SERVICE_USER="${AIS_SERVICE_USER:-ais-kantin}"
HTTP_PORT="${AIS_HTTP_PORT:-8080}"
JAVA_XMS="${AIS_JAVA_XMS:-512m}"
JAVA_XMX="${AIS_JAVA_XMX:-2048m}"
CONTEXT_NAME="kantin"
CONFIG_DIR="/opt/.g/.h"
CONFIG_FILE="${CONFIG_DIR}/${CONTEXT_NAME}.txt"

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
info() { printf '\n==> %s\n' "$*"; }

[[ "${EUID}" -eq 0 ]] || die "Installer harus dijalankan sebagai root (sudo)."
[[ -d "$PAYLOAD_DIR" && -x "${PAYLOAD_DIR}/jre/bin/java" ]] || die "Payload installer tidak lengkap."
command -v systemctl >/dev/null 2>&1 || die "systemd/systemctl diperlukan."
command -v useradd >/dev/null 2>&1 || die "useradd tidak tersedia."
[[ "$HTTP_PORT" =~ ^[0-9]+$ ]] && ((HTTP_PORT >= 1 && HTTP_PORT <= 65535)) || die "AIS_HTTP_PORT tidak valid."
[[ "$SERVICE_USER" =~ ^[a-z_][a-z0-9_-]*$ ]] || die "AIS_SERVICE_USER tidak valid."
[[ "$INSTALL_ROOT" == /* && "$INSTALL_ROOT" != *[[:space:]]* ]] || die "AIS_INSTALL_ROOT harus path absolut tanpa spasi."
[[ "$JAVA_XMS" =~ ^[0-9]+[mMgG]$ && "$JAVA_XMX" =~ ^[0-9]+[mMgG]$ ]] || die "AIS_JAVA_XMS/AIS_JAVA_XMX tidak valid."
[[ -t 0 ]] || die "Instalasi interaktif memerlukan terminal."

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
    while [[ -z "$value" ]]; do
        read -r -s -p "${label}: " value
        printf '\n' >&2
    done
    printf '%s' "$value"
}

validate_host() {
    [[ "$1" =~ ^[A-Za-z0-9._:\[\]-]+$ ]] || die "Host database mengandung karakter yang tidak didukung."
}

validate_port() {
    [[ "$1" =~ ^[0-9]+$ ]] && (($1 >= 1 && $1 <= 65535)) || die "Port database tidak valid: $1"
}

escape_property() {
    local value="$1"
    value="${value//\\/\\\\}"
    value="${value//:/\\:}"
    value="${value//=/\\=}"
    value="${value// /\\ }"
    value="${value//#/\\#}"
    value="${value//!/\\!}"
    printf '%s' "$value"
}

check_tcp() {
    local label="$1" host="$2" port="$3"
    if command -v timeout >/dev/null 2>&1; then
        if timeout 4 bash -c "</dev/tcp/${host}/${port}" >/dev/null 2>&1; then
            printf '  [OK] %s dapat dijangkau (%s:%s)\n' "$label" "$host" "$port"
        else
            printf '  [PERINGATAN] %s belum dapat dijangkau (%s:%s). Instalasi tetap dilanjutkan.\n' "$label" "$host" "$port" >&2
        fi
    fi
}

cat <<'BANNER'
============================================================
     INSTALASI AIS e-KANTIN - JVM + TOMCAT TERBUNDEL
============================================================
Database yang digunakan: kantin dan streaming_kantin
BANNER

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
check_tcp "Database utama" "$MAIN_HOST" "$MAIN_PORT"
check_tcp "Database streaming" "$STREAM_HOST" "$STREAM_PORT"

info "Membuat user service dan release aplikasi"
if ! id "$SERVICE_USER" >/dev/null 2>&1; then
    useradd --system --home-dir "$INSTALL_ROOT" --shell /usr/sbin/nologin "$SERVICE_USER"
fi
release_id="$(date -u +%Y%m%d%H%M%S)"
release_dir="${INSTALL_ROOT}/releases/${release_id}"
mkdir -p "$release_dir" "$INSTALL_ROOT/releases"
if [[ ! -d /opt/cache ]]; then
    install -d -m 0770 -o "$SERVICE_USER" -g "$SERVICE_USER" /opt/cache
else
    chgrp "$SERVICE_USER" /opt/cache
    chmod g+rwx /opt/cache
fi
cp -a "${PAYLOAD_DIR}/jre" "$release_dir/jre"
cp -a "${PAYLOAD_DIR}/tomcat" "$release_dir/tomcat"
mkdir -p "$release_dir/tomcat/webapps" "$release_dir/tomcat/logs" "$release_dir/tomcat/temp" "$release_dir/tomcat/work"
cp -- "${PAYLOAD_DIR}/app/kantin.war" "$release_dir/tomcat/webapps/kantin.war"

sed -i -E '0,/port="8080"/s//port="'"$HTTP_PORT"'"/' "$release_dir/tomcat/conf/server.xml"
sed -i -E '0,/port="8005" shutdown=/s//port="-1" shutdown=/' "$release_dir/tomcat/conf/server.xml"
cat > "$release_dir/tomcat/bin/setenv.sh" <<EOF
#!/usr/bin/env bash
export JAVA_HOME="${INSTALL_ROOT}/current/jre"
export JRE_HOME="\${JAVA_HOME}"
export CATALINA_HOME="${INSTALL_ROOT}/current/tomcat"
export CATALINA_BASE="\${CATALINA_HOME}"
export CATALINA_PID="${INSTALL_ROOT}/current/tomcat/temp/tomcat.pid"
export CATALINA_OPTS="-Djava.awt.headless=true -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Jakarta -Dais.init.async=true -Xms${JAVA_XMS} -Xmx${JAVA_XMX}"
EOF
chmod 0750 "$release_dir/tomcat/bin/setenv.sh"
chown -R "$SERVICE_USER:$SERVICE_USER" "$release_dir"
ln -sfn "$release_dir" "${INSTALL_ROOT}/current.new"
mv -Tf "${INSTALL_ROOT}/current.new" "${INSTALL_ROOT}/current"

info "Menulis konfigurasi database"
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

install -m 0750 -o root -g root "${PAYLOAD_DIR}/configure.sh" /usr/local/sbin/ais-kantin-configure

info "Mendaftarkan service systemd"
cat > "/etc/systemd/system/${SERVICE_NAME}.service" <<EOF
[Unit]
Description=AIS e-Kantin (bundled Temurin JRE and Tomcat)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=${SERVICE_USER}
Group=${SERVICE_USER}
Environment=JAVA_HOME=${INSTALL_ROOT}/current/jre
Environment=JRE_HOME=${INSTALL_ROOT}/current/jre
Environment=CATALINA_HOME=${INSTALL_ROOT}/current/tomcat
Environment=CATALINA_BASE=${INSTALL_ROOT}/current/tomcat
ExecStart=${INSTALL_ROOT}/current/tomcat/bin/catalina.sh run
ExecStop=/bin/kill -s TERM \$MAINPID
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
TimeoutStartSec=180
TimeoutStopSec=60
LimitNOFILE=65535
NoNewPrivileges=true
PrivateTmp=true
ProtectHome=true
ProtectSystem=full
ReadWritePaths=${INSTALL_ROOT} /opt/cache

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable "$SERVICE_NAME" >/dev/null
systemctl restart "$SERVICE_NAME"

info "Instalasi selesai"
printf 'Service : %s\n' "$SERVICE_NAME"
printf 'URL     : http://ALAMAT-SERVER:%s/%s/\n' "$HTTP_PORT" "$CONTEXT_NAME"
printf 'Status  : sudo systemctl status %s\n' "$SERVICE_NAME"
printf 'Log     : sudo journalctl -u %s -f\n' "$SERVICE_NAME"
printf 'Ubah DB : sudo /usr/local/sbin/ais-kantin-configure\n'
