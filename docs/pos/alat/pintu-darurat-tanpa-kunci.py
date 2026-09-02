# -*- coding: utf-8 -*-
"""Pintu darurat yang dijaga sebuah syarat, padahal tak ada klien yang bisa membukanya.

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

KEDEKATAN BUKAN SEBAB-AKIBAT (docs/pos/88).
Versi pertama berkas ini menyatakan sebuah syarat "menjaga penolakan" bila ada
`status 9x` dalam ~1.500 karakter sesudahnya. Itu keliru dua arah:

  * `termasuk_nonaktif` ikut terhitung, padahal ia menjaga sebuah FILTER
    (`c.add(Restrictions.eq("aktif", TRUE))`), bukan penolakan.
  * Pada pintu bernilai, `brandId` dituduh buntu padahal pemeriksaan kosongnya
    ada di dalam ternary dan penolakan di dekatnya milik kondisi yang lain
    (`if (brandId != null)` -> "Brand bukan milik Anda").

Sekarang syaratnya tegas: pemeriksaannya harus berada di dalam KONDISI sebuah
`if`, dan penolakannya harus berada di dalam BADAN `if` itu -- keduanya
ditentukan dengan mencocokkan kurung, bukan menghitung jarak.

MENGAPA INI BOLEH MENJADI GERBANG, sementara dok. 84 dan 86 tidak.
Kedua dokumen itu berakhir tanpa penjaga karena pengukurannya menyisakan
ratusan/puluhan kandidat yang bercampur panggilan pihak ketiga yang sah; gerbang
di atas dasar seperti itu akan menuduh yang tidak bersalah. Di sini himpunannya
TIGA. Setiap anggotanya dapat -- dan sudah -- diperiksa tangan satu per satu.

CAKUPAN, diukur bukan ditebak:

    10.828  blok `if (...) { ... }` diurai di pohon servlet
       222  pemanggilan optBoolean("k", false)
         2  penanda boolean yang benar-benar menjaga penolakan
         1  pemeriksaan nilai kosong yang benar-benar menjaga penolakan

Tiga bentuk dijaring: penanda boolean yang dinegasikan langsung di kondisi,
penanda boolean yang singgah di variabel lokal lebih dulu, dan pemeriksaan
"nilai kosong" atas teks. Yang TIDAK dijaring: syarat yang dirakit dinamis,
syarat yang dibaca di berkas berbeda dari tempat penolakannya, pintu yang dibuka
oleh peran pengguna, dan penolakan di dalam blok `else`.

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

# Penolakan bisnis: status galat yang dikembalikan ke klien.
TOLAK = re.compile(r'\.put\s*\(\s*"status"\s*,\s*"(9[0-9]|error)"')
PESAN = re.compile(r'"description"\s*,\s*"([^"]{20,240})')

# Bentuk 1: penanda boolean dinegasikan langsung di dalam kondisi.
BOOL_LANGSUNG = re.compile(r'!\s*[A-Za-z_][A-Za-z0-9_]*\.optBoolean\s*\(\s*'
                           r'"([A-Za-z_][A-Za-z0-9_]*)"\s*,\s*false\s*\)')
# Bentuk 2: penanda boolean singgah di variabel lokal lebih dulu.
BOOL_VAR = re.compile(r'boolean\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*'
                      r'[A-Za-z_][A-Za-z0-9_]*\.optBoolean\s*\(\s*'
                      r'"([A-Za-z_][A-Za-z0-9_]*)"\s*,\s*false\s*\)')
# Bentuk 3: nilai teks yang kosong/terlalu pendek.
#
# Lookbehind (?<![!\w]) berada tepat sebelum NAMA PENERIMA, bukan sebelum
# .optString: negasinya ditulis !request.optString(...). Tanpa itu,
# `!isNull(k) && !optString(k).isEmpty()` -- yang justru jalur "nilainya ADA" --
# ikut terjaring, dan tiga kunci yang sah pernah dituduh buntu karenanya.
NILAI_KOSONG = re.compile(r'(?<![!\w])[A-Za-z_]\w*\.optString\s*\(\s*'
                          r'"([A-Za-z_][A-Za-z0-9_]*)"[^;{)]{0,40}\)'
                          r'\s*(?:\.trim\(\)\s*)?'
                          r'(?:\.isEmpty\(\)|\.length\(\)\s*[<=]=?\s*\d+)')


def berkas(akar, ekstensi):
    hasil = []
    for dp, dn, fn in os.walk(akar):
        dn[:] = [d for d in dn if d.lower() not in DILEWATI]
        for n in fn:
            if n.lower().endswith(ekstensi):
                p = os.path.join(dp, n)
                hasil.append((p, akar_repo.baca(p)))
    return hasil


def blok_if(teks):
    """(kondisi, badan) untuk setiap `if (...) { ... }` yang kurungnya seimbang."""
    hasil = []
    for m in re.finditer(r'(?<![A-Za-z0-9_])if\s*\(', teks):
        i = m.end() - 1
        dalam, j = 0, i
        while j < len(teks):
            if teks[j] == '(':
                dalam += 1
            elif teks[j] == ')':
                dalam -= 1
                if dalam == 0:
                    break
            j += 1
        if j >= len(teks):
            continue
        kondisi = teks[i + 1:j]
        k = j + 1
        while k < len(teks) and teks[k] in ' \t\r\n':
            k += 1
        if k >= len(teks) or teks[k] != '{':
            continue
        dalam, n = 0, k
        while n < len(teks):
            if teks[n] == '{':
                dalam += 1
            elif teks[n] == '}':
                dalam -= 1
                if dalam == 0:
                    break
            n += 1
        hasil.append((kondisi, teks[k:n + 1]))
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
    n_blok = 0
    for p, t in berkas(akar_repo.AIS_SERVLET, ('.java',)):
        nama = os.path.basename(p)
        # variabel lokal -> kunci, supaya `!v` di kondisi tetap tertelusuri
        var_ke_kunci = {}
        for m in BOOL_VAR.finditer(t):
            var_ke_kunci[m.group(1)] = m.group(2)

        for kondisi, badan in blok_if(t):
            n_blok += 1
            if not TOLAK.search(badan):
                continue
            pesan = PESAN.search(badan)
            pesan = pesan.group(1) if pesan else ''
            for m in BOOL_LANGSUNG.finditer(kondisi):
                pintu.setdefault(m.group(1), (nama, 'penanda', pesan))
            for m in NILAI_KOSONG.finditer(kondisi):
                pintu.setdefault(m.group(1), (nama, 'nilai', pesan))
            for var, kunci in var_ke_kunci.items():
                if re.search(r'!\s*' + re.escape(var) + r'(?![A-Za-z0-9_])', kondisi):
                    pintu.setdefault(kunci, (nama, 'penanda', pesan))

    klien = '\n'.join(t for _, t in (
        berkas(os.path.join(pos, 'apps'), ('.dart',))
        + berkas(os.path.join(pos, 'packages'), ('.dart',))
        + berkas(akar_repo.KANTIN_JSP, ('.jsp', '.js'))))
    akar_repo.pastikan_terbaca()

    print('blok if diurai                     : %d' % n_blok)
    print('pintu darurat yang menjaga penolakan: %d' % len(pintu))
    print('sumber klien terbaca               : %d karakter' % len(klien))

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
            nama, bentuk, pesan = pintu[k]
            print('   - %s   (%s, %s)' % (k, bentuk, nama))
            if pesan:
                print('     pesan ke pengguna: "%s..."' % pesan[:120])
    else:
        print('   (tidak ada) -- inilah yang dijaga alat ini')

    print('')
    print('== Pintu yang memang dapat dibuka: %d ==' % (len(pintu) - len(buntu)))
    for k in sorted(pintu):
        if dikirim(k):
            print('   %-32s %-9s %s' % (k, pintu[k][1], pintu[k][0]))

    print('')
    if buntu:
        print('%d PINTU BUNTU -- pesannya menjanjikan yang mustahil' % len(buntu))
        return 1
    print('SELURUH PINTU DAPAT DIBUKA')
    return 0


if __name__ == '__main__':
    sys.exit(main())
