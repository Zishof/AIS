# -*- coding: utf-8 -*-
"""Untuk tiap blok JavaDoc yatim, tebak anggota mana yang SEHARUSNYA didokumentasikannya.

Pelengkap javadoc-yatim.py. Alat itu hanya memberi tahu bahwa sebuah blok tidak
menempel ke apa pun; alat ini mencoba menemukan tuannya, supaya remedi PINDAH
dapat dikerjakan tanpa membaca seluruh berkas lebih dulu.

Cara menebak: blok yatim biasanya menyebut nama tuannya di dalam teksnya sendiri
-- lewat {@link #namaMethod} atau {@code namaMethod}. Kandidat disaring ke
deklarasi pada berkas yang SAMA yang saat ini TIDAK berdokumen, karena blok yang
terlepas meninggalkan tuannya kosong. Itu pula bukti terkuat bahwa remedinya
PINDAH, bukan GABUNG.

Dua kasus yang sudah dikerjakan sbg contoh (r83057):
  KantinHelper.java   "Fase 1: validasi stok server-side" -> validasiStokCukupDenganLock
  HibernateUtil.java  "session native berbasis ThreadLocal" -> currentNativeSession

Pakai:  python javadoc-cari-tuan.py [akar]
"""
import os
import re
import sys

AKAR = sys.argv[1] if len(sys.argv) > 1 else r'C:\opt\AIS\ais\src\main\java'

BLOK = re.compile(r'/\*\*.*?\*/', re.S)
YATIM = re.compile(r'\*/[ \t]*\r?\n(?:[ \t]*\r?\n)*[ \t]*/\*\*')
DEKL = re.compile(
    r'^[ \t]*(?:@\w+[^\n]*\r?\n[ \t]*)*'
    r'(?:public|protected|private)[\w\s<>\[\],.$]*?\b(\w+)\s*\(', re.M)
SEBUT = re.compile(r'\{@(?:link|code)\s+#?(\w+)|\b([a-z]\w{4,})\s*\(\)')


def berdokumen(teks, pos):
    """Apakah deklarasi pada `pos` didahului blok JavaDoc yang menempel?"""
    return teks[:pos].rstrip().endswith('*/')


def periksa(teks):
    dekl = {}
    for m in DEKL.finditer(teks):
        dekl.setdefault(m.group(1), []).append(berdokumen(teks, m.start()))
    tanpa_dok = {n for n, v in dekl.items() if not all(v)}

    hasil = []
    for m in BLOK.finditer(teks):
        if not re.match(r'\s*/\*\*', teks[m.end():m.end() + 200]):
            continue
        disebut = {a or b for a, b in SEBUT.findall(m.group(0))} & tanpa_dok
        hasil.append((teks.count('\n', 0, m.start()) + 1, sorted(disebut)[:3]))
    return hasil


if __name__ == '__main__':
    # Saringan murah lebih dulu. Tanpa ini alat memindai 7.000+ berkas dan tidak
    # selesai dalam waktu wajar; yang punya blok yatim hanya sekitar sembilan puluh.
    kandidat = []
    for dp, _, fs in os.walk(AKAR):
        for f in fs:
            if not f.endswith('.java'):
                continue
            jalur = os.path.join(dp, f)
            try:
                teks = open(jalur, encoding='utf-8', errors='replace').read()
            except Exception:
                continue
            if YATIM.search(teks):
                kandidat.append((jalur, teks))

    total = bertuan = 0
    for jalur, teks in kandidat:
        try:
            temuan = periksa(teks)
        except Exception:
            continue
        for baris, calon in temuan:
            total += 1
            if calon:
                bertuan += 1
                print('%s:%d  -> %s' % (os.path.relpath(jalur, AKAR), baris,
                                        ', '.join(calon)))
    print('\nberkas dianalisis     : %d' % len(kandidat))
    print('blok yatim            : %d' % total)
    print('punya calon tuan      : %d' % bertuan)
    print('sisanya perlu dibaca manual (bloknya tidak menyebut nama tuannya)')
