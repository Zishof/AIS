# -*- coding: utf-8 -*-
"""AUDIT kolom entitas yang dapat DISIMPAN tetapi tidak pernah DIBACA.

BUKAN PENJAGA. Ia menyusun daftar kandidat untuk ditelusuri manusia; keluar
selalu dengan kode 0. Alasannya sama seperti docs/pos/84 dan 92: 18 kandidat
yang dihasilkannya belum ditelusuri satu per satu, dan gerbang di atas dasar
seperti itu menuduh yang tidak bersalah.

PERTANYAANNYA SENGAJA SEMPIT. docs/pos/95 mencoba menjawab "apakah nilainya
DIBANDINGKAN server?" dan gagal: heuristik kedekatan menyatakan 12 dari 12 batas
tidak ditegakkan, padahal maksimal_transaksi_harian ditegakkan dengan rapi --
nilainya disalin ke `double[] batas` lebih dulu sehingga perbandingannya jauh
dari getter-nya.

Yang ditanyakan di sini jauh lebih mekanis dan tidak memakai heuristik jarak:

    setter-nya dipanggil dari luar direktori model  (admin dapat menyimpannya)
    getter-nya TIDAK PERNAH dipanggil, di mana pun

Kolom seperti itu tidak mungkin dibaca lewat mekanisme apa pun. Ini bukan soal
bentuk perbandingan, melainkan tidak ada pembacaan sama sekali.

EMPAT JALUR BACA yang wajib dikecualikan sebelum menuduh -- ketiadaan salah
satunya sudah terbukti menghasilkan tuduhan palsu:

  1. getter dipanggil di kode Java lain
  2. nama properti disebut di JSP/ZUL (EL / binding ZK) atau di klien Dart
  3. nama properti sebagai string di HQL/Criteria
  4. NAMA KOLOM snake_case di SQL native -- inilah yang paling sering terlewat.
     Tanpa saringan ini, `sesiKasKasir` dituduh mati padahal `sesi_kas_kasir`
     muncul 47 kali di kueri native. Saringan ini saja menurunkan 41 kandidat
     menjadi 18.

Dua kluster sudah ditelusuri tangan sampai tuntas (docs/pos/96):

  * alasanReversal / reversalDari pada tiga entitas -- alasan reversal WAJIB
    diketik Pemilik/Admin, disimpan, dan tidak pernah ditampilkan di layar mana
    pun.
  * totalPenerimaanTunai / NonTunai / totalPembayaranPembelian pada
    NotaSalesSession -- dihitung saat tutup trip, tidak pernah dibaca kembali.

Pakai:  python kolom-hanya-ditulis.py
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import akar_repo  # noqa: E402  -- perlu sys.path di atas

MODEL = [
    os.path.join(akar_repo.AIS_SRC, 'ais', 'database', 'model', 'koperasi'),
    os.path.join(akar_repo.AIS_SRC, 'ais', 'database', 'model', 'inventory'),
]
DILEWATI = ('build', '.svn', 'node_modules', '.dart_tool', '.git', 'generated',
            'ephemeral', 'pods')

AKSESOR = re.compile(r'public\s+[\w<>\[\].]+\s+(get|is)([A-Z]\w*)\s*\(\s*\)')
MUTATOR = re.compile(r'public\s+void\s+set([A-Z]\w*)\s*\(')


def kumpulkan(akar, ekstensi, lewati_model=False):
    potongan = []
    for dp, dn, fn in os.walk(akar):
        dn[:] = [d for d in dn if d.lower() not in DILEWATI]
        if lewati_model and ('database' + os.sep + 'model') in dp:
            continue
        for n in fn:
            if n.lower().endswith(ekstensi):
                potongan.append(akar_repo.baca(os.path.join(dp, n)))
    return '\n'.join(potongan)


def ke_kolom(prop):
    return re.sub(r'([A-Z])', lambda m: '_' + m.group(1).lower(), prop)


def main():
    akar_repo.pastikan_lengkap(perlu_pos=True)
    pos = akar_repo.repo_pos()

    entitas = {}
    for akar in MODEL:
        if not os.path.isdir(akar):
            continue
        for dp, dn, fn in os.walk(akar):
            for n in fn:
                if not n.endswith('.java'):
                    continue
                t = akar_repo.baca(os.path.join(dp, n))
                entitas[n[:-5]] = (
                    set(m.group(1) + m.group(2) for m in AKSESOR.finditer(t)),
                    set('set' + m.group(1) for m in MUTATOR.finditer(t)))

    java = kumpulkan(akar_repo.AIS_SRC, ('.java',), lewati_model=True)
    web = kumpulkan(akar_repo.AIS_WEBAPP, ('.jsp', '.zul', '.js'))
    dart = kumpulkan(os.path.join(pos, 'apps'), ('.dart',))
    akar_repo.pastikan_terbaca()

    print('CAKUPAN  kelas entitas diperiksa : %d' % len(entitas))
    print('         java di luar model      : %d karakter' % len(java))
    print('         web                     : %d karakter' % len(web))
    print('         dart                    : %d karakter' % len(dart))

    mati = []
    for kelas in sorted(entitas):
        getters, setters = entitas[kelas]
        for g in sorted(getters):
            nama = g[3:] if g.startswith('get') else g[2:]
            if ('set' + nama) not in setters:
                continue
            if ('set' + nama + '(') not in java:
                continue
            if (g + '(') in java:
                continue
            prop = nama[:1].lower() + nama[1:]
            if prop in web or prop in dart:
                continue
            if ('"%s"' % prop) in java:
                continue
            kolom = ke_kolom(prop)
            if kolom in java or kolom in web:
                continue
            mati.append((prop, kolom, kelas))

    print('')
    print('== Dapat disimpan, tidak pernah dibaca: %d kandidat ==' % len(mati))
    print('   Ini KANDIDAT, bukan vonis -- baca empat jalur baca di kepala berkas.')
    print('')
    for prop, kolom, kelas in mati:
        print('   %-30s %-28s %s' % (prop, kolom, kelas))

    # Selalu 0: melapor, tidak memvonis.
    return 0


if __name__ == '__main__':
    sys.exit(main())
