# -*- coding: utf-8 -*-
"""Jaga bentuk aturan tiga-nilai `izinkan_jual_minus_stok` di KantinHelper.

Regresi r77493 (16-08-2026 s.d. 02-09-2026) berupa SATU perbandingan boolean:
syarat blokirnya ditulis inline sebagai `!Boolean.TRUE.equals(override)`, yang
menyamakan `null` ("Ikut Pengaturan Toko", default) dengan `FALSE` ("Wajib
Diblokir"). Akibatnya seluruh produk yang belum pernah disetel admin ikut
diblokir keras, dan penolakannya menyebut "produk yang dikunci admin" untuk
produk yang tidak pernah dikunci siapa pun. Lihat docs/pos/73 dan 77.

Alat ini menolak BENTUK yang menyebabkannya kembali. Ia menjaga sumber, bukan
perilaku -- uji perilakunya ada di src/test/java/.../StokMinusTigaNilaiUat.java,
tetapi direktori itu TIDAK berada di bawah SVN (lihat docs/pos/82), sehingga
alat inilah satu-satunya penjaga aturan tsb yang benar-benar terversi.

Kolom bertipe Boolean yang bermakna TIGA nilai adalah jebakan berulang: `null`
tampak "kosong" sehingga naluri pertama menulisnya sebagai `!Boolean.TRUE.equals`
-- dan itu diam-diam memindahkan seluruh default ke sisi yang salah, tanpa galat
kompilasi dan tanpa uji merah.

Alat ini MELAPORKAN saja. Keluar dengan kode 1 bila bentuknya menyimpang.

Pakai:  python aturan-stok-tiga-nilai.py [KantinHelper.java]
"""
import os
import re
import sys

BAWAAN = (r'C:\opt\AIS\ais\src\main\src\ais\action\servlet\api\KantinHelper.java')

gagal = []


def cek(kondisi, pesan):
    print(('  OK    ' if kondisi else '  GAGAL ') + pesan)
    if not kondisi:
        gagal.append(pesan)


def main():
    src = sys.argv[1] if len(sys.argv) > 1 else BAWAAN
    if not os.path.isfile(src):
        print('sumber tidak ketemu: ' + src)
        return 1
    with open(src, 'rb') as f:
        t = f.read().decode('utf-8', 'replace').replace('\r\n', '\n')

    print('== Aturan berdiri sendiri, bukan ditulis inline di tengah kueri ==')
    i = t.find('static boolean wajibDiblokirKarenaStok(')
    cek(i >= 0, 'method wajibDiblokirKarenaStok ada')
    if i < 0:
        return 1
    akhir = t.index('\n\t}', i)
    badan = t[i:akhir]

    print('')
    print('== Ketiga nilai diperlakukan BERBEDA ==')
    cek('Boolean.FALSE.equals(izinkanJualMinusStok)' in badan,
        'FALSE "Wajib Diblokir" diperiksa eksplisit -> memblokir')
    cek('Boolean.TRUE.equals(izinkanJualMinusStok)' in badan,
        'TRUE "Selalu Boleh Dijual" diperiksa eksplisit -> tidak memblokir')
    cek('return cegahOversellAktif;' in badan,
        'null "Ikut Pengaturan" mengikuti sakelar, bukan diperlakukan sbg FALSE')

    print('')
    print('== Bentuk yang MENYEBABKAN regresi r77493 tidak boleh kembali ==')
    # Inilah inti alat ini. `!Boolean.TRUE.equals(x)` benar untuk pertanyaan
    # "bukan TRUE?", tetapi SALAH sebagai syarat blokir: ia menelan null.
    cek('!Boolean.TRUE.equals(izinkanJualMinusStok)' not in badan,
        'syarat blokir TIDAK memakai !Boolean.TRUE.equals(...) yang menelan null')
    cek(not re.search(r'!\s*Boolean\.TRUE\.equals', badan),
        'tidak ada varian berspasi dari bentuk itu')

    print('')
    print('== Gerbang konfigurasi benar-benar dibaca (dok. 77) ==')
    cek('cegahOversellAktif' in t.split('wajibDiblokirKarenaStok')[0]
        or 'boolean cegahOversellAktif' in badan,
        'aturan menerima sakelar sbg parameter, bukan mengabaikannya')
    cek('KANTIN_POS_CEGAH_OVERSELL' in t,
        'sakelar dibaca di berkas ini -- JavaDoc-nya dulu menjanjikan tanpa membacanya')
    cek('Konfigurasi.TIDAK_AKTIF' in t,
        'defaultnya MATI, sesuai label layar Konfigurasi')

    print('')
    if gagal:
        print('%d BENTUK MENYIMPANG' % len(gagal))
        return 1
    print('BENTUK ATURAN UTUH (9 periksaan)')
    return 0


if __name__ == '__main__':
    sys.exit(main())
