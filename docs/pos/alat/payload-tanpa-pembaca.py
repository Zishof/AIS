# -*- coding: utf-8 -*-
"""Cari kunci payload yang DIKIRIM klien tetapi tidak pernah dibaca server.

Pasangan arah sebaliknya dari field-tanpa-pembaca.py. Arah inilah tempat cacat
PERTAMA dari seluruh rangkaian ini berada -- docs/pos/45: klien sudah lama
mengirim `metodeBayar` pada payload dasbor, dan PosApi.prosesDashboardUmum tidak
pernah membacanya. Chip "Jenis pembayaran" menyala, tabelnya tetap bercampur.

Yang dicari: kunci pada literal payload klien -- Dart `'kunci':` di dalam
`aksi(...)`, JavaScript `kunci:` di dalam `fetchDataAPI(...)` -- lalu nama itu
dicari sebagai literal "kunci" di sumber server.

Tiga jebakan yang sudah memakan korban saat alat ini dibuat, semuanya sudah
ditutup di sini:

  1. `aksi(` sebagai substring juga cocok dengan `transaksi(` dan `koreksi(`,
     sehingga map yang sama sekali bukan payload ikut terjaring.
  2. `aksi('x')` tanpa map membuat pencarian melompat ke kurung milik badan
     fungsi berikutnya lalu menelan isinya. Kurung buka wajib DEKAT dan seimbang.
  3. Pembacaan dicari lewat regex pengakses (`optString("x"`) menuduh `induk_id`
     yatim -- padahal ia dibaca `Common.angkaAtauNull(request, "induk_id")`.
     Karena itu yang dicari adalah literalnya, bukan nama method-nya.

Batas yang disadari: alat ini bertanya "dibaca di mana pun?", bukan "dibaca oleh
handler aksinya". Kunci yang dibaca handler LAIN akan lolos. Memetakan aksi ke
handler menuntut penguraian yang rapuh, dan salah petakan menghasilkan tuduhan
palsu. Payload yang dirakit dinamis (`payload['x'] = ...`) juga tidak terjaring.

Alat ini MELAPORKAN saja, tidak menyunting apa pun.

Pakai:  python payload-tanpa-pembaca.py
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import akar_repo  # noqa: E402  -- perlu sys.path di atas

AKAR_REPO_POS = akar_repo.repo_pos()

AKAR_KLIEN_DART = os.path.join(AKAR_REPO_POS, 'apps', 'ebisnis', 'lib')
AKAR_KLIEN_JSP = akar_repo.KANTIN_JSP
AKAR_SERVER = akar_repo.AIS_SERVLET

DILEWATI = {'build', '.dart_tool', 'node_modules', '.git', 'generated', 'ephemeral',
            'pods', 'test', 'integration_test'}

DIIZINKAN = {
    'action': 'nama aksinya sendiri, dibaca dispatcher',
    'sql': 'endpoint SQL generik',
    'class': 'dipakai CRUD generik',
    'tanpaLoading': 'penanda UI murni, tidak dikirim ke logika server',
    'offline': ('kunci RESPONS yang dirakit klien untuk dirinya sendiri '
                '(master_offline/outbox_is), bukan payload ke server'),
}

UTANG = {
    # impor_excel_produk_screen.dart:164 -> aksi "produk_impor_excel_komit".
    # KantinHelper.produkImporExcelKomit hanya membaca `toko_id` dan `baris`.
    #
    # SUDAH DITELUSURI, dan sengaja BELUM dibayar -- keduanya bukan korupsi data:
    #
    #  * nomor_batch_klien: klien mencoba ulang s.d. 5x saat jaringan putus,
    #    sehingga satu batch bisa terkirim dua kali. AMAN karena pencocokan produk
    #    berlapis empat (kode/barcode/nama/kunci unik) membuat kiriman ulang
    #    MEMPERBARUI baris yang sama, bukan menggandakan. Yang hilang hanya
    #    keterlacakan batch di log server.
    #
    #  * hanya_perubahan: server memproses semua baris, bukan hanya yang berubah.
    #    TIDAK menimbulkan jurnal opname palsu (dijaga `if (selisih != 0)`).
    #    Akibatnya dua: hitungan "diperbarui" pada ringkasan impor ikut menghitung
    #    baris yang tidak berubah, dan tiap baris tetap menahan lock
    #    koperasi.produk sehingga memperbesar permukaan deadlock yang sudah punya
    #    mekanisme percobaan-ulang tersendiri.
    #
    # Belum dibayar karena memutuskan "apa artinya tidak berubah" adalah keputusan
    # tersendiri: perbandingan field yang terlalu longgar akan MELEWATI perubahan
    # yang sah, dan itu kehilangan data yang sunyi.
    'hanya_perubahan',
    'nomor_batch_klien',
}

PANGGIL_DART = re.compile(r'(?<![A-Za-z])aksi\s*\(')
PANGGIL_JS = re.compile(r'(?<![A-Za-z])fetchDataAPI[A-Za-z0-9_]*\s*\(')
KUNCI_DART = re.compile(r"'([A-Za-z_][A-Za-z0-9_]*)'\s*:")
KUNCI_JS = re.compile(r'(?m)^\s*([A-Za-z_][A-Za-z0-9_]*)\s*:')


def kumpulkan(akar, ekstensi):
    potongan = []
    for dirpath, dirnames, filenames in os.walk(akar):
        dirnames[:] = [d for d in dirnames if d.lower() not in DILEWATI]
        for nama in filenames:
            if nama.lower().endswith(ekstensi):
                potongan.append(akar_repo.baca(os.path.join(dirpath, nama)))
    return '\n'.join(potongan)


def potong_payload(sumber, pola_panggil):
    """Isi map payload sesudah tiap pemanggilan, dengan hitung kurung."""
    hasil = []
    for m in pola_panggil.finditer(sumber):
        buka = sumber.find('{', m.end() - 1)
        # Kurung buka wajib DEKAT: pemanggilan tanpa map tidak boleh membuat
        # pencarian melompat ke badan fungsi berikutnya (jebakan no. 2).
        if buka < 0 or buka - m.end() > 120:
            continue
        dalam = 0
        seimbang = False
        j = buka
        batas = min(len(sumber), buka + 4000)
        while j < batas:
            c = sumber[j]
            if c == '{':
                dalam += 1
            elif c == '}':
                dalam -= 1
                if dalam == 0:
                    seimbang = True
                    break
            j += 1
        # Kurung tak seimbang dalam jendela = penguraian meleset. Lebih baik
        # melewatkan satu payload daripada memasukkan teks yang bukan payload.
        if seimbang:
            hasil.append(sumber[buka:j + 1])
    return '\n'.join(hasil)


def main():
    akar_repo.pastikan_lengkap(perlu_pos=True)
    for akar in (AKAR_KLIEN_DART, AKAR_KLIEN_JSP, AKAR_SERVER):
        if not os.path.isdir(akar):
            print('AKAR TIDAK KETEMU: ' + akar)
            return 1

    dart = kumpulkan(AKAR_KLIEN_DART, ('.dart',))
    jsp = kumpulkan(AKAR_KLIEN_JSP, ('.jsp', '.js'))
    server = kumpulkan(AKAR_SERVER, ('.java',))

    dikirim = []
    for nama in KUNCI_DART.findall(potong_payload(dart, PANGGIL_DART)):
        if nama not in dikirim:
            dikirim.append(nama)
    for nama in KUNCI_JS.findall(potong_payload(jsp, PANGGIL_JS)):
        if nama not in dikirim:
            dikirim.append(nama)

    akar_repo.pastikan_terbaca()
    print('kunci payload yang dikirim klien : %d' % len(dikirim))
    print('sumber server terbaca            : %d karakter' % len(server))

    yatim = [k for k in dikirim
             if k not in DIIZINKAN and ('"%s"' % k) not in server]
    baru = [k for k in yatim if k not in UTANG]
    basi = sorted(u for u in UTANG if u not in yatim)

    print('')
    print('== Utang lama (dibekukan): %d ==' % (len(yatim) - len(baru)))
    print('== Kunci BARU yang dikirim klien tanpa pembaca di server ==')
    if baru:
        for k in baru:
            print('   - ' + k)
    else:
        print('   (tidak ada) -- inilah yang dijaga alat ini')

    print('')
    print('== Utang yang sudah LUNAS tetapi masih terdaftar ==')
    if basi:
        for k in basi:
            print('   - %s  <- keluarkan dari daftar UTANG' % k)
    else:
        print('   (tidak ada)')

    print('')
    if baru or basi:
        print('PERIKSA LAGI: %d kunci baru, %d utang basi' % (len(baru), len(basi)))
        return 1
    print('BERSIH')
    return 0


if __name__ == '__main__':
    sys.exit(main())
