#!/bin/sh
# Kompilasi HANYA berkas Java yang berubah sejak titik waktu tertentu.
#
# Pelengkap kompilasi-penuh.sh, dan yang ini dimaksudkan untuk BENAR-BENAR
# dijalankan. Kompilasi penuh makan belasan menit, jadi tidak akan pernah
# dipakai sebelum commit; yang ini menit-menitan karena hanya menyentuh yang
# berubah, dan itu sudah cukup: dua kerusakan HEAD pada 2026-09-02
# (docs/pos/76 dan docs/pos/81) dua-duanya galat di dalam berkas yang baru
# saja disunting, jadi keduanya akan tertangkap di sini.
#
# Pakai:
#   sh kompilasi-berubah.sh              # sejak awal hari ini
#   sh kompilasi-berubah.sh 2026-09-01   # sejak tanggal
#   sh kompilasi-berubah.sh 83000        # sejak revisi
#
# Keluar: 0 bila bersih, 1 bila ada galat.
#
# JANGAN disalurkan ke pipa: kode keluarnya menjadi milik perintah terakhir di
# pipa, sehingga kegagalan tampak seperti sukses. (Jebakan itu sudah termakan
# sekali -- docs/pos/81.)

AKAR="${AKAR:-/c/opt/AIS/ais/src/main/java}"
LIB='C:\opt\AIS\ais\src\main\webapp\WEB-INF\lib\*'
SEJAK="${1:-$(date +%Y-%m-%d)}"
KERJA="${TMPDIR:-/tmp}/kompilasi-berubah"

# Revisi ditulis apa adanya; tanggal dibungkus kurung kurawal spt sintaks SVN.
case "$SEJAK" in
    [0-9]*[!0-9-]*|*-*) SPEK="{$SEJAK}" ;;
    *)                  SPEK="$SEJAK"   ;;
esac

# Direktori keluaran WAJIB dibuat: javac -d menolak direktori yang belum ada,
# dan gagalnya berupa "directory not found" TANPA baris "error:" -- penghitung
# galat membaca nol dan gerbangnya melaporkan BERSIH padahal tidak satu berkas
# pun dikompilasi. Jebakan itu sudah termakan sekali di sini.
rm -rf "$KERJA" && mkdir -p "$KERJA/kelas" || exit 1
cd "$AKAR" || exit 1

# Berkas yang berubah menurut repositori, DITAMBAH yang masih tersunting lokal
# (working copy ini dipakai beberapa sesi sekaligus -- lihat docs/pos/07 no. 6).
{
    svn diff --summarize -r "$SPEK:HEAD" . 2>/dev/null | sed 's/^........//'
    svn status . 2>/dev/null | grep '^[MA]' | sed 's/^........//'
} | sed 's#\\#/#g' | grep '\.java$' | sort -u > "$KERJA/berubah.txt"

# Yang sudah dihapus tidak bisa dikompilasi.
: > "$KERJA/daftar.txt"
while IFS= read -r f; do
    [ -f "$f" ] && echo "$f" >> "$KERJA/daftar.txt"
done < "$KERJA/berubah.txt"

jml=$(wc -l < "$KERJA/daftar.txt")
echo "sejak         : $SPEK"
echo "berkas diuji  : $jml"
[ "$jml" -eq 0 ] && { echo "tidak ada yang berubah"; exit 0; }

# -sourcepath supaya dependensinya diambil dari sumber, bukan dari pohon kelas
# yang mungkin sudah basi. -source/-target 1.7: server produksi masih Java 7.
javac -source 1.7 -target 1.7 -encoding UTF-8 -nowarn -J-Xmx2g \
      -sourcepath . -cp "$LIB" -d "$KERJA/kelas" "@$KERJA/daftar.txt" \
      > "$KERJA/galat.log" 2>&1
kode=$?

galat=$(grep -c 'error:' "$KERJA/galat.log" 2>/dev/null)
[ -z "$galat" ] && galat=0
echo "galat         : $galat"
kelas=$(find "$KERJA/kelas" -name '*.class' 2>/dev/null | wc -l)
echo "kelas         : $kelas"

# Tiga syarat, bukan satu. Menghitung baris "error:" saja tidak cukup: javac bisa
# gagal SEBELUM sempat mengompilasi (opsi salah, classpath tak terbaca) dan
# keluarannya tidak memuat satu pun baris "error:".
if [ "$kode" -ne 0 ] && [ "$galat" -eq 0 ]; then
    echo
    echo "javac gagal TANPA galat kompilasi -- kemungkinan salah pemanggilan:"
    head -5 "$KERJA/galat.log"
    exit 1
fi
if [ "$kelas" -eq 0 ]; then
    echo
    echo "tidak satu kelas pun dihasilkan; kompilasinya tidak benar-benar berjalan."
    head -5 "$KERJA/galat.log"
    exit 1
fi
if [ "$galat" -gt 0 ]; then
    echo
    grep 'error:' "$KERJA/galat.log" | head -20
    echo
    echo "log lengkap: $KERJA/galat.log"
    exit 1
fi
echo "BERSIH"
exit 0
