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
palsu.

Bentuk `payload['x'] = ...` dulu juga tidak terjaring; sekarang terjaring lewat
kunci_ditempel() (docs/pos/85). Yang MASIH lolos: penggabungan map utuh
(`payload.addAll(petaLain)`) dan kunci yang bukan literal (`payload[v] = ...`).

Alat ini MELAPORKAN saja, tidak menyunting apa pun.

Pakai:  python payload-tanpa-pembaca.py
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import akar_repo  # noqa: E402  -- perlu sys.path di atas

AKAR_REPO_POS = akar_repo.repo_pos()

# Seluruh apps + packages, bukan satu aplikasi saja.
#
# Sampai docs/pos/93 akar ini adalah `apps/ebisnis/lib`, sehingga apps/ecanteen
# -- aplikasi kedua, 12 berkas memanggil aksi() -- dan kesembilan packages/core_*
# tidak pernah diperiksa. Dua alat sibling di direktori ini sudah memindai
# apps+packages penuh; hanya berkas ini yang tertinggal, dan tidak ada yang
# menyatakannya. Pelebarannya menjaring 2 kunci baru, keduanya ternyata dibaca
# server, jadi tidak ada cacat yang selama ini tersembunyi -- tetapi 40 berkas
# memang tak pernah terlihat.
AKAR_KLIEN_DART = [
    os.path.join(AKAR_REPO_POS, 'apps'),
    os.path.join(AKAR_REPO_POS, 'packages'),
]
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
    #
    #    DIKOREKSI (docs/pos/102). Semula tertulis di sini bahwa "tiap baris tetap
    #    menahan lock koperasi.produk sehingga memperbesar permukaan deadlock".
    #    Itu SALAH. Produk diambil lewat session.createCriteria(...).list() sehingga
    #    berstatus managed; Hibernate dirty-check saat flush hanya menerbitkan
    #    UPDATE untuk entitas yang nilainya benar-benar berubah. Method impor tidak
    #    menyetel satu pun timestamp/audit tanpa syarat, dan
    #    AuditTimestampInterceptor hanya bereaksi lewat onFlushDirty -- ia tidak
    #    dapat mengotori entitas yang bersih. Baris yang nilainya sama karena itu
    #    tidak menghasilkan UPDATE dan tidak mengambil lock.
    #
    #    Akibat yang benar-benar tersisa TINGGAL SATU: hitungan "diperbarui" pada
    #    ringkasan impor ikut menghitung baris yang tidak berubah. Itu laporan yang
    #    keliru, bukan risiko deadlock.
    #
    # "Apa artinya tidak berubah" pun ternyata bukan pertanyaan terbuka: yang
    # ditulis impor ini hanya sepuluh field (kode, nama, barcode, hargaBeli,
    # hargaJual, jenisProduk, pemasok, satuan, toko, kunciUnik). Perbandingan yang
    # mencakup persis field-field itu tidak mungkin MELEWATI perubahan yang sah,
    # karena tidak ada field lain yang ditulis.
    'hanya_perubahan',
    'nomor_batch_klien',
}

PANGGIL_DART = re.compile(r'(?<![A-Za-z])aksi\s*\(')
# Variabel yang diserahkan apa adanya ke aksi(): `aksi('bayar', payload)`.
VAR_DART = re.compile(
    r"(?<![A-Za-z])aksi\s*\(\s*'[^']*'\s*,\s*([A-Za-z_][A-Za-z0-9_]*)\s*[,)]")
PANGGIL_JS = re.compile(r'(?<![A-Za-z])fetchDataAPI[A-Za-z0-9_]*\s*\(')
KUNCI_DART = re.compile(r"'([A-Za-z_][A-Za-z0-9_]*)'\s*:")
KUNCI_JS = re.compile(r'(?m)^\s*([A-Za-z_][A-Za-z0-9_]*)\s*:')


def berkas_demi_berkas(akar, ekstensi):
    """(jalur, isi) per berkas -- perlu untuk analisis yang tidak boleh lintas berkas."""
    hasil = []
    for dirpath, dirnames, filenames in os.walk(akar):
        dirnames[:] = [d for d in dirnames if d.lower() not in DILEWATI]
        for nama in filenames:
            if nama.lower().endswith(ekstensi):
                p = os.path.join(dirpath, nama)
                hasil.append((p, akar_repo.baca(p)))
    return hasil


def kumpulkan(akar, ekstensi):
    return '\n'.join(t for _, t in berkas_demi_berkas(akar, ekstensi))


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


def kunci_ditempel(daftar_berkas):
    """Kunci yang ditempel SESUDAH payload dirakit: `payload['x'] = ...`.

    Titik buta yang dulu ditulis terus terang di kepala berkas ini, lalu ditutup
    setelah contohnya benar-benar ditemukan: riwayat_penjualan_screen.dart
    menempelkan input_supervisor, alasan_supervisor, dan kasir_user_id pada
    payload hasil pemutaran ulang, jauh dari literal mana pun. Ketiganya ternyata
    dibaca server -- tetapi alat ini tidak akan pernah tahu, dan itu justru
    persoalannya: titik buta tidak memberi tahu kapan ia menutupi sesuatu.

    Dibatasi PER BERKAS dan hanya pada variabel yang benar-benar diserahkan ke
    `aksi(...)`. Mencocokkan sembarang `map['x'] =` akan menjaring setiap map di
    aplikasi dan mengubah alat ini menjadi mesin tuduhan palsu.
    """
    hasil = []
    for _, teks in daftar_berkas:
        nama_var = set(VAR_DART.findall(teks))
        for v in nama_var:
            pola = re.compile(r'(?<![A-Za-z0-9_])' + re.escape(v) +
                              r"\[\s*'([A-Za-z_][A-Za-z0-9_]*)'\s*\]\s*=(?!=)")
            for k in pola.findall(teks):
                if k not in hasil:
                    hasil.append(k)
    return hasil


def main():
    akar_repo.pastikan_lengkap(perlu_pos=True)
    for akar in AKAR_KLIEN_DART + [AKAR_KLIEN_JSP, AKAR_SERVER]:
        if not os.path.isdir(akar):
            print('AKAR TIDAK KETEMU: ' + akar)
            return 1

    berkas_dart = []
    for akar in AKAR_KLIEN_DART:
        berkas_dart += berkas_demi_berkas(akar, ('.dart',))
    dart = '\n'.join(t for _, t in berkas_dart)
    jsp = kumpulkan(AKAR_KLIEN_JSP, ('.jsp', '.js'))
    server = kumpulkan(AKAR_SERVER, ('.java',))

    dikirim = []
    for nama in KUNCI_DART.findall(potong_payload(dart, PANGGIL_DART)):
        if nama not in dikirim:
            dikirim.append(nama)
    for nama in KUNCI_JS.findall(potong_payload(jsp, PANGGIL_JS)):
        if nama not in dikirim:
            dikirim.append(nama)
    ditempel = kunci_ditempel(berkas_dart)
    for nama in ditempel:
        if nama not in dikirim:
            dikirim.append(nama)

    akar_repo.pastikan_terbaca()
    print('CAKUPAN  berkas Dart dipindai    : %d' % len(berkas_dart))
    print('         JSP dipindai            : modul/kantin saja -- JSP lain')
    print('                                   memanggil /Data action=sql, bukan PosApi')
    print('kunci payload yang dikirim klien : %d' % len(dikirim))
    print('   di antaranya ditempel dinamis : %d' % len(ditempel))
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
