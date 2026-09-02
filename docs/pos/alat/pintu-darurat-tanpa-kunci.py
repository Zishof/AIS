# -*- coding: utf-8 -*-
"""Pintu darurat yang dijaga penanda, padahal tak ada klien yang bisa mengirimnya.

Bentuk yang dikunci di sini datang dari cacat nyata (docs/pos/86):

    if (!request.optBoolean("izin_harga_modal_tinggi", false)
            && hargaJualFinal > 0.0 && hargaModalFinal > hargaJualFinal * 10.0) {
        hasil.put("status", "91");
        hasil.put("description", "... bila memang disengaja, simpan ulang dengan
                                   persetujuan harga modal tinggi.");
        return;
    }

Penjaganya benar dan perlu (insiden 19-08-2026: harga modal 5,66 miliar melawan
harga jual 7.500). Pesannya yang tidak: `izin_harga_modal_tinggi` tidak pernah
dikirim klien mana pun, sehingga perintah "simpan ulang dengan persetujuan"
mustahil dijalankan. Kasus yang sah tidak dapat disimpan sama sekali, dan
barisnya terparkir GAGAL di outbox.

Pesan galat adalah JANJI kepada pengguna. Berbeda dari kontrak antarkode, janji
itu tidak dikompilasi, tidak diuji, dan -- sampai berkas ini ada -- tidak dijaga
siapa pun.

MENGAPA INI BOLEH MENJADI GERBANG, sementara dok. 84 dan 86 tidak.
Kedua dokumen itu berakhir tanpa penjaga karena pengukurannya menyisakan
ratusan/puluhan kandidat yang bercampur panggilan pihak ketiga yang sah; gerbang
di atas dasar seperti itu akan menuduh yang tidak bersalah. Di sini himpunannya
TIGA. Setiap anggotanya dapat -- dan sudah -- diperiksa tangan satu per satu.

CAKUPAN, diukur bukan ditebak:

    222  pemanggilan optBoolean("k", false) di pohon servlet
     11  di antaranya dinegasikan langsung di dalam syarat
     41  melewati variabel lokal lebih dulu
      3  benar-benar menjaga sebuah PENOLAKAN -- itulah yang dijaga berkas ini

Kedua bentuk (langsung dan lewat variabel lokal) dijaring. Yang TIDAK dijaring:
penanda yang dirakit dinamis, penanda yang dibaca di berkas berbeda dari tempat
penolakannya, dan penolakan yang lebih dari ~1.200 karakter setelah negasinya.

Alat ini MELAPORKAN dan memvonis: keluar dengan kode 1 bila ada pintu yang tidak
dapat dibuka siapa pun.

SEBELUM mempercayai hasil BERSIH, buktikan alat ini bisa GAGAL: hapus sementara
baris `if (_izinHargaModalTinggi) 'izin_harga_modal_tinggi': true,` dari
produk_screen.dart, jalankan, pastikan ia menyebut pintunya, lalu kembalikan.

Pakai:  python pintu-darurat-tanpa-kunci.py
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import akar_repo  # noqa: E402  -- perlu sys.path di atas

DILEWATI = ('build', '.dart_tool', 'node_modules', '.git', '.svn', 'generated',
            'ephemeral', 'pods', 'test', 'integration_test')

# Bentuk 1: dinegasikan langsung di dalam syarat.
LANGSUNG = re.compile(r'!\s*[A-Za-z_][A-Za-z0-9_]*\.optBoolean\s*\(\s*'
                      r'"([A-Za-z_][A-Za-z0-9_]*)"\s*,\s*false\s*\)')
# Bentuk 2: singgah di variabel lokal, lalu dinegasikan.
LEWAT_VAR = re.compile(r'boolean\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*'
                       r'[A-Za-z_][A-Za-z0-9_]*\.optBoolean\s*\(\s*'
                       r'"([A-Za-z_][A-Za-z0-9_]*)"\s*,\s*false\s*\)')
# Penolakan bisnis: status galat yang dikembalikan ke klien.
TOLAK = re.compile(r'\.put\s*\(\s*"status"\s*,\s*"(9[0-9]|error)"')
PESAN = re.compile(r'"description"\s*,\s*"([^"]{20,240})')


def berkas(akar, ekstensi):
    hasil = []
    for dp, dn, fn in os.walk(akar):
        dn[:] = [d for d in dn if d.lower() not in DILEWATI]
        for n in fn:
            if n.lower().endswith(ekstensi):
                p = os.path.join(dp, n)
                hasil.append((p, akar_repo.baca(p)))
    return hasil


def ke_camel(s):
    b = s.split('_')
    return b[0] + ''.join(x[:1].upper() + x[1:] for x in b[1:])


def ke_snake(s):
    return re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s).lower()


def main():
    akar_repo.pastikan_lengkap(perlu_pos=True)
    pos = akar_repo.repo_pos()

    pintu = {}
    for p, t in berkas(akar_repo.AIS_SERVLET, ('.java',)):
        nama = os.path.basename(p)
        for m in LANGSUNG.finditer(t):
            jendela = t[m.end():m.end() + 1500]
            if TOLAK.search(jendela):
                pesan = PESAN.search(jendela)
                pintu.setdefault(m.group(1),
                                 (nama, pesan.group(1) if pesan else ''))
        for m in LEWAT_VAR.finditer(t):
            var, kunci = m.group(1), m.group(2)
            jendela = t[m.end():m.end() + 3000]
            neg = re.search(r'!\s*' + re.escape(var) + r'(?![A-Za-z0-9_])', jendela)
            if neg and TOLAK.search(jendela[neg.end():neg.end() + 1200]):
                pesan = PESAN.search(jendela[neg.end():])
                pintu.setdefault(kunci, (nama, pesan.group(1) if pesan else ''))

    klien = '\n'.join(t for _, t in (
        berkas(os.path.join(pos, 'apps'), ('.dart',))
        + berkas(os.path.join(pos, 'packages'), ('.dart',))
        + berkas(akar_repo.KANTIN_JSP, ('.jsp', '.js'))))
    akar_repo.pastikan_terbaca()

    print('pintu darurat yang menjaga penolakan : %d' % len(pintu))
    print('sumber klien terbaca                 : %d karakter' % len(klien))

    def dikirim(k):
        for v in set([k, ke_camel(k), ke_snake(k)]):
            if ('"%s"' % v) in klien or ("'%s'" % v) in klien:
                return True
        return False

    buntu = [k for k in sorted(pintu) if not dikirim(k)]

    print('')
    print('== Pintu yang TIDAK DAPAT dibuka klien mana pun ==')
    if buntu:
        for k in buntu:
            nama, pesan = pintu[k]
            print('   - %s   (%s)' % (k, nama))
            if pesan:
                print('     pesan ke pengguna: "%s..."' % pesan[:120])
    else:
        print('   (tidak ada) -- inilah yang dijaga alat ini')

    print('')
    print('== Pintu yang memang dapat dibuka: %d ==' % (len(pintu) - len(buntu)))
    for k in sorted(pintu):
        if dikirim(k):
            print('   %-32s %s' % (k, pintu[k][0]))

    print('')
    if buntu:
        print('%d PINTU BUNTU -- pesannya menjanjikan yang mustahil' % len(buntu))
        return 1
    print('SELURUH PINTU DAPAT DIBUKA')
    return 0


if __name__ == '__main__':
    sys.exit(main())
