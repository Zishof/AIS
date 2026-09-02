# -*- coding: utf-8 -*-
"""Cari blok JavaDoc YATIM: dua blok beruntun tanpa deklarasi di antaranya.

Javadoc dan hover IDE hanya membaca blok TERAKHIR sebelum sebuah deklarasi.
Blok di atasnya tidak menempel ke apa pun -- isinya tetap ada di berkas, tetapi
tidak pernah terlihat oleh siapa pun yang membaca dokumentasi.

Perbaikannya TIDAK bisa diseragamkan, dan itulah sebabnya alat ini hanya
melaporkan, tidak menyunting:

  GABUNG -- bila kedua blok membicarakan anggota yang sama (blok atas biasanya
            versi lama atau lanjutan uraian); satukan menjadi satu blok.
  PINDAH -- bila blok atas membicarakan anggota LAIN yang dideklarasikan di
            tempat lain pada berkas yang sama; pindahkan ke deklarasinya.
            Menggabungnya justru menempelkan dokumentasi ke anggota yang salah.

Contoh PINDAH yang sudah dikerjakan: Toko.roleBolehUbahHarga (r83001).

Pakai:  python javadoc-yatim.py [akar]
"""
import os
import re
import sys

AKAR = sys.argv[1] if len(sys.argv) > 1 else r'C:\opt\AIS\ais\src\main\java'

# Baris kosong di antara dua blok tidak membuat blok atas menempel:
# tetap yatim, jadi ikut dihitung.
PASANGAN = re.compile(r'\*/[ \t]*\r?\n(?:[ \t]*\r?\n)*[ \t]*/\*\*')
DEKLARASI = re.compile(r'(?:public|protected|private)[\w\s<>\[\],.]*?\b(\w+)\s*[(;=]')
ANGGOTA = re.compile(r'\b(?:get|set|is)([A-Z]\w*)\s*\(')


def blok_di_atas(teks, akhir):
    """Isi blok JavaDoc yang berakhir pada posisi `akhir` (indeks '*/')."""
    mulai = teks.rfind('/**', 0, akhir)
    return teks[mulai:akhir] if mulai >= 0 else ''


def anggota_setelah(teks, pos):
    """Nama anggota yang dideklarasikan sesudah pasangan blok."""
    m = DEKLARASI.search(teks, pos)
    return m.group(1) if m else None


def telusuri(akar):
    temuan = []
    for dp, _, berkas in os.walk(akar):
        for f in berkas:
            if not f.endswith('.java'):
                continue
            jalur = os.path.join(dp, f)
            try:
                teks = open(jalur, encoding='utf-8', errors='replace').read()
            except Exception:
                continue
            for m in PASANGAN.finditer(teks):
                atas = blok_di_atas(teks, m.start())
                sesudah = anggota_setelah(teks, m.end())
                # nama anggota yang DISEBUT blok yatim
                disebut = set(ANGGOTA.findall(atas))
                # apakah salah satunya dideklarasikan di tempat lain pd berkas ini?
                lain = [n for n in disebut
                        if sesudah and n.lower() not in (sesudah or '').lower()
                        and re.search(r'\b(?:get|set|is)%s\s*\(' % re.escape(n), teks)]
                temuan.append({
                    'berkas': os.path.relpath(jalur, akar),
                    'baris': teks.count('\n', 0, m.start()) + 1,
                    'anggota': sesudah,
                    'dugaan': 'PINDAH?' if lain else 'GABUNG',
                    'menyebut': sorted(lain)[:3],
                })
    return temuan


if __name__ == '__main__':
    hasil = telusuri(AKAR)
    pindah = [t for t in hasil if t['dugaan'].startswith('PINDAH')]
    print('pasangan JavaDoc yatim : %d' % len(hasil))
    print('berkas terdampak       : %d' % len({t['berkas'] for t in hasil}))
    print('dugaan PINDAH          : %d  (sisanya GABUNG)\n' % len(pindah))
    print('== dugaan PINDAH (perlu penilaian manusia lebih dulu) ==')
    for t in pindah:
        print('%s:%d  menempel ke %s, tetapi menyebut %s'
              % (t['berkas'], t['baris'], t['anggota'], ', '.join(t['menyebut'])))
