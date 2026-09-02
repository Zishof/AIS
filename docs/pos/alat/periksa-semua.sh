#!/bin/sh
# Jalankan seluruh pemeriksaan CEPAT sekaligus, lalu ringkas hasilnya.
#
# Alat di direktori ini tumbuh menjadi sembilan dengan tiga cara pemanggilan
# berbeda (sh, python, powershell) dan sebagian menuntut argumen. Tidak ada yang
# akan mengingat semuanya, dan pemeriksa yang tidak dijalankan tidak menemukan
# apa pun -- pelajaran yang sudah dibayar mahal di docs/pos/82.
#
# Yang dijalankan di sini hanya yang MURAH (detik sampai beberapa menit) dan
# tidak menuntut pohon kelas:
#
#   zul-rujukan-kelas.py     rujukan kelas pada 1.557 berkas .zul
#   menu-tujuan-hilang.py    tujuan 1.049 butir menu
#   include-tujuan-hilang.py include runtime pada JSP dan ZUL
#   subreport-hilang.py      subreport di dalam .jrxml
#   jsp-terjemah.ps1         terjemahan seluruh JSP (Jasper)
#
# Yang TIDAK dijalankan di sini, karena mahal atau butuh persiapan:
#
#   kompilasi-penuh.sh    belasan menit; jalankan berkala, tanpa ditunggui
#   kompilasi-berubah.sh  hanya berkas berubah; jalankan sebelum commit
#   jsp-scriptlet.ps1     menuntut pohon kelas SEGAR (lihat docs/pos/87)
#
# Keluar: 0 bila semua bersih, 1 bila ada pemeriksaan yang menemukan sesuatu.
# JANGAN disalurkan ke pipa -- kode keluarnya akan menjadi milik perintah
# terakhir di pipa (docs/pos/82).

ALAT=$(cd "$(dirname "$0")" && pwd)
gagal=0
ringkas=""

jalankan() {
    nama="$1"; shift
    keluaran=$("$@" 2>&1)
    kode=$?
    baris=$(echo "$keluaran" | grep -iE 'hilang|menggantung' | head -3)
    if [ $kode -eq 0 ]; then
        ringkas="$ringkas\n  BERSIH  $nama"
    else
        gagal=1
        ringkas="$ringkas\n  TEMUAN  $nama"
        printf '\n=== %s\n%s\n' "$nama" "$keluaran"
    fi
}

jalankan "rujukan kelas ZUL" python "$ALAT/zul-rujukan-kelas.py"
jalankan "tujuan butir menu" python "$ALAT/menu-tujuan-hilang.py"
jalankan "include runtime"   python "$ALAT/include-tujuan-hilang.py"
jalankan "subreport jrxml"   python "$ALAT/subreport-hilang.py"

if command -v powershell.exe >/dev/null 2>&1; then
    jalankan "terjemahan JSP" powershell.exe -NoProfile -ExecutionPolicy Bypass \
        -File "$ALAT/jsp-terjemah.ps1" -Semua
else
    ringkas="$ringkas\n  DILEWATI terjemahan JSP (powershell tidak ada)"
fi

printf '\n===== RINGKASAN%b\n' "$ringkas"
if [ $gagal -eq 0 ]; then
    printf 'Semua pemeriksaan cepat bersih.'
else
    printf 'Ada pemeriksaan yang menemukan sesuatu; rinciannya di atas.'
fi
echo
exit $gagal
