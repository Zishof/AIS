# -*- coding: utf-8 -*-
"""Periksa tiap entri katalog laporan POS punya cabang pelaksana.

Katalog di LaporanKatalogData.java mendaftar laporan dengan kunci, mis.

    k.items.add(item("pnj_faktur", "Daftar Faktur Penjualan", ...))

dan LaporanKantinUtil melaksanakannya dengan rantai

    if ("pnj_faktur".equals(r)) { ... }

Kunci yang tidak punya cabang JATUH ke penutup rantai, yang menyetel
status="soon" dan pesan "Laporan ini sedang disiapkan". Jadi ia tidak
menghasilkan galat -- ia menghasilkan laporan kosong berpesan, dan hanya
ketahuan sesudah pengguna memilih laporannya lalu menjalankannya.

Alat ini melaporkan kunci semacam itu. Perlu diingat: BUKAN semuanya cacat.
Sebagian memang placeholder yang disengaja. Yang layak ditanyakan adalah apakah
katalognya menyebutkan hal itu -- pada saat alat ini ditulis, tidak.

Pakai:  python katalog-laporan-tanpa-pelaksana.py [akar-java]
Keluar: 0 bila semua kunci punya pelaksana, 1 bila ada yang tidak.
"""
import os
import re
import sys

AKAR = sys.argv[1] if len(sys.argv) > 1 else r'C:\opt\AIS\ais\src\main\java'
KATALOG = os.path.join('ais', 'action', 'master', 'koperasi', 'helper',
                       'LaporanKatalogData.java')


def main():
    kat = os.path.join(AKAR, KATALOG)
    if not os.path.isfile(kat):
        print('katalog tidak ditemukan: %s' % kat)
        return 2
    teks = open(kat, encoding='utf-8', errors='replace').read()
    kunci = re.findall(r'item\(\s*"([a-z0-9_]+)"', teks)
    judul = dict(re.findall(r'item\(\s*"([a-z0-9_]+)"\s*,\s*"([^"]*)"', teks))

    isi = []
    for dp, _, fs in os.walk(AKAR):
        for f in fs:
            if not f.endswith('.java'):
                continue
            p = os.path.join(dp, f)
            if os.path.abspath(p) == os.path.abspath(kat):
                continue
            try:
                isi.append(open(p, encoding='utf-8', errors='replace').read())
            except Exception:
                pass

    tanpa = [k for k in sorted(set(kunci)) if not any(('"%s"' % k) in t for t in isi)]
    print('entri katalog        : %d  (kunci unik: %d)' % (len(kunci), len(set(kunci))))
    print('berkas sumber dipindai: %d' % len(isi))
    print('kunci tanpa pelaksana : %d\n' % len(tanpa))
    for k in tanpa:
        print('   %-22s %s' % (k, judul.get(k, '')))
    return 1 if tanpa else 0


if __name__ == '__main__':
    sys.exit(main())
