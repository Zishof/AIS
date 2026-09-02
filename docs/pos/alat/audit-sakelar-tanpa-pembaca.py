# -*- coding: utf-8 -*-
"""AUDIT sakelar Konfigurasi yang tampaknya tidak dibaca logika mana pun.

BUKAN PENJAGA. Alat ini sengaja TIDAK dipasang sebagai gerbang lulus/gagal, dan
alasannya penting -- lihat "Batas" di bawah. Ia menyusun DAFTAR KANDIDAT untuk
ditelusuri manusia, bukan vonis.

Kelas cacat yang dicari sama seperti docs/pos/77: sebuah sakelar ditawarkan di
layar Konfigurasi, admin membaliknya, dan tidak terjadi apa-apa karena tak ada
kode yang membacanya. Tidak ada galat, tidak ada log; yang ada hanya admin yang
mengira sudah mengubah perilaku sistem.

Empat contoh sudah ditelusuri tangan sampai tuntas dan terbukti nyata
(docs/pos/84):

    audit_listener_aktif       "Aktifkan AuditListener untuk mencatat create,
                               edit..." -- AuditListener 2.224 baris tidak
                               pernah membaca kunci ini; ia selalu menyala.
    aktifkan_video_conference  "Aktifkan video conference menggunakan Jitsi"
    api_response_selalu_json   "Selalu kembalikan response API dalam format JSON"
    daftar_s1                  "Aktifkan Daftar Mahasiswa Baru"

BATAS -- inilah sebab alat ini bukan penjaga:

  1. Konfigurasi juga dibaca dengan kunci NON-literal. Di basis kode ini ada 198
     bentuk argumen berbeda yang bukan literal: `key` (69x), `kunci` (15x),
     `nama` (10x), dan seterusnya. Sakelar mana pun BISA saja dibaca lewat salah
     satu jalur itu, dan pemindaian literal tidak akan pernah melihatnya. Jadi
     alat ini dapat mengatakan "tidak ada pembaca literal"; ia TIDAK dapat
     mengatakan "tidak ada pembaca".

  2. Dua rumusan yang sama-sama masuk akal memberi angka berbeda: pencocokan
     substring memberi 218, pencocokan himpunan literal memberi 261. Selisih 43
     itu sendiri adalah ukuran kerapuhan pengukuran ini.

Penjaga yang menuduh 218 hal tanpa dapat membuktikan satu pun akan berhenti
dipercaya pada pemakaian pertama, dan penjaga yang tidak dipercaya sama saja
dengan tidak ada (docs/pos/83 3.1). Karena itu: audit, bukan gerbang.

Positif palsu yang SUDAH ditutup (jangan dibuka lagi tanpa alasan):

  * Kode yang dikomentari. `aktifkan_item_penilaian_uas` sempat dituduh padahal
    barisnya diawali `//` sehingga tidak pernah dirender. Komentar dibuang dulu.
  * Pembaca yang memakai nama konstanta, bukan literal. `Konfigurasi.X` dihitung
    sebagai pembaca bagi kunci yang nilainya X.
  * Berkas layar yang merender DAN membaca. Yang dikecualikan dari korpus
    pembaca hanya potongan di dalam panggilan createRow, bukan seluruh berkas.

Pakai:  python audit-sakelar-tanpa-pembaca.py [--semua]
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import akar_repo  # noqa: E402  -- perlu sys.path di atas

GARIS_MIRING = chr(47)
BINTANG = chr(42)
BALIK = chr(92)
BARIS = chr(10)

KONFIGURASI_JAVA = os.path.join(
    akar_repo.AIS_SRC, 'ais', 'database', 'model', 'Konfigurasi.java')

LAYAR = re.compile(r'^(Konfigurasi|ParameterUmum)')
PANGGIL = re.compile(
    r'createRow(Active|ActiveDefault|ActiveWithDefault|NotActive)\s*\(')
LIT = re.compile(r'"([^"]*)"')
KUNCI = re.compile(r'^[a-z][a-z0-9_]{2,}$')
KONSTANTA = re.compile(r'public static final String ([A-Z0-9_]+)\s*=\s*"([^"]+)"')

# Wilayah POS/kantin -- bagian yang menjadi tanggung jawab dokumen seri ini.
#
# Dicocokkan per TOKEN antar-garis-bawah, bukan sebagai substring. Versi
# substring menjaring `init_index_hindari_include_postgresql_lama`, karena
# "include_postgresql" memuat "_pos" -- penyaring yang dipakai untuk mempersempit
# tuduhan ternyata melebarkannya.
POS = set(['kantin', 'pos', 'koperasi', 'produk', 'stok', 'struk',
           'kasir', 'voucher'])


def tanpa_komentar(t):
    """Buang // dan /* */, tetapi pertahankan isi string literal."""
    keluar = []
    i, n = 0, len(t)
    while i < n:
        c = t[i]
        d = t[i + 1] if i + 1 < n else ''
        if c == GARIS_MIRING and d == GARIS_MIRING:
            j = t.find(BARIS, i)
            i = n if j < 0 else j
        elif c == GARIS_MIRING and d == BINTANG:
            j = t.find(BINTANG + GARIS_MIRING, i + 2)
            i = n if j < 0 else j + 2
        elif c == '"':
            j = i + 1
            while j < n and t[j] != '"' and t[j] != BARIS:
                j += 2 if t[j] == BALIK else 1
            keluar.append(t[i:j + 1])
            i = j + 1
        else:
            keluar.append(c)
            i += 1
    return ''.join(keluar)


def main():
    akar_repo.pastikan_lengkap()
    semua = '--semua' in sys.argv

    konst = {}
    for nama, nilai in KONSTANTA.findall(akar_repo.baca(KONFIGURASI_JAVA)):
        konst[nilai] = nama

    sakelar = {}
    render = {}
    for dp, dn, fn in os.walk(akar_repo.AIS_SRC):
        dn[:] = [d for d in dn if d.lower() not in ('build', '.svn')]
        for n in fn:
            if not n.endswith('.java') or not LAYAR.match(n):
                continue
            p = os.path.abspath(os.path.join(dp, n))
            t = tanpa_komentar(akar_repo.baca(p))
            rentang = []
            for m in PANGGIL.finditer(t):
                cuplik = t[m.end():m.end() + 400]
                rentang.append((m.end(), m.end() + 400))
                lit = LIT.findall(cuplik)
                for s in lit:
                    if KUNCI.match(s):
                        if s not in sakelar:
                            sakelar[s] = (lit[0][:70] if lit else '', n)
                        break
            render[p] = (t, rentang)

    korpus = []
    for akar, ext in ((akar_repo.AIS_SRC, ('.java', '.xml', '.properties')),
                      (akar_repo.AIS_WEBAPP, ('.jsp', '.js', '.jspf', '.tag'))):
        for dp, dn, fn in os.walk(akar):
            dn[:] = [d for d in dn
                     if d.lower() not in ('build', '.svn', 'node_modules')]
            for n in fn:
                if not n.endswith(ext):
                    continue
                p = os.path.abspath(os.path.join(dp, n))
                if p in render:
                    t, rentang = render[p]
                    sisa, lalu = [], 0
                    for a, b in rentang:
                        sisa.append(t[lalu:a])
                        lalu = min(b, len(t))
                    sisa.append(t[lalu:])
                    korpus.append(''.join(sisa))
                else:
                    korpus.append(tanpa_komentar(akar_repo.baca(p)))
    korpus = BARIS.join(korpus)
    akar_repo.pastikan_terbaca()

    print('sakelar ditawarkan layar Konfigurasi : %d' % len(sakelar))
    print('korpus pembaca                       : %d karakter' % len(korpus))

    yatim = []
    for k in sorted(sakelar):
        if ('"%s"' % k) in korpus:
            continue
        c = konst.get(k)
        if c and ('Konfigurasi.' + c) in korpus:
            continue
        yatim.append(k)

    kena_pos = [k for k in yatim if POS.intersection(k.split('_'))]

    print('')
    print('== Wilayah POS/kantin ==')
    if kena_pos:
        for k in kena_pos:
            print('   %-44s %s' % (k, sakelar[k][0][:52]))
    else:
        print('   (bersih) -- tidak ada sakelar POS tanpa pembaca literal')

    print('')
    print('== Seluruh basis kode: %d kandidat ==' % len(yatim))
    print('   Ini KANDIDAT, bukan vonis -- baca "Batas" di kepala berkas ini.')
    if semua:
        for k in yatim:
            print('   %-52s %s' % (k, sakelar[k][0][:44]))
    else:
        grup = {}
        for k in yatim:
            grup.setdefault(k.split('_')[0], []).append(k)
        for g in sorted(grup, key=lambda x: -len(grup[x]))[:15]:
            print('   %-20s %3d   contoh: %s' % (g, len(grup[g]), grup[g][0][:44]))
        print('   (--semua untuk daftar lengkap)')

    # Selalu 0: alat ini melapor, tidak memvonis.
    return 0


if __name__ == '__main__':
    sys.exit(main())
