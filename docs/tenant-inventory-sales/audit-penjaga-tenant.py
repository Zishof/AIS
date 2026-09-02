# -*- coding: utf-8 -*-
"""
Audit penjaga tenant: memastikan SETIAP aksi si_* yang dirutekan dispatcher punya
penjaga atau cabang jalur tenant.

CARA PAKAI (dari mana saja):
    python audit-penjaga-tenant.py [akar-sumber]

    akar-sumber bawaan: C:\\opt\\AIS\\ais\\src\\main\\java\\ais\\action\\servlet\\api

Keluar dengan kode 1 bila ada aksi tanpa penjaga, sehingga bisa dipasang di rangkaian
pemeriksaan mana pun.

-------------------------------------------------------------------------------------
MENGAPA PEMERIKSAAN INI ADA

Aksi yang lupa diberi penjaga tidak gagal, tidak berisik, dan tidak terlihat pada uji
fungsional biasa -- ia hanya diam-diam MEMBACA DAN MENULIS KE SCHEMA BERSAMA untuk
pemakai tenant. Dua akibatnya sekaligus buruk: data tenant mendarat di tempat yang
salah, dan schema tenantnya sendiri tetap kosong sehingga layarnya tampak tidak
menyimpan apa-apa.

Pemeriksaan ini menemukan satu lubang nyata saat pertama dijalankan: si_import_legacy,
yang menulis tiga belas jenis entitas Hibernate tanpa penjaga apa pun.

-------------------------------------------------------------------------------------
BATAS PEMERIKSAAN -- BACA SEBELUM MEMPERCAYAI HASILNYA

1. Ini pemeriksaan TEKSTUAL, bukan analisis alur. Ia hanya mencari kemunculan
   "Tenant.aktif" atau "TenantSchema.aktif" di dalam badan metode.
2. Delegasi ditelusuri sampai DUA tingkat ke metode statis sekelas. Aksi yang
   menyembunyikan penjaganya lebih dalam dari itu akan dilaporkan sebagai lubang
   (positif palsu) -- versi pertama pemeriksaan ini tidak menelusuri delegasi sama
   sekali dan melaporkan tujuh positif palsu.
3. Ia TIDAK memeriksa apakah penjaganya benar, hanya bahwa ada. Penjaga yang salah
   letak -- misalnya sebelum pemeriksaan hak akses -- lolos di sini.

DIKECUALIKAN dengan sengaja:
    si_actor_context -- hanya mengembalikan konteks aktor pemanggil, tidak menyentuh
    schema mana pun, sehingga tidak ada yang perlu dijaga.
"""
import io
import os
import re
import sys

DIKECUALIKAN = set(['si_actor_context'])

AKAR_BAWAAN = os.path.join('C:', os.sep, 'opt', 'AIS', 'ais', 'src', 'main', 'java',
                           'ais', 'action', 'servlet', 'api')

TANDA_PENJAGA = ('Tenant.aktif', 'TenantSchema.aktif')

_cache = {}


def sumber(akar, berkas):
    if berkas not in _cache:
        jalur = os.path.join(akar, berkas)
        if not os.path.exists(jalur):
            _cache[berkas] = None
        else:
            _cache[berkas] = io.open(jalur, encoding='utf-8', newline='').read()
    return _cache[berkas]


def badan(akar, berkas, metode):
    """Badan satu metode statis, dari tanda tangannya sampai tanda tangan berikutnya."""
    s = sumber(akar, berkas)
    if s is None:
        return None
    awal = re.search(r'\t(?:public|private) static \w[\w<>\[\], .]* %s\('
                     % re.escape(metode), s)
    if not awal:
        return None
    lanjut = re.search(r'\n\t(?:public|private) static \w[\w<>\[\], .]* \w+\(', s[awal.end():])
    akhir = awal.end() + lanjut.start() if lanjut else len(s)
    return s[awal.start():akhir]


def terjaga(akar, berkas, metode, dalam=0):
    b = badan(akar, berkas, metode)
    if b is None or dalam > 2:
        return False
    for tanda in TANDA_PENJAGA:
        if tanda in b:
            return True
    for kandidat in set(re.findall(r'\b([a-z]\w+)\(', b)):
        if kandidat == metode:
            continue
        if badan(akar, berkas, kandidat) is not None \
                and terjaga(akar, berkas, kandidat, dalam + 1):
            return True
    return False


def main():
    akar = sys.argv[1] if len(sys.argv) > 1 else AKAR_BAWAAN
    disp = sumber(akar, 'SalesInventoryApiDispatcher.java')
    if disp is None:
        print('GAGAL: dispatcher tidak ditemukan di %s' % akar)
        return 2
    pasangan = re.findall(
        r'"(si_[a-z_]+)"\.equals\(action\)\)\s*\{\s*(?://[^\n]*\n\s*)*([A-Za-z]+Helper)\.(\w+)\(',
        disp)
    if not pasangan:
        print('GAGAL: tidak satu pun aksi si_* terbaca dari dispatcher --'
              ' polanya mungkin sudah berubah.')
        return 2

    bolong = []
    for aksi, helper, metode in pasangan:
        if aksi in DIKECUALIKAN:
            continue
        if not terjaga(akar, helper + '.java', metode):
            bolong.append((aksi, helper.replace('SalesInventory', '') + '.' + metode))

    print('Aksi si_* terdaftar : %d' % len(pasangan))
    print('Dikecualikan        : %s' % ', '.join(sorted(DIKECUALIKAN)))
    print('Tanpa penjaga tenant: %d' % len(bolong))
    if bolong:
        print()
        for aksi, target in bolong:
            print('  LUBANG  %-28s -> %s' % (aksi, target))
        print()
        print('Aksi tanpa penjaga akan MEMBACA DAN MENULIS KE SCHEMA BERSAMA untuk'
              ' pemakai tenant.')
        return 1
    print()
    print('LULUS: setiap aksi punya penjaga atau cabang jalur tenant.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
