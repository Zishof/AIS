#!/bin/sh
# Kompilasi SELURUH pohon sumber Java, lalu laporkan galatnya.
#
# Kenapa ada: penyapu commit yang berjalan di mesin ini meng-commit perubahan
# siapa pun dalam hitungan detik, tanpa langkah kompilasi. Itu sudah terbukti
# meloloskan berkas yang tidak dapat dikompilasi ke HEAD (Pegawai.java dgn
# @ManyToOne ganda -- docs/pos/76). Tidak ada gerbang lain; pemeriksaan ini
# satu-satunya jaring pengaman, dan hanya berguna kalau benar-benar dijalankan.
#
# Pakai:  sh kompilasi-penuh.sh [direktori-keluaran]
# Lama    : belasan menit, tanpa perlu ditunggui.
# Keluar  : 0 bila bersih, 1 bila ada galat (cocok untuk dipasang di penjadwal).
#
# JANGAN disalurkan ke pipa (mis. "| tail"): kode keluar yang terbaca menjadi
# milik perintah terakhir di pipa, sehingga kegagalan tampak seperti sukses.
# Jalankan langsung, atau baca ${PIPESTATUS[0]} bila memang perlu dipipa.

AKAR="${AKAR:-/c/opt/AIS/ais/src/main/java}"
LIB='C:\opt\AIS\ais\src\main\webapp\WEB-INF\lib\*'
KELUAR="${1:-${TMPDIR:-/tmp}/kompilasi-ais}"
DAFTAR="$KELUAR/daftar-java.txt"
LOG="$KELUAR/galat.log"

rm -rf "$KELUAR" && mkdir -p "$KELUAR" || exit 1
cd "$AKAR" || exit 1
find . -name '*.java' > "$DAFTAR"
echo "berkas sumber : $(wc -l < "$DAFTAR")"

# -source/-target 1.7 disengaja: server produksi masih Java 7, dan javac 8
# menerima konstruksi yang ditolak Java 7 (lihat docs/pos/36).
javac -source 1.7 -target 1.7 -encoding UTF-8 -nowarn -J-Xmx2g \
      -cp "$LIB" -d "$KELUAR" "@$DAFTAR" > "$LOG" 2>&1
kode=$?

galat=$(grep -c 'error:' "$LOG" 2>/dev/null || echo 0)
echo "kelas         : $(find "$KELUAR" -name '*.class' | wc -l)"
echo "galat         : $galat"
if [ "$galat" -gt 0 ]; then
    echo
    echo "--- galat pertama:"
    grep 'error:' "$LOG" | head -20
    echo
    echo "log lengkap: $LOG"
    exit 1
fi
[ $kode -ne 0 ] && { echo "javac keluar dgn kode $kode; lihat $LOG"; exit 1; }
echo "BERSIH"
exit 0
