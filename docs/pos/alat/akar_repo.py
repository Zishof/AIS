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
