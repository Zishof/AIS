# -*- coding: utf-8 -*-
"""Cari field respons yang DIKIRIM server tetapi tidak dibaca kanal mana pun.

Cacat berbentuk ini sudah berulang lima kali dengan wajah berbeda -- lihat
docs/pos/45, 46, 75, 76. Tidak satu pun menghasilkan galat kompilasi maupun uji
merah: sistem ini punya tiga kanal (JSP, POS Desktop/Android, ZK), dan
menyambungkan field baru di satu kanal terasa seperti selesai.

Yang dicari: `hasil.put("nama")` pada helper API, lalu nama itu dicari di seluruh
sumber kanal klien DAN di lapisan servlet server. Rantainya bisa
server -> server -> klien; penjaga yang hanya melihat ujung terakhir akan menuduh
yang tidak bersalah (itu terjadi: 37 tuduhan, 22 di antaranya palsu).

Pembacaan sengaja dicari sebagai LITERAL "nama", bukan lewat daftar nama method
pengakses. Selama helper seperti `Common.angkaAtauNull(request, "kunci")` boleh
ditulis siapa saja, tidak ada daftar method yang bisa lengkap. Konsekuensinya
disadari: nama yang kebetulan sama dengan string lain akan lolos.

Dua daftar di bawah punya arti BERBEDA:

  DIIZINKAN -- field yang memang wajar tidak dibaca per nama (ditangani lapisan
               umum). Tetap.
  UTANG     -- yatim yang sudah ada sebelum alat ini dibuat. HANYA BOLEH MENGECIL;
               alat ini menolak nama di daftar ini yang ternyata sudah dibaca,
               supaya baseline-nya tidak membusuk jadi daftar yang tidak benar.

Alat ini MELAPORKAN saja, tidak menyunting apa pun.

Keluar dengan kode 1 bila ada yatim BARU atau ada utang yang sudah lunas tetapi
masih terdaftar -- sehingga bisa dipakai langsung sebagai gerbang.

Pakai:  python field-tanpa-pembaca.py
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import akar_repo  # noqa: E402  -- perlu sys.path di atas

AKAR_REPO_POS = akar_repo.repo_pos()

SUMBER_SERVER = [
    akar_repo.KANTIN_HELPER,
    akar_repo.DRAFT_JURNAL_HELPER,
]

AKAR_PEMBACA = [
    os.path.join(akar_repo.AIS_WEBAPP, 'WEB-INF'),
    os.path.join(AKAR_REPO_POS, 'apps'),
    os.path.join(AKAR_REPO_POS, 'packages'),
    # Lapisan servlet ikut dihitung sbg pembaca -- lihat catatan di atas. Sengaja
    # HANYA lapisan ini, bukan seluruh pohon ais/: memindai semuanya (105 MB)
    # membuat nama field bertabrakan dgn variabel lokal mana pun yang kebetulan
    # bernama sama. Selisihnya terukur: seluruh pohon 15 yatim, lapisan servlet 16.
    akar_repo.AIS_SERVLET,
]

EKSTENSI = ('.dart', '.jsp', '.js', '.java')

# Berkas uji BUKAN pembaca: menyebut nama field di dalam uji tidak membuat field
# itu sampai ke mata pengguna mana pun.
DILEWATI = {'build', '.dart_tool', 'node_modules', '.git', 'generated', 'ephemeral',
            'pods', 'test', 'integration_test'}

DIIZINKAN = {
    'status': 'ditangani lapisan umum ApiClient/fetchDataAPI',
    'description': 'idem -- pesan galat/sukses generik',
    'message': 'idem',
    'teknis': 'hanya utk manusia yang membaca respons mentah',
    'referensi': 'id jejak, dibaca lapisan galat umum',
    'kode': 'terlalu umum -- dipakai puluhan konteks berbeda',
    'data': 'amplop generik daftar',
    'solusi': 'dirender panel galat umum',
    'judul': 'idem',
    'statusAsli': 'idem',
}

# Seluruhnya sudah DITELUSURI satu per satu (docs/pos/79). Nilainya adalah vonis
# penelusuran itu -- bukan sekadar penanda "belum diperiksa".
#
# Tiga kelompok, dan hanya kelompok pertama yang benar-benar merugikan.
UTANG = {
    # --- KELOMPOK 1 sudah LUNAS (dok. 80).
    # Kelimanya dahulu berupa field lepas yang tidak pernah dibaca kanal mana pun:
    # sesi_kas_sudah_tutup, sesi_kas_asal, sesi_kas_tidak_dikenal,
    # sesi_kas_direkonsiliasi, peringatanPengajuanLimit.
    #
    # Sekarang seluruhnya mengalir lewat SATU saluran `peringatanTransaksi` yang
    # dibaca lima titik klien. Yang membuktikan lunasnya bukan pernyataan ini,
    # melainkan pemeriksaan "utang yang sudah LUNAS tetapi masih terdaftar" pada
    # alat ini sendiri -- ia menyebut kelimanya dan menolak hijau sampai daftar
    # ini dibersihkan.

    # --- KELOMPOK 2: pendamping yang saudaranya MEMANG dibaca klien.
    # Diverifikasi satu per satu; klien memakai saudaranya, bukan nilai ini.
    'idSesiKas': 'klien memakai kodeSesiKas (terbaca 2 Dart + 1 JSP)',
    'satuanDasarId': 'klien memakai satuanDasarNama (terbaca 1 Dart)',
    'waktuHargaBeliTerakhir': 'klien memakai hargaBeliTerakhir (terbaca 2 Dart)',
    'versiStok': 'isinya hanya so.getId(), yang juga sudah dikirim sbg "id" '
                 '(stokAkhir terbaca 2 Dart). Nama menyesatkan, bukan penjaga '
                 'lost-update seperti yang terbaca sekilas.',
    'pengajuanLimitId': 'klien memakai kode unik nota, bukan id pengajuan '
                        '(lihat keranjang_screen.dart _kodePengajuanLimitTertunda)',

    # --- KELOMPOK 3: angka turunan/gema yang klien dapat hitung sendiri.
    'draftDiperbarui': 'boolean !draftBaru; klien sudah tahu ia mengirim draft atau tidak',
    'ringkasanBerjalan': 'objek bersarang; klien membaca totalTunai/totalNonTunai/'
                         'kasSaatIni yang sudah diratakan di sebelahnya',
    'siapDisimpan': 'boolean turunan dari validasi yang isinya sudah dikirim terpisah',
    'totalGrup': 'sama dengan grup.length(); klien menghitung arraynya sendiri',
    'statusRincian': 'gema parameter status dari permintaannya sendiri',
    'totalTerjualBersih':
        'totalTerjual - totalRetur. CATATAN BATAS ALAT: kedua saudaranya juga TIDAK '
        'dibaca klien mana pun (0 Dart, 0 JSP) -- keduanya lolos dari daftar ini hanya '
        'karena namanya kebetulan muncul di sumber server untuk keperluan lain. '
        'Kelonggaran "pembaca sisi server" bisa menutupi field yang sebenarnya yatim.',
}

POLA_KIRIM = re.compile(r'hasil\.put\("([A-Za-z0-9_]+)"')


def kumpulkan(akar):
    potongan = []
    for dirpath, dirnames, filenames in os.walk(akar):
        dirnames[:] = [d for d in dirnames if d.lower() not in DILEWATI]
        for nama in filenames:
            if nama.lower().endswith(EKSTENSI):
                potongan.append(akar_repo.baca(os.path.join(dirpath, nama)))
    return '\n'.join(potongan)


def main():
    akar_repo.pastikan_lengkap(perlu_pos=True)
    dikirim = []
    for jalur in SUMBER_SERVER:
        if not os.path.isfile(jalur):
            print('SUMBER TIDAK KETEMU: ' + jalur)
            return 1
        for nama in POLA_KIRIM.findall(akar_repo.baca(jalur)):
            if nama not in dikirim:
                dikirim.append(nama)

    pembaca = []
    for akar in AKAR_PEMBACA:
        if not os.path.isdir(akar):
            print('AKAR PEMBACA TIDAK KETEMU: ' + akar)
            return 1
        pembaca.append(kumpulkan(akar))
    semua = '\n'.join(pembaca)
    # Penulisannya sendiri dibuang, supaya hasil.put("x") tidak dihitung sbg
    # pembacaan atas "x".
    semua = POLA_KIRIM.sub('', semua)

    akar_repo.pastikan_terbaca()
    print('field yang dikirim server : %d' % len(dikirim))
    print('sumber pembaca terbaca    : %d karakter' % len(semua))

    yatim = [f for f in dikirim if f not in DIIZINKAN and f not in semua]
    baru = [f for f in yatim if f not in UTANG]
    basi = sorted(u for u in UTANG.keys() if u not in yatim)

    print('')
    print('== Utang lama (dibekukan): %d ==' % (len(yatim) - len(baru)))
    print('== Yatim BARU (belum ada di daftar utang) ==')
    if baru:
        for f in baru:
            print('   - ' + f)
    else:
        print('   (tidak ada) -- inilah yang dijaga alat ini')

    print('')
    print('== Utang yang sudah LUNAS tetapi masih terdaftar ==')
    if basi:
        for f in basi:
            print('   - %s  <- keluarkan dari daftar UTANG' % f)
    else:
        print('   (tidak ada)')

    print('')
    if baru or basi:
        print('PERIKSA LAGI: %d yatim baru, %d utang basi' % (len(baru), len(basi)))
        return 1
    print('BERSIH')
    return 0


if __name__ == '__main__':
    sys.exit(main())
