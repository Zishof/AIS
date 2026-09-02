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


# Method yang memuat pintunya, lalu aksi dispatcher yang memanggil method itu.
# Dipakai untuk pertanyaan PER JALUR: bukan "adakah klien yang mengirim kuncinya?"
# melainkan "apakah SETIAP layar yang bisa menabrak pintu ini juga bisa
# membukanya?" -- lihat docs/pos/91.
#
# Perbedaannya nyata: izin_harga_modal_tinggi dikirim produk_screen.dart, sehingga
# pertanyaan lama menjawab "dapat dibuka". Tetapi kulakan_bulk_entry_screen.dart
# juga memanggil produk_simpan dengan harga_beli sungguhan, dan di sana pengguna
# tetap buntu.
METHOD = re.compile(r'static\s+(?:void|boolean|String)\s+([a-zA-Z][A-Za-z0-9_]*)\s*\(')
AKSI_DISPATCH = re.compile(
    r'"([a-z_]+)"\.equals\(action\)[^{]*\{\s*[A-Za-z0-9_]+\.([a-zA-Z][A-Za-z0-9_]*)\s*\(')

# Jalur klien yang SUDAH ditelusuri dan sengaja dibiarkan buntu, beserta sebabnya.
# Daftar ini hanya boleh MENYUSUT.
UTANG_JALUR = {
    ('izin_harga_modal_tinggi', 'kulakan_bulk_entry_screen.dart'):
        'entri massal -- satu persetujuan untuk seluruh batch atau per baris '
        'adalah keputusan tersendiri; menebaknya di jalur yang menulis banyak '
        'baris sekaligus lebih berbahaya daripada membiarkannya (dok. 86 par.5)',
}


# Jalur yang menyebut nama aksinya tetapi TERBUKTI tidak dapat memicu pintunya.
# Berbeda dari UTANG_JALUR: ini bukan pekerjaan yang ditunda, melainkan tuduhan
# yang salah. Tiap baris menyebut ALASAN yang dapat diperiksa ulang, karena
# pembebasan tanpa alasan adalah cara termudah membuat sebuah penjaga bohong.
TAK_MEMICU = {
    ('izin_harga_modal_tinggi', 'kasir_screen.dart'):
        "tambah produk cepat mengirim 'harga_beli': 0 yang dipatok, sehingga "
        "syarat hargaModal > hargaJual * 10 tidak pernah dapat terpenuhi",
    ('izin_harga_modal_tinggi', 'core_db.dart'):
        "nama aksinya hanya dipakai sebagai filter SQL where atas tabel "
        "outbox_master saat memulihkan cache; berkas ini tidak pernah mengirim "
        "permintaan apa pun",
}


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


def method_pemuat(teks, kunci):
    """Nama method yang memuat kemunculan pertama `kunci` -- METHOD terakhir sebelumnya."""
    i = teks.find('"' + kunci + '"')
    if i < 0:
        return None
    nama = None
    for m in METHOD.finditer(teks, 0, i):
        nama = m.group(1)
    return nama


def aksi_untuk(dispatcher, nama_method):
    """Nama aksi dispatcher yang memanggil method itu."""
    hasil = []
    for m in AKSI_DISPATCH.finditer(dispatcher):
        if m.group(2) == nama_method:
            hasil.append(m.group(1))
    return hasil


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

    klien_berkas = (
        berkas(os.path.join(pos, 'apps'), ('.dart',))
        + berkas(os.path.join(pos, 'packages'), ('.dart',))
        + berkas(akar_repo.KANTIN_JSP, ('.jsp', '.js')))
    klien = '\n'.join(t for _, t in klien_berkas)
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


    # --- Per jalur: setiap layar yang bisa MENABRAK pintu harus bisa membukanya.
    #
    # Pertanyaan lama ("adakah klien yang mengirim kuncinya?") terlalu longgar:
    # ia menjawab "dapat dibuka" untuk izin_harga_modal_tinggi karena
    # produk_screen.dart mengirimnya, padahal pengguna di kulakan_bulk_entry
    # tetap buntu.
    dispatcher = akar_repo.baca(os.path.join(akar_repo.AIS_SERVLET, 'PosApi.java'))
    per_berkas = {}
    for jalur, isi in klien_berkas:
        per_berkas[os.path.basename(jalur)] = isi

    buntu_jalur, utang_terpakai, bebas_terpakai = [], [], []
    for k in sorted(pintu):
        nama_method = None
        for jalur, isi in berkas(akar_repo.AIS_SERVLET, ('.java',)):
            if os.path.basename(jalur) == pintu[k][0]:
                nama_method = method_pemuat(isi, k)
                break
        if not nama_method:
            continue
        for aksi in aksi_untuk(dispatcher, nama_method):
            for nama_berkas, isi in per_berkas.items():
                if ("'%s'" % aksi) not in isi and ('"%s"' % aksi) not in isi:
                    continue
                if ("'%s'" % k) in isi or ('"%s"' % k) in isi:
                    continue
                if (k, nama_berkas) in TAK_MEMICU:
                    bebas_terpakai.append((k, nama_berkas))
                elif (k, nama_berkas) in UTANG_JALUR:
                    utang_terpakai.append((k, nama_berkas))
                else:
                    buntu_jalur.append((k, nama_berkas, aksi))

    print('')
    print('== Jalur klien yang bisa menabrak pintu tetapi tidak bisa membukanya ==')
    if buntu_jalur:
        for k, f, a in buntu_jalur:
            print('   - %s  lewat aksi %s, tanpa %s' % (f, a, k))
    else:
        print('   (tidak ada yang baru)')
    if utang_terpakai:
        print('')
        print('   Utang jalur yang sudah ditelusuri (dibekukan):')
        for k, f in utang_terpakai:
            print('   * %s / %s' % (f, k))
            print('     %s' % UTANG_JALUR[(k, f)])
    if bebas_terpakai:
        print('')
        print('   Jalur yang terbukti tidak dapat memicu (dibebaskan):')
        for k, f in bebas_terpakai:
            print('   * %s / %s' % (f, k))
            print('     %s' % TAK_MEMICU[(k, f)])
    basi = ([x for x in UTANG_JALUR if x not in utang_terpakai]
            + [x for x in TAK_MEMICU if x not in bebas_terpakai])
    if basi:
        print('')
        print('   Entri daftar yang sudah tidak berlaku:')
        for k, f in basi:
            print('   * %s / %s  <- keluarkan dari daftarnya' % (f, k))

    print('')
    if buntu or buntu_jalur:
        if buntu:
            print('%d PINTU BUNTU -- pesannya menjanjikan yang mustahil' % len(buntu))
        if buntu_jalur:
            print('%d JALUR BUNTU -- layarnya bisa menabrak, tidak bisa membuka'
                  % len(buntu_jalur))
        return 1
    print('SELURUH PINTU DAPAT DIBUKA DARI SETIAP JALURNYA')
    return 0


if __name__ == '__main__':
    sys.exit(main())
