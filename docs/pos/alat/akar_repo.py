# -*- coding: utf-8 -*-
"""Menentukan letak kedua repositori TANPA jalur absolut yang ditanam di kode.

docs/pos/82 ditutup dengan satu pertanyaan:

    Kalau repositori ini di-checkout besok di mesin lain, apakah angka ini masih
    dapat dihasilkan ulang?

Pertanyaan itu lalu diterapkan pada alat-alat di direktori ini sendiri, dan
jawabannya TIDAK: tujuh dari delapan menanam `C:\\opt\\...` di dalam kodenya. Alat
yang menjaga "diklaim tetapi tidak tersambung" ternyata hanya berjalan di satu
mesin -- pengulangan yang ketiga dari bentuk yang sama.

Modul ini menampung logikanya di SATU tempat. Menyalinnya ke tiap alat berarti
mengulang persis masalah yang dikejar dok. 80 dan 81: satu perubahan, banyak titik.
Harganya satu baris bootstrap di tiap alat (menambahkan direktori ini ke sys.path)
supaya masing-masing tetap dapat dijalankan langsung, sesuai konvensi di sini.

Repositori AIS diturunkan dari letak berkas ini sendiri, jadi selalu benar.
Repositori POS (Flutter) tidak dapat diturunkan -- ia repositori terpisah -- jadi
dicari dengan urutan: variabel lingkungan POS_REPO, lalu beberapa letak yang lazim,
lalu menyerah dengan pesan yang menyebutkan cara menentukannya.
"""
import os

# .../src/main/docs/pos/alat/akar_repo.py -> naik ke .../src/main
_ALAT = os.path.dirname(os.path.abspath(__file__))
AIS_MAIN = os.path.dirname(os.path.dirname(os.path.dirname(_ALAT)))

AIS_SRC = os.path.join(AIS_MAIN, 'src')
AIS_WEBAPP = os.path.join(AIS_MAIN, 'webapp')
AIS_DOCS_POS = os.path.join(AIS_MAIN, 'docs', 'pos')

KANTIN_JSP = os.path.join(
    AIS_WEBAPP, 'WEB-INF', 'baru', 'modul', 'kantin')
PESANAN_JSP = os.path.join(KANTIN_JSP, 'pesanan', '_draft_pesanan_anggota.jsp')
KANTIN_HELPER = os.path.join(
    AIS_SRC, 'ais', 'action', 'servlet', 'api', 'KantinHelper.java')
DRAFT_JURNAL_HELPER = os.path.join(
    AIS_SRC, 'ais', 'action', 'servlet', 'api', 'DraftJurnalApiHelper.java')
AIS_SERVLET = os.path.join(AIS_SRC, 'ais', 'action', 'servlet')

_KANDIDAT_POS = [
    r'C:\opt\CodeBaseDesktopDanMobile',
    os.path.join(os.path.dirname(os.path.dirname(AIS_MAIN)), '..',
                 'CodeBaseDesktopDanMobile'),
]


def repo_pos(wajib=True):
    """Akar repositori POS (Flutter), atau None bila tidak ketemu dan tidak wajib."""
    dari_env = os.environ.get('POS_REPO')
    if dari_env and os.path.isdir(dari_env):
        return os.path.abspath(dari_env)
    for kandidat in _KANDIDAT_POS:
        if os.path.isdir(kandidat):
            return os.path.abspath(kandidat)
    if not wajib:
        return None
    raise SystemExit(
        'Repositori POS (Flutter) tidak ketemu.\n'
        'Tentukan lewat variabel lingkungan POS_REPO, contoh:\n'
        '    set POS_REPO=D:\\kerja\\CodeBaseDesktopDanMobile\n'
        'Yang sudah dicoba: ' + ', '.join(_KANDIDAT_POS))


def ringkas(path):
    """Jalur relatif terhadap akar AIS -- untuk pesan yang tidak membocorkan letak mesin."""
    try:
        return os.path.relpath(path, AIS_MAIN)
    except ValueError:
        return path


# Berkas yang WAJIB ada sebelum hasil pemindaian boleh dipercaya.
#
# Ditemukan lewat uji "checkout di jalur lain" (docs/pos/83): pada pohon yang tidak
# lengkap, field-tanpa-pembaca.py menuduh `memerlukanPersetujuanLimit` yatim --
# padahal ia dibaca PosApi.java, yang kebetulan tidak ikut tersalin. Alat yang
# memindai pohon setengah jadi menghasilkan tuduhan palsu TANPA satu pun tanda, dan
# tuduhan palsu adalah cara tercepat membuat sebuah penjaga berhenti dipercaya.
#
# Kelengkapannya diperiksa dengan MENYEBUT berkas yang memang harus ada, bukan
# sekadar mengukur besar korpus: pohon palsu itu pun sudah 7,4 juta karakter --
# "besar" tidak berarti "lengkap".
_WAJIB_ADA = [
    ('PosApi.java (pembaca respons sisi server)',
     os.path.join(AIS_SERVLET, 'PosApi.java')),
    ('KantinHelper.java', KANTIN_HELPER),
    ('halaman Pesanan (JSP)', PESANAN_JSP),
]


def pastikan_lengkap(perlu_pos=False):
    """Berhenti dengan pesan jelas bila pohon sumbernya tidak lengkap."""
    kurang = [nama for nama, jalur in _WAJIB_ADA if not os.path.exists(jalur)]
    if perlu_pos:
        pos = repo_pos(wajib=False)
        if pos is None or not os.path.isdir(os.path.join(pos, 'apps')):
            kurang.append('repositori POS (set POS_REPO)')
    if kurang:
        # Pesannya dirakit sebagai daftar baris lalu disambung, bukan memakai
        # escape di dalam literal: berkas ini beberapa kali ditulis lewat
        # heredoc, dan heredoc menelan backslash tanpa memberi tanda apa pun.
        pesan = [
            'Pohon sumber TIDAK LENGKAP -- hasilnya akan menuduh yang tidak bersalah.',
            'Tidak ketemu: ' + '; '.join(kurang),
            'Akar AIS yang dipakai: ' + AIS_MAIN,
        ]
        raise SystemExit(os.linesep.join(pesan))


# Kegagalan membaca berkas DICATAT, tidak ditelan dan tidak pula meledak mentah.
#
# Ditemukan lewat uji checkout di jalur lain (docs/pos/83): satu JSP bernama
# panjang di dalam jalur yang dalam melewati batas 260 karakter Windows, dan
# alatnya berhenti dengan traceback FileNotFoundError -- padahal berkasnya ada,
# hanya jalurnya terlalu panjang untuk dibuka.
#
# Godaannya adalah membungkus pembacaan dengan try/except lalu melanjutkan. Itu
# JUSTRU cacat yang dijaga alat-alat ini: korpus pembaca menyusut diam-diam, dan
# field yang pembacanya kebetulan ada di berkas yang gagal dibaca akan dituduh
# yatim. Karena itu kegagalan dikumpulkan, lalu pastikan_terbaca() menolak
# melaporkan angka apa pun selama masih ada yang belum terbaca.
_GAGAL_BACA = []


def baca(path):
    """Isi berkas sebagai teks. Gagal baca dicatat, bukan diabaikan."""
    try:
        f = open(path, 'rb')
    except (IOError, OSError) as e:
        _GAGAL_BACA.append((path, str(e)))
        return ''
    try:
        return f.read().decode('utf-8', 'replace')
    finally:
        f.close()


def pastikan_terbaca():
    """Berhenti bila ada berkas yang gagal dibaca -- korpusnya tidak utuh."""
    if not _GAGAL_BACA:
        return
    pesan = ['%d berkas GAGAL DIBACA -- korpusnya bolong, angkanya tidak sah.'
             % len(_GAGAL_BACA)]
    for path, sebab in _GAGAL_BACA[:10]:
        pesan.append('   ' + ringkas(path))
        pesan.append('      ' + sebab)
    if len(_GAGAL_BACA) > 10:
        pesan.append('   ... dan %d lagi' % (len(_GAGAL_BACA) - 10))
    pesan.append('Pada Windows sebab tersering adalah jalur > 260 karakter:')
    pesan.append('checkout ke direktori yang lebih dangkal, atau aktifkan LongPathsEnabled.')
    raise SystemExit(os.linesep.join(pesan))


# Letak node juga tidak boleh ditanam.
#
# docs/pos/83 mengurus jalur repositori, tetapi melewatkan ini: dua alat
# menjalankan node, dan salah satunya menyebut satu jalur pemasangan tanpa
# cadangan. Di mesin yang node-nya ada di PATH tetapi bukan di jalur itu, alatnya
# berhenti -- persis kegagalan yang sama, hanya pada berkas yang berbeda.
KANDIDAT_NODE = [
    r"C:\Program Files\nodejs\node.exe",
    r"C:\Program Files (x86)\nodejs\node.exe",
]


def node():
    """Perintah node yang dapat dijalankan; jatuh ke PATH bila tak ada kandidat."""
    dari_env = os.environ.get('NODE_EXE')
    if dari_env and os.path.isfile(dari_env):
        return dari_env
    for kandidat in KANDIDAT_NODE:
        if os.path.isfile(kandidat):
            return kandidat
    return 'node'


# Letak Flutter/Dart, dengan pola yang sama seperti repo_pos() dan node().
#
# Ditulis setelah kesalahan yang mahal (docs/pos/98): `command -v dart` dan
# `command -v flutter` sama-sama kosong, dan dari situ disimpulkan "mesin ini
# tidak punya toolchain Dart/Flutter". Kesimpulan itu diulang tiga kali di
# dokumen dan pesan commit, dan tiga perbaikan sisi klien dikirim tanpa pernah
# dianalisis maupun diuji.
#
# Flutter ADA di mesin ini, di C:\opt\flutter -- hanya tidak berada di PATH.
# PATH kosong berarti "tidak dapat dipanggil begitu saja", bukan "tidak ada".
KANDIDAT_FLUTTER = [
    os.path.join('C:' + os.sep, 'opt', 'flutter'),
    os.path.join(os.path.expanduser('~'), 'flutter'),
    os.path.join('C:' + os.sep, 'src', 'flutter'),
]


def flutter_bin(wajib=True):
    """Direktori bin Flutter (berisi flutter.bat dan dart lewat cache/dart-sdk)."""
    dari_env = os.environ.get('FLUTTER_ROOT')
    kandidat = ([dari_env] if dari_env else []) + KANDIDAT_FLUTTER
    for akar in kandidat:
        if not akar:
            continue
        b = os.path.join(akar, 'bin')
        if os.path.isfile(os.path.join(b, 'flutter.bat')) or \
                os.path.isfile(os.path.join(b, 'flutter')):
            return b
    if not wajib:
        return None
    raise SystemExit(
        'Flutter tidak ketemu. Tentukan lewat FLUTTER_ROOT, atau pasang di '
        'salah satu: ' + ', '.join(KANDIDAT_FLUTTER))


def dart_bin(wajib=True):
    """Direktori bin Dart SDK yang dibundel Flutter."""
    b = flutter_bin(wajib=wajib)
    if b is None:
        return None
    return os.path.join(b, 'cache', 'dart-sdk', 'bin')
