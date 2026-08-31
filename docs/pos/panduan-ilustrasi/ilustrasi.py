# -*- coding: utf-8 -*-
"""Panduan laporan keuangan POS Desktop dalam bentuk ILUSTRASI (bukan tangkapan layar).

Tiap laporan digambar sebagai maket layar: jalur menu, panel filter, lalu tabel
dengan NAMA KOLOM YANG SEBENARNYA - diambil dari definisi laporan di server,
bukan karangan. Angka isinya contoh, dan diberi tanda supaya tidak dikira data.
"""
import os
import fitz

SC = os.path.dirname(os.path.abspath(__file__))
KELUAR = os.path.join(SC, 'Panduan-Laporan-Keuangan-An-Nahl.pdf')

A4 = fitz.paper_rect('a4')
MARGIN = 46
LEBAR = A4.width - 2 * MARGIN

KERTAS = (0.949, 0.965, 0.945)
KARTU = (0.988, 0.996, 0.984)
TINTA = (0.102, 0.129, 0.110)
REDUP = (0.357, 0.420, 0.373)
GARIS = (0.796, 0.847, 0.796)
AKSEN = (0.059, 0.318, 0.196)
LEMBUT = (0.886, 0.933, 0.898)
GELAP = (0.071, 0.086, 0.059)
PUTIH = (1, 1, 1)
MERAH = (0.608, 0.173, 0.173)

_PETA = {u'—': '-', u'–': '-', u'’': "'", u'‘': "'",
         u'“': '"', u'”': '"', u'…': '...', u' ': ' ',
         u'•': '-', u'›': '>', u'→': '>'}


def aman(t):
    for a, b in _PETA.items():
        t = t.replace(a, b)
    return t.encode('latin-1', 'replace').decode('latin-1')


class Buku(object):
    def __init__(self):
        self.doc = fitz.open()
        self.hal = None
        self.y = 0
        self.nomor = 0

    def halaman_baru(self):
        self.hal = self.doc.new_page(width=A4.width, height=A4.height)
        self.nomor += 1
        self.y = MARGIN
        if self.nomor > 1:
            self.hal.draw_line(fitz.Point(MARGIN, A4.height - 30),
                               fitz.Point(A4.width - MARGIN, A4.height - 30),
                               color=GARIS, width=0.6)
            self.hal.insert_text(fitz.Point(A4.width - MARGIN - 52, A4.height - 19),
                                 'Halaman %d' % self.nomor, fontname='helv',
                                 fontsize=7.5, color=REDUP)
        return self.hal

    def ruang(self, butuh):
        if self.hal is None or self.y + butuh > A4.height - 50:
            self.halaman_baru()

    def _tinggi(self, isi, ukuran, jarak=1.35, lebar=None):
        lebar = lebar or LEBAR
        bay = fitz.open()
        p = bay.new_page(width=A4.width, height=2400)
        sisa = p.insert_textbox(fitz.Rect(0, 0, lebar, 2390), isi, fontname='helv',
                                fontsize=ukuran, lineheight=jarak)
        t = (2390 - sisa) if sisa >= 0 else (isi.count('\n') + 1) * ukuran * jarak + 24
        bay.close()
        return max(t, ukuran * jarak)

    def teks(self, isi, ukuran=10, warna=TINTA, font='helv', sesudah=6, jarak=1.35):
        isi = aman(isi)
        tinggi = self._tinggi(isi, ukuran, jarak)
        self.ruang(tinggi + sesudah)
        self.hal.insert_textbox(fitz.Rect(MARGIN, self.y, MARGIN + LEBAR, self.y + tinggi + 5),
                                isi, fontname=font, fontsize=ukuran, color=warna,
                                lineheight=jarak)
        self.y += tinggi + sesudah

    def judul_bagian(self, nomor, judul):
        self.ruang(80)
        if self.y > MARGIN + 4:
            self.y += 12
        atas = self.y
        self.hal.draw_rect(fitz.Rect(MARGIN, atas - 5, A4.width - MARGIN, atas + 24),
                           color=None, fill=LEMBUT)
        self.hal.draw_line(fitz.Point(MARGIN, atas - 5), fitz.Point(MARGIN, atas + 24),
                           color=AKSEN, width=2.6)
        self.hal.insert_text(fitz.Point(MARGIN + 11, atas + 14),
                             aman(('%s  %s' % (nomor, judul)) if nomor else judul),
                             fontname='hebo', fontsize=12.5, color=TINTA)
        self.y = atas + 34

    def langkah(self, daftar):
        for i, l in enumerate(daftar, 1):
            self.teks(u'%d.  %s' % (i, l), ukuran=10, sesudah=3)
        self.y += 4

    def maket(self, kategori, laporan, kolom, baris, filter_unit=True, sorot='AKUNTANSI'):
        n = max(len(baris), 1)
        h = 122 + n * 14
        self.ruang(h + 12)
        x0, y0 = MARGIN, self.y
        x1, y1 = MARGIN + LEBAR, y0 + h
        self.hal.draw_rect(fitz.Rect(x0, y0, x1, y1), color=GARIS, fill=KARTU, width=0.8)
        self.hal.draw_rect(fitz.Rect(x0, y0, x1, y0 + 14), color=None, fill=LEMBUT)
        self.hal.insert_text(fitz.Point(x0 + 8, y0 + 10), 'TokoQu Al-Bahjah An Nahl',
                             fontname='helv', fontsize=6.5, color=REDUP)

        sb = fitz.Rect(x0, y0 + 14, x0 + 74, y1)
        self.hal.draw_rect(sb, color=None, fill=GELAP)
        for i, m in enumerate(['OPERASIONAL', 'DASHBOARD', 'MASTER DATA', 'PENGADAAN',
                               'KEUANGAN', 'TRANSAKSI', 'AKUNTANSI']):
            ym = y0 + 27 + i * 12
            if m == sorot:
                self.hal.draw_rect(fitz.Rect(x0 + 4, ym - 7.5, x0 + 70, ym + 2.5),
                                   color=None, fill=AKSEN)
                self.hal.insert_text(fitz.Point(x0 + 8, ym), m, fontname='hebo',
                                     fontsize=5.4, color=PUTIH)
            else:
                self.hal.insert_text(fitz.Point(x0 + 8, ym), m, fontname='helv',
                                     fontsize=5.4, color=(0.62, 0.68, 0.63))

        jx = x0 + 84
        jalur = ('Akuntansi > Laporan-Laporan Keuangan' if sorot == 'AKUNTANSI'
                 else 'Transaksi & Laporan > Laporan-Laporan')
        self.hal.insert_text(fitz.Point(jx, y0 + 29), aman(jalur),
                             fontname='hebo', fontsize=7, color=AKSEN)
        self.hal.insert_text(fitz.Point(jx, y0 + 40), aman('Kategori: %s' % kategori),
                             fontname='helv', fontsize=6.6, color=REDUP)
        self.hal.insert_text(fitz.Point(jx, y0 + 54), aman(laporan),
                             fontname='hebo', fontsize=9.6, color=TINTA)

        fy = y0 + 62
        self.hal.draw_rect(fitz.Rect(jx, fy, x1 - 10, fy + 26), color=GARIS,
                           fill=KERTAS, width=0.6)
        medan = [('Tgl Mulai', '01/06/2026', 74), ('Tgl Sampai', '30/06/2026', 74)]
        if filter_unit:
            medan.append(('Unit / Satuan Kerja', 'SMP Al-Bahjah An Nahl', 120))
        mx = jx + 7
        for label, isi, lw in medan:
            self.hal.insert_text(fitz.Point(mx, fy + 9), label, fontname='helv',
                                 fontsize=5.2, color=REDUP)
            self.hal.draw_rect(fitz.Rect(mx, fy + 11, mx + lw, fy + 21),
                               color=GARIS, fill=PUTIH, width=0.5)
            self.hal.insert_text(fitz.Point(mx + 4, fy + 18), isi, fontname='helv',
                                 fontsize=5.8, color=TINTA)
            mx += lw + 8
        self.hal.draw_rect(fitz.Rect(mx, fy + 11, mx + 46, fy + 21), color=None, fill=AKSEN)
        self.hal.insert_text(fitz.Point(mx + 10, fy + 18), 'Tampilkan', fontname='hebo',
                             fontsize=5.8, color=PUTIH)

        ty = fy + 32
        lebar_tabel = (x1 - 10) - jx
        nk = len(kolom)
        bobot = ([2.2] + [1.0] * (nk - 1)) if nk > 2 else [1.7, 1.0]
        lebar = [lebar_tabel * b / sum(bobot) for b in bobot]
        # Kotak sel setinggi 12,5pt; di atas 6,6pt PyMuPDF menolak menggambar
        # teksnya (kembali negatif) dan tabel keluar kosong tanpa peringatan.
        uk = 5.4 if nk > 7 else (6.0 if nk > 4 else 6.6)

        self.hal.draw_rect(fitz.Rect(jx, ty, x1 - 10, ty + 14), color=None, fill=LEMBUT)
        cx = jx
        for i, k in enumerate(kolom):
            self.hal.insert_textbox(fitz.Rect(cx + 3, ty + 1.5, cx + lebar[i] - 2, ty + 14),
                                    aman(k), fontname='hebo', fontsize=uk, color=TINTA)
            cx += lebar[i]
        ty += 14
        for brs in baris:
            tebal = brs[0].startswith('~')
            if tebal:
                self.hal.draw_rect(fitz.Rect(jx, ty, x1 - 10, ty + 14), color=None, fill=KERTAS)
            cx = jx
            for i, sel in enumerate(brs[:nk]):
                isi = sel[1:] if sel.startswith('~') else sel
                self.hal.insert_textbox(fitz.Rect(cx + 3, ty + 1.5, cx + lebar[i] - 2, ty + 14),
                                        aman(isi), fontname=('hebo' if tebal else 'helv'),
                                        fontsize=uk, color=TINTA)
                cx += lebar[i]
            self.hal.draw_line(fitz.Point(jx, ty + 14), fitz.Point(x1 - 10, ty + 14),
                               color=GARIS, width=0.4)
            ty += 14

        self.hal.insert_text(fitz.Point(jx, y1 - 5), 'ilustrasi tata letak; angka hanya contoh',
                             fontname='helv', fontsize=5.2, color=REDUP)
        self.y = y1 + 10

    def sampul(self, judul, subjudul, info):
        self.halaman_baru()
        self.hal.draw_rect(fitz.Rect(0, 0, A4.width, 196), color=None, fill=LEMBUT)
        self.hal.draw_line(fitz.Point(MARGIN, 152), fitz.Point(A4.width - MARGIN, 152),
                           color=AKSEN, width=2)
        self.hal.insert_textbox(fitz.Rect(MARGIN, 54, A4.width - MARGIN, 146), aman(judul),
                                fontname='hebo', fontsize=25, color=TINTA, lineheight=1.2)
        self.hal.insert_textbox(fitz.Rect(MARGIN, 160, A4.width - MARGIN, 192), aman(subjudul),
                                fontname='helv', fontsize=11, color=REDUP)
        self.y = 220
        for b in info:
            self.teks(b, ukuran=10, sesudah=4)
        self.y += 8

    def kotak(self, judul, isi, warna=AKSEN):
        t = self._tinggi(aman(isi), 9.2, 1.35, LEBAR - 22)
        h = t + 30
        self.ruang(h + 10)
        atas = self.y
        self.hal.draw_rect(fitz.Rect(MARGIN, atas, A4.width - MARGIN, atas + h),
                           color=GARIS, fill=KARTU, width=0.7)
        self.hal.draw_line(fitz.Point(MARGIN, atas), fitz.Point(MARGIN, atas + h),
                           color=warna, width=2.6)
        self.hal.insert_text(fitz.Point(MARGIN + 11, atas + 14), aman(judul),
                             fontname='hebo', fontsize=8.2, color=warna)
        self.hal.insert_textbox(fitz.Rect(MARGIN + 11, atas + 19, A4.width - MARGIN - 11, atas + h - 4),
                                aman(isi), fontname='helv', fontsize=9.2, color=TINTA,
                                lineheight=1.35)
        self.y = atas + h + 12

    def simpan(self):
        self.doc.save(KELUAR, deflate=True)
        self.doc.close()
        return KELUAR
